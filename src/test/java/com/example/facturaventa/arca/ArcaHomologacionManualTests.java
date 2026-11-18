package com.example.facturaventa.arca;
import com.example.facturaventa.dto.*;
import com.example.facturaventa.model.*;
import com.example.facturaventa.repository.*;
import com.example.facturaventa.service.FacturaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
/** Prueba remota opcional: emite UN comprobante C en homologación usando BD temporal. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:arca-manual;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","app.demo-data.enabled=false","arca.enabled=true"})
@EnabledIfEnvironmentVariable(named="ARCA_RUN_LIVE_TEST",matches="true")
class ArcaHomologacionManualTests {
 @Autowired ArcaService arca; @Autowired FacturaService facturas;
 @Autowired PuntoVentaRepository puntos; @Autowired CondicionIvaRepository condiciones;
 @Autowired TipoMonedaRepository monedas; @Autowired ClienteRepository clientes;
 @Autowired ListaPrecioRepository listas;
 @Autowired ArticuloRepository articulos; @Autowired ListaPrecioArticuloRepository precios;
 @Value("${ARCA_TEST_PTO_VTA}") int puntoFiscal;
 @Test void emiteFacturaCSinIvaEnHomologacion() throws Exception {
   var punto=puntos.save(new PuntoVenta("Prueba temporal"));
   var iva=condiciones.save(new CondicionIva("Consumidor Final"));
   var moneda=monedas.save(new TipoMoneda("ARS","Pesos"));
   var cliente=clientes.save(new Cliente("Consumidor Final - prueba homologación",null));
   var articulo=articulos.save(new Articulo("Artículo de prueba sin IVA",BigDecimal.ZERO));
   var precio=precios.save(new ListaPrecioArticulo(articulo,listas.save(new ListaPrecio("Prueba ARCA")),new BigDecimal("100.00")));
   var factura=facturas.crear(new FacturaRequestDTO(LocalDate.now(),punto.getId(),cliente.getId(),iva.getId(),moneda.getId(),BigDecimal.ZERO,"Prueba técnica de homologación, sin valor fiscal",List.of(new FacturaDetalleRequestDTO(precio.getId(),BigDecimal.ONE))));
   Long id=facturas.buscarGestion(null,null,null,null).get(0).getId();
   var resultado=arca.emitir(id,new ArcaSolicitudDTO(puntoFiscal,11,99,0L,5));
   assertEquals("A",resultado.resultado());assertNotNull(resultado.cae());
   assertFalse(resultado.cae().isBlank());assertNotNull(facturas.buscarGestion(null,null,null,null).get(0).getCae());
   System.out.println("HOMOLOGACION: Factura C punto "+resultado.puntoVenta()+", número "+resultado.numeroFiscal()+", CAE "+resultado.cae()+", vencimiento "+resultado.vencimiento());
 }
}
