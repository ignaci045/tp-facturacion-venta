package com.example.facturaventa.arca;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.jcajce.*;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;
import javax.xml.xpath.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Adaptación moderna del flujo WSAA/WSFE incluido en ARCAGestion; solo homologación. */
@Component
public class ArcaSoapClient {
    private final ArcaConfig config;
    private static class Transporte {
        private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12)).build();
    }
    private String token, sign;
    private boolean cacheRevisada;
    private Instant expires = Instant.EPOCH;
    private static final String WSAA="https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
    private static final String WSFE="https://wswhomo.afip.gov.ar/wsfev1/service.asmx";
    private static final String NS="http://ar.gov.afip.dif.FEV1/";
    private static final AtomicLong IDS=new AtomicLong();

    public ArcaSoapClient(ArcaConfig config) {this.config=config;}
    public record Credentials(String token,String sign) {}

    public synchronized Credentials auth() throws Exception {
        if (!config.ready()) throw new IllegalStateException("ARCA no está configurada para homologación");
        if (token!=null && sign!=null && Instant.now().isBefore(expires.minusSeconds(5))) return new Credentials(token,sign);
        if(!cacheRevisada) {
            cacheRevisada=true;
            String guardado=ArcaTicketStore.leer(config);
            if(guardado!=null) cargarTicket(guardado);
            if(token!=null && sign!=null && Instant.now().isBefore(expires.minusSeconds(5))) return new Credentials(token,sign);
        }
        Instant now=Instant.now();
        String tra="<loginTicketRequest version=\"1.0\"><header><uniqueId>"+(Instant.now().getEpochSecond()+IDS.incrementAndGet())+
           "</uniqueId><generationTime>"+now.minusSeconds(600)+"</generationTime><expirationTime>"+
           now.plusSeconds(600)+"</expirationTime></header><service>wsfe</service></loginTicketRequest>";
        String cms=signCMS(tra);
        String response=post(WSAA,"", "<loginCms xmlns=\"http://wsaa.view.sua.dvadac.desein.afip.gov\"><in0>"+cms+"</in0></loginCms>");
        Document outer=parse(response);
        String ticket=text(outer,"loginCmsReturn");
        if(ticket.isBlank())throw new IllegalStateException("WSAA no devolvió ticket de acceso");
        cargarTicket(ticket);
        ArcaTicketStore.guardar(config,ticket);
        return new Credentials(token,sign);
    }

    private void cargarTicket(String ticket) throws Exception {
        Document inner=parse(ticket);
        String t=text(inner,"token"),s=text(inner,"sign"),expiration=text(inner,"expirationTime");
        if(t.isBlank()||s.isBlank())throw new IllegalStateException("Ticket WSAA incompleto");
        Instant exp=OffsetDateTime.parse(expiration).toInstant();
        token=t;sign=s;expires=exp;

    }

    private String signCMS(String xml) throws Exception {
        KeyStore ks=KeyStore.getInstance("PKCS12");
        try(InputStream in=Files.newInputStream(Path.of(config.pkcs12Path()))) {
            ks.load(in,config.pkcs12Password().toCharArray());
        }
        String alias=config.pkcs12Alias();
        if(alias==null||alias.isBlank()) {
            Enumeration<String> aliases=ks.aliases();
            while(aliases.hasMoreElements()) {String candidate=aliases.nextElement(); if(ks.isKeyEntry(candidate)){alias=candidate;break;}}
        }
        if(alias==null||!ks.isKeyEntry(alias))throw new IllegalStateException("No existe clave privada en el PKCS12");
        PrivateKey key=(PrivateKey)ks.getKey(alias,config.pkcs12Password().toCharArray());
        X509Certificate cert=(X509Certificate)ks.getCertificate(alias);
        cert.checkValidity();
        CMSSignedDataGenerator generator=new CMSSignedDataGenerator();
        generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
            .build(new JcaContentSignerBuilder("SHA256withRSA").build(key),cert));
        generator.addCertificates(new JcaCertStore(List.of(cert)));
        return Base64.getEncoder().encodeToString(generator.generate(
            new CMSProcessableByteArray(xml.getBytes(StandardCharsets.UTF_8)),true).getEncoded());
    }

    public Document invoke(String operation, String body) throws Exception {
        Credentials c=auth();
        String auth="<Auth><Token>"+escape(c.token())+"</Token><Sign>"+escape(c.sign())+"</Sign><Cuit>"+config.cuit()+"</Cuit></Auth>";
        return parse(post(WSFE,NS+operation,"<"+operation+" xmlns=\""+NS+"\">"+auth+body+"</"+operation+">"));
    }
    private String post(String url,String action,String payload) throws Exception {
        String envelope="<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"+
            payload+"</soap:Body></soap:Envelope>";
        HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(35))
            .header("Content-Type","text/xml; charset=utf-8").header("SOAPAction","\""+action+"\"")
            .POST(HttpRequest.BodyPublishers.ofString(envelope,StandardCharsets.UTF_8)).build();
        HttpResponse<String> response=Transporte.HTTP.send(req,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Document documento;
        try {documento=parse(response.body());}
        catch(Exception e){throw new IOException("Servicio ARCA respondió HTTP "+response.statusCode()+" con una respuesta XML inválida");}
        String fault=text(documento,"faultstring");
        if(!fault.isBlank()) {
            if(fault.contains("ya posee un TA") || text(documento,"faultcode").contains("alreadyAuthenticated"))
                throw new IllegalStateException("WSAA: ya existe un ticket válido para este certificado. Usá la instancia que lo obtuvo o esperá su vencimiento; no se puede recuperar un ticket perdido desde WSAA. Los nuevos tickets de esta aplicación se conservan cifrados entre reinicios.");
            throw new IllegalStateException("ARCA: "+text(documento,"faultcode")+": "+fault);
        }
        if(response.statusCode()!=200)throw new IOException("Servicio ARCA respondió HTTP "+response.statusCode());
        return response.body();
    }
    public static Document parse(String xml) throws Exception {
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        f.setFeature("http://xml.org/sax/features/external-general-entities",false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        f.setXIncludeAware(false); f.setExpandEntityReferences(false);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
    public static String text(Object node,String name) throws Exception {
        XPath xpath=XPathFactory.newInstance().newXPath();
        return xpath.evaluate("string((.//*[local-name()='"+name+"'])[1])",node).trim();
    }
    public static String escape(String s) {return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
    public static String error(Document doc) throws Exception {
        String fault=text(doc,"faultstring");
        if(!fault.isBlank())return fault;
        String msg=text(doc,"Msg"),code=text(doc,"Code");
        return msg.isBlank()?"Respuesta inesperada de ARCA":code+": "+msg;
    }
}
