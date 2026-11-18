package com.example.facturaventa;
import com.example.facturaventa.config.DemoData;
import com.example.facturaventa.controller.ClienteRestController;
import com.example.facturaventa.repository.*;
import com.example.facturaventa.service.FacturaService;
import com.example.facturaventa.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:clientesdemo;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","app.demo-data.enabled=true"})
class ClientesDemoIntegrationTests {
 @Autowired ClienteRepository clientes;
 @Autowired FacturaVentaRepository facturas;
 @Autowired ClienteRestController controller;
 @Autowired FacturaService servicio;
 @Autowired DemoData demo;
 @Autowired ArticuloRepository articulos;
 @Autowired ListaPrecioArticuloRepository precios;
 @Autowired com.example.facturaventa.controller.ArticuloRestController articulosController;
 @Test @org.springframework.transaction.annotation.Transactional void contactoDomicilioYArticuloEditablesSinCambiarPreciosNiFacturas(){
   var existente=controller.buscar("",0,10).getContent().get(0);
   var actualizado=controller.actualizar(existente.getId(),new ClienteRestController.Alta(existente.getDenominacion(),existente.getDocumento(),"MAYORISTA","cliente@example.test","111","222","Calle Demo","123","Ciudad","Provincia","1000"));
   assertEquals("MAYORISTA",actualizado.getTipoCliente());assertEquals("cliente@example.test",actualizado.getContacto().getEmail());assertEquals("Ciudad",actualizado.getDomicilio().getLocalidad());
   assertEquals(15,servicio.buscarPorCliente(existente.getId()).size());
   var a=articulos.findAll().get(0);var valor=precios.findAll().get(0).getPrecioNeto();
   assertNotNull(a.getCodigo());assertNotNull(a.getRubro());assertNotNull(a.getMarca());
   articulosController.actualizar(a.getId(),new com.example.facturaventa.controller.ArticuloRestController.Datos("EDIT-001","Artículo editado","Rubro nuevo","Marca nueva"));
   assertEquals("EDIT-001",a.getCodigo());assertEquals("Rubro nuevo",a.getRubro().getDenominacion());assertEquals("Marca nueva",a.getMarca().getDenominacion());
   assertEquals(valor,precios.findAll().get(0).getPrecioNeto());assertEquals(900,facturas.count());
   var otro=articulos.findAll().stream().filter(x->!x.getId().equals(a.getId())).findFirst().orElseThrow();
   assertThrows(ReglaNegocioException.class,()->articulosController.actualizar(otro.getId(),new com.example.facturaventa.controller.ArticuloRestController.Datos("EDIT-001","Otro","Rubro","Marca")));
 }
 @Test void datosPaginacionHistorialYAltaIndependiente() {
   assertEquals(60,clientes.count()); assertEquals(900,facturas.count());
   assertEquals(120,articulos.count());assertEquals(240,precios.count());
   var primera=controller.buscar("",0,10); var segunda=controller.buscar("",1,10);
   assertEquals(6,primera.getTotalPages()); assertEquals(10,segunda.getContent().size());
   assertNotEquals(primera.getContent().get(0).getId(),segunda.getContent().get(0).getId());
   var cliente=primera.getContent().get(0);
   assertEquals(15,servicio.buscarPorCliente(cliente.getId()).size());
   assertEquals(1,controller.buscar(cliente.getDocumento(),0,10).getTotalElements());
   var nuevo=controller.crear(new ClienteRestController.Alta("Cliente sin facturas","40.123.456")).getBody();
   assertNotNull(nuevo); assertTrue(servicio.buscarPorCliente(nuevo.getId()).isEmpty());
   assertThrows(ReglaNegocioException.class,()->controller.crear(new ClienteRestController.Alta("Duplicado","40123456")));
   demo.run(); assertEquals(61,clientes.count()); assertEquals(900,facturas.count());
 }
}
