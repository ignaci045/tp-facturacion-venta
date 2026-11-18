package com.example.facturaventa.arca;

import com.example.facturaventa.model.*;
import com.example.facturaventa.repository.FacturaVentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import java.math.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.Date;
import java.util.*;

@Service
public class ArcaService {
    private final ArcaSoapClient soap;
    private final ArcaConfig cfg;
    private final FacturaVentaRepository repository;
    public ArcaService(ArcaSoapClient soap,ArcaConfig cfg,FacturaVentaRepository repository) {
        this.soap=soap;this.cfg=cfg;this.repository=repository;
    }
    public record Estado(boolean configurado,String ambiente,String detalle) {}
    public Estado estado() {return new Estado(cfg.ready(),"homologacion",cfg.ready()?"Configuración lista; autorización no comprobada":"Configurar ARCA_ENABLED, ARCA_CUIT y credenciales PKCS12");}
    public record PuntoFiscal(String numero,String emisionTipo,String bloqueado) {}
    public record Diagnostico(boolean autenticado,String ambiente,List<PuntoFiscal> puntosVenta,String detalle) {}
    public Diagnostico diagnosticar() throws Exception {
        soap.auth();
        Document d=soap.invoke("FEParamGetPtosVenta", "");
        var nodos=d.getElementsByTagNameNS("*","PtoVenta");
        List<PuntoFiscal> puntos=new ArrayList<>();
        for(int i=0;i<nodos.getLength();i++)puntos.add(new PuntoFiscal(ArcaSoapClient.text(nodos.item(i),"Nro"),
            ArcaSoapClient.text(nodos.item(i),"EmisionTipo"),ArcaSoapClient.text(nodos.item(i),"Bloqueado")));
        return new Diagnostico(true,"homologacion",puntos,puntos.isEmpty()?"Autenticación WSAA correcta. Consulta de puntos de venta: "+ArcaSoapClient.error(d):"WSAA y consulta WSFE correctas");
    }
    public record Resultado(Long facturaId,int puntoVenta,int tipoComprobante,long numeroFiscal,
                            String resultado,String cae,String vencimiento,String observaciones) {}

    /** Impide que IDs del catálogo local se utilicen como códigos fiscales por accidente. */
    @Transactional
    public Resultado emitir(Long id,ArcaSolicitudDTO req) throws Exception {
        if(!cfg.ready())throw new IllegalStateException("ARCA homologación no configurada");
        FacturaVenta factura=repository.findById(id).orElseThrow(()->new IllegalArgumentException("Factura inexistente"));
        if(factura.getCae()!=null&&!factura.getCae().isBlank())throw new IllegalStateException("La factura ya tiene CAE. No se permite emitirla nuevamente");
        if(!"EMITIDA".equals(factura.getEstado()))throw new IllegalStateException("No se puede autorizar una factura anulada");
        if(!"ARS".equalsIgnoreCase(factura.getTipoMoneda().getCodigo()))throw new IllegalArgumentException("Solo se admiten pesos argentinos");
        int tipo=req.tipoComprobante();
        if(!Set.of(1,6,11).contains(tipo))throw new IllegalArgumentException("Tipos admitidos: Factura A(1), B(6), C(11)");
        if(!Set.of(80,96,99).contains(req.tipoDocumento()))throw new IllegalArgumentException("Tipo de documento inválido");
        if(req.tipoDocumento()==99 && req.numeroDocumento()!=0L)throw new IllegalArgumentException("Sin identificar debe utilizar número 0");
        if(req.tipoDocumento()!=99 && req.numeroDocumento()==0L)throw new IllegalArgumentException("Falta el número del documento");
        if(req.condicionIvaReceptor()<1)throw new IllegalArgumentException("Condición IVA del receptor inválida");
        Map<Integer,BigDecimal[]> tasas=new TreeMap<>();
        BigDecimal neto=BigDecimal.ZERO, iva=BigDecimal.ZERO;
        for(FacturaVentaDetalle linea:factura.getDetalles()) {
            BigDecimal tasa=linea.getAlicuotaIva();
            int idTasa=mapearIva(tasa);
            BigDecimal[] valores=tasas.computeIfAbsent(idTasa,k->new BigDecimal[]{BigDecimal.ZERO,BigDecimal.ZERO});
            valores[0]=valores[0].add(linea.getImporteNeto());
            valores[1]=valores[1].add(linea.getImporteIva());
            neto=neto.add(linea.getImporteNeto());iva=iva.add(linea.getImporteIva());
        }
        BigDecimal total=neto.add(iva).setScale(2,RoundingMode.HALF_UP);
        if(total.compareTo(BigDecimal.valueOf(factura.getImporteTotal()).setScale(2,RoundingMode.HALF_UP))!=0)
            throw new IllegalStateException("No coinciden los importes del detalle con la cabecera");
        if(tipo==11 && iva.signum()!=0)throw new IllegalArgumentException("Factura C no puede desglosar IVA. Ajustar el comprobante interno antes de autorizar");
        if(tipo!=11 && tasas.containsKey(0))throw new IllegalArgumentException("Alícuota IVA no admitida para este comprobante");
        Document consulta=soap.invoke("FECompUltimoAutorizado", "<PtoVta>"+req.puntoVentaFiscal()+"</PtoVta><CbteTipo>"+tipo+"</CbteTipo>");
        String anterior=ArcaSoapClient.text(consulta,"CbteNro");
        if(anterior.isBlank())throw new IllegalStateException("Consulta del último comprobante: "+ArcaSoapClient.error(consulta));
        long siguiente=Long.parseLong(anterior)+1;
        String fecha=new java.sql.Date(factura.getFechaEmision().getTime()).toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        StringBuilder detalle=new StringBuilder("<FECAEDetRequest><Concepto>1</Concepto><DocTipo>").append(req.tipoDocumento())
          .append("</DocTipo><DocNro>").append(req.numeroDocumento()).append("</DocNro><CbteDesde>").append(siguiente)
          .append("</CbteDesde><CbteHasta>").append(siguiente).append("</CbteHasta><CbteFch>").append(fecha)
          .append("</CbteFch><ImpTotal>").append(total.toPlainString()).append("</ImpTotal><ImpTotConc>0</ImpTotConc><ImpNeto>")
          .append(tipo==11?total.toPlainString():neto.toPlainString())
          .append("</ImpNeto><ImpOpEx>0</ImpOpEx><ImpTrib>0</ImpTrib><ImpIVA>")
          .append(tipo==11?"0":iva.toPlainString()).append("</ImpIVA><MonId>PES</MonId><MonCotiz>1</MonCotiz>")
          .append("<CondicionIVAReceptorId>").append(req.condicionIvaReceptor()).append("</CondicionIVAReceptorId>");
        if(tipo!=11) {
            detalle.append("<Iva>");
            for(var entry:tasas.entrySet()){
                detalle.append("<AlicIva><Id>").append(entry.getKey()).append("</Id><BaseImp>")
                    .append(entry.getValue()[0].toPlainString()).append("</BaseImp><Importe>")
                    .append(entry.getValue()[1].toPlainString()).append("</Importe></AlicIva>");
            }
            detalle.append("</Iva>");
        }
        detalle.append("</FECAEDetRequest>");
        String payload="<FeCAEReq><FeCabReq><CantReg>1</CantReg><PtoVta>"+req.puntoVentaFiscal()+
            "</PtoVta><CbteTipo>"+tipo+"</CbteTipo></FeCabReq><FeDetReq>"+detalle+"</FeDetReq></FeCAEReq>";
        Document resp;
        try {resp=soap.invoke("FECAESolicitar",payload);}
        catch(Exception e){throw new IllegalStateException("Estado fiscal incierto: consultar el comprobante en ARCA antes de reintentar. "+e.getMessage(),e);}
        String resultado=ArcaSoapClient.text(resp,"Resultado");
        String cae=ArcaSoapClient.text(resp,"CAE");
        String venc=ArcaSoapClient.text(resp,"CAEFchVto");
        String observations=ArcaSoapClient.text(resp,"Msg");
        if(!"A".equals(resultado)||cae.isBlank()) {
            throw new IllegalStateException("ARCA no autorizó el comprobante: "+ArcaSoapClient.error(resp));
        }
        factura.setCae(cae);
        factura.setResultadoAfip("A");
        factura.setCaeFechaVencimiento(Date.valueOf(LocalDate.parse(venc,DateTimeFormatter.BASIC_ISO_DATE)));
        factura.setMotivoRechazo(observations.isBlank()?null:observations);
        factura.setPuntoVentaFiscal(req.puntoVentaFiscal());
        factura.setTipoComprobanteFiscal(tipo);
        factura.setNumeroFiscal(siguiente);
        repository.saveAndFlush(factura);
        return new Resultado(id,req.puntoVentaFiscal(),tipo,siguiente,resultado,cae,venc,observations);
    }
    private int mapearIva(BigDecimal pct) {
        if(pct.compareTo(new BigDecimal("21"))==0)return 5;
        if(pct.compareTo(new BigDecimal("10.5"))==0)return 4;
        if(pct.compareTo(new BigDecimal("27"))==0)return 6;
        if(pct.compareTo(new BigDecimal("5"))==0)return 8;
        if(pct.compareTo(new BigDecimal("2.5"))==0)return 9;
        if(pct.signum()==0)return 3;
        throw new IllegalArgumentException("Alícuota no soportada por adaptador: "+pct);
    }
}
