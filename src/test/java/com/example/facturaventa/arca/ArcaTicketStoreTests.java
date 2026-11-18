package com.example.facturaventa.arca;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
class ArcaTicketStoreTests {
 @TempDir Path temp;
 private ArcaConfig config() throws Exception {
   Path p12=temp.resolve("prueba.p12");Files.writeString(p12,"material ficticio exclusivo de prueba");
   return new ArcaConfig(true,"homologacion","20000000001",p12.toString(),"password-ficticio-de-prueba",null);
 }
 @Test void guardaCifradoYRecuperaTrasReinicio() throws Exception {
   var cfg=config();String ticket="<loginTicketResponse><header><expirationTime>"+Instant.now().plusSeconds(3600)+"</expirationTime></header><credentials><token>token-ficticio</token><sign>firma-ficticia</sign></credentials></loginTicketResponse>";
   try {
     ArcaTicketStore.guardar(cfg,ticket);
     assertEquals(ticket,ArcaTicketStore.leer(cfg));
     assertFalse(new String(Files.readAllBytes(ArcaTicketStore.ruta(cfg)),java.nio.charset.StandardCharsets.ISO_8859_1).contains("token-ficticio"));
     assertEquals("token-ficticio",new ArcaSoapClient(cfg).auth().token());
   } finally {Files.deleteIfExists(ArcaTicketStore.ruta(cfg));}
 }
 @Test void noAceptaPasswordIncorrecto() throws Exception {
   var cfg=config();try {
     ArcaTicketStore.guardar(cfg,"ticket");
     var otro=new ArcaConfig(true,"homologacion",cfg.cuit(),cfg.pkcs12Path(),"otra-password",null);
     assertThrows(IllegalStateException.class,()->ArcaTicketStore.leer(otro));
   } finally {Files.deleteIfExists(ArcaTicketStore.ruta(cfg));}
 }
}
