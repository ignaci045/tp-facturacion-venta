package com.example.facturaventa;
import com.example.facturaventa.config.DemoData;
import com.example.facturaventa.model.*;
import com.example.facturaventa.repository.*;
import com.example.facturaventa.service.CatalogoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:incompleta;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","app.demo-data.enabled=false"})
@Transactional
class CatalogosExistentesIntegrationTests {
 @Autowired PuntoVentaRepository puntos;
 @Autowired ClienteRepository clientes;
 @Autowired FacturaVentaRepository facturas;
 @Autowired CatalogoService catalogos;
 @Autowired org.springframework.context.ApplicationContext context;
 @Test void completaCatalogosSinRecrearCuentasNiFacturas() {
   puntos.save(new PuntoVenta("Punto existente"));
   clientes.save(new Cliente("Cuenta existente","30123456"));
   var demo=new DemoData(clientes,puntos,context.getBean(CondicionIvaRepository.class),context.getBean(TipoMonedaRepository.class),
      context.getBean(ArticuloRepository.class),context.getBean(ListaPrecioArticuloRepository.class),context.getBean(com.example.facturaventa.service.FacturaService.class),context.getBean(ListaPrecioRepository.class),context.getBean(RubroRepository.class),context.getBean(MarcaRepository.class));
   demo.run();
   var datos=catalogos.obtener();
   assertEquals(1,datos.puntosVenta().size()); assertEquals(1,datos.clientes().size());
   assertFalse(datos.condicionesIva().isEmpty()); assertFalse(datos.tiposMoneda().isEmpty()); assertEquals(240,datos.listaPreciosArticulos().size());
   assertEquals(0,facturas.count());
   demo.run(); assertEquals(240,catalogos.obtener().listaPreciosArticulos().size()); assertEquals(1,clientes.count());
 }
}
