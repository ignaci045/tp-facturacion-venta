package com.example.facturaventa;

import com.example.facturaventa.dto.*;
import com.example.facturaventa.exception.*;
import com.example.facturaventa.model.*;
import com.example.facturaventa.repository.*;
import com.example.facturaventa.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:integracion;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.demo-data.enabled=false"
})
@Transactional
class FacturaServiceIntegrationTests {
    @Autowired FacturaService servicio;
    @Autowired ReporteExportService export;
    @Autowired ClienteRepository clientes;
    @Autowired PuntoVentaRepository puntos;
    @Autowired CondicionIvaRepository ivas;
    @Autowired TipoMonedaRepository monedas;
    @Autowired ArticuloRepository articulos;
    @Autowired ListaPrecioArticuloRepository precios;
    @Autowired FacturaVentaRepository repositorio;
    @Autowired ListaPrecioRepository listas;

    private FacturaRequestDTO pedido;
    @BeforeEach void preparar() {
        var punto=puntos.save(new PuntoVenta("Principal"));
        var iva=ivas.save(new CondicionIva("Consumidor Final"));
        var moneda=monedas.save(new TipoMoneda("ARS"+System.nanoTime(),"Pesos"));
        var producto=articulos.save(new Articulo("Prueba",new BigDecimal("21")));
        var precio=precios.save(new ListaPrecioArticulo(producto,listas.save(new ListaPrecio("Prueba-"+System.nanoTime())),new BigDecimal("100.00")));
        pedido=new FacturaRequestDTO(LocalDate.of(2026,10,14),punto.getId(),clientes.save(new Cliente("Consumidor Final",null)).getId(),iva.getId(),moneda.getId(),
            BigDecimal.ZERO,"Prueba integrada",List.of(new FacturaDetalleRequestDTO(precio.getId(),new BigDecimal("3"))));
    }

    @Test void creacionEnCascadaCalculoIvaConsultaYFiltros() {
        FacturaReporteDTO dto=servicio.crear(pedido);
        assertEquals("Consumidor Final",dto.getClienteDenominacion());
        assertEquals(1,dto.getCantidadItems());
        assertEquals(363.00,dto.getImporteTotal(),0.001);
        assertTrue(servicio.buscarFacturasFiltradas(null,null,"EMITIDA",350d)
            .stream().anyMatch(r->r.getNumeroFactura().equals(dto.getNumeroFactura())));
        assertTrue(servicio.buscarFacturasFiltradas(null,null,"EMITIDA",400d)
            .stream().noneMatch(r->r.getNumeroFactura().equals(dto.getNumeroFactura())));
        assertTrue(servicio.buscarFacturasFiltradas(LocalDate.of(2027,1,1),null,null,null)
            .stream().noneMatch(r->r.getNumeroFactura().equals(dto.getNumeroFactura())));
    }

    @Test void bonificacionDescripcionEdicionYPdf() throws Exception {
        var detalle=pedido.getDetalles().get(0);detalle.setDescripcion("Entrega especial pactada");detalle.setPorcentajeBonificacion(new BigDecimal("10.00"));
        var dto=servicio.crear(pedido);
        assertEquals(326.70,dto.getImporteTotal(),.001);
        var id=servicio.buscarGestion(null,null,null,null).stream().filter(x->x.getNumeroFactura().equals(dto.getNumeroFactura())).findFirst().orElseThrow().getId();
        var guardado=repositorio.findById(id).orElseThrow().getDetalles().get(0);
        assertEquals(0,new BigDecimal("270.00").compareTo(guardado.getImporteNeto()));
        assertEquals(0,new BigDecimal("56.70").compareTo(guardado.getImporteIva()));
        var editado=servicio.obtenerParaEdicion(id).getFactura().getDetalles().get(0);
        assertEquals("Entrega especial pactada",editado.getDescripcion());assertEquals(0,new BigDecimal("10").compareTo(editado.getPorcentajeBonificacion()));
        String text=textoPdf(export.pdfIndividual(id));assertTrue(text.contains("Entrega especial pactada"));assertTrue(text.contains("326,70"));
        muestra("bonificacion.pdf",export.pdfIndividual(id));
        detalle.setPorcentajeBonificacion(new BigDecimal("100"));assertEquals(0,servicio.actualizar(id,pedido).getImporteTotal(),.001);
    }
    @Test void rechazaBonificacionesFueraDeRango(){
        for(String valor:List.of("-1","100.01")){pedido.getDetalles().get(0).setPorcentajeBonificacion(new BigDecimal(valor));assertThrows(ReglaNegocioException.class,()->servicio.crear(pedido));}
    }

    @Test void actualizacionAnulacionYEliminacion() {
        var creado=servicio.crear(pedido);
        var gestion=servicio.buscarGestion(null,null,null,null).stream()
            .filter(g->g.getNumeroFactura().equals(creado.getNumeroFactura())).findFirst().orElseThrow();
        var actualizacion=pedido;
        actualizacion.setDetalles(List.of(new FacturaDetalleRequestDTO(pedido.getDetalles().get(0).getListaPrecioArticuloId(),BigDecimal.ONE)));
        assertEquals(121d,servicio.actualizar(gestion.getId(),actualizacion).getImporteTotal(),0.001);
        servicio.actualizarEstado(gestion.getId(),new ActualizarEstadoDTO("ANULADA","Error de carga"));
        assertEquals("ANULADA",servicio.buscarGestion(null,null,null,null).stream()
            .filter(f->f.getId().equals(gestion.getId())).findFirst().orElseThrow().getEstado());
        assertThrows(ReglaNegocioException.class,()->servicio.actualizar(gestion.getId(),pedido));
        servicio.eliminar(gestion.getId());
        assertThrows(RecursoNoEncontradoException.class,()->servicio.obtenerParaEdicion(gestion.getId()));
    }

    @Test void reportesSonArchivosValidos() {
        servicio.crear(pedido);
        byte[] pdf=export.pdf(null,null,null,null);
        byte[] excel=export.excel(null,null,null,null);
        assertTrue(pdf.length>1000);
        assertEquals("%PDF",new String(pdf,0,4,java.nio.charset.StandardCharsets.US_ASCII));
        assertTrue(excel.length>1000);
        assertEquals('P',excel[0]);assertEquals('K',excel[1]);
    }

    private String textoPdf(byte[] bytes) throws Exception {
        var reader=new com.lowagie.text.pdf.PdfReader(bytes);
        try {var extractor=new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);StringBuilder t=new StringBuilder();for(int i=1;i<=reader.getNumberOfPages();i++)t.append(extractor.getTextFromPage(i));return t.toString();}
        finally {reader.close();}
    }
    private void muestra(String nombre,byte[] bytes) throws Exception {
        String dir=System.getProperty("app.test.pdf-output");
        if(dir!=null){java.nio.file.Files.createDirectories(java.nio.file.Path.of(dir));java.nio.file.Files.write(java.nio.file.Path.of(dir,nombre),bytes);}
    }
    @Test void pdfIndividualNoIncluyeOtrasFacturas() throws Exception {
        var cliente=clientes.findById(pedido.getClienteId()).orElseThrow();cliente.setDenominacion("Cliente Seleccionado");
        var primera=servicio.crear(pedido);Long id=servicio.buscarGestion(null,null,null,null).get(0).getId();
        pedido.setClienteId(clientes.save(new Cliente("Otro Cliente Excluido",null)).getId());servicio.crear(pedido);
        byte[] bytes=export.pdfIndividual(id);String text=textoPdf(bytes);
        assertTrue(text.contains("Cliente Seleccionado"));assertTrue(text.contains("Prueba"));assertTrue(text.contains("363,00"));
        assertFalse(text.contains("Otro Cliente Excluido"));assertFalse(text.contains("TOTAL GENERAL"));
        assertTrue(textoPdf(export.pdf(null,null,null,null)).contains("Otro Cliente Excluido"));
        assertThrows(RecursoNoEncontradoException.class,()->export.pdfIndividual(Long.MAX_VALUE));
        muestra("factura-individual-local.pdf",bytes);
    }
    @Test void pdfIndividualMuestraCaeYAnulacion() throws Exception {
        servicio.crear(pedido);var g=servicio.buscarGestion(null,null,null,null).get(0);var f=repositorio.findById(g.getId()).orElseThrow();
        f.setCae("12345678901234");f.setPuntoVentaFiscal(3);f.setTipoComprobanteFiscal(11);f.setNumeroFiscal(2L);
        f.setCaeFechaVencimiento(java.sql.Date.valueOf("2026-11-23"));f.setEstado("ANULADA");
        byte[] bytes=export.pdfIndividual(f.getId());String text=textoPdf(bytes);
        assertTrue(text.contains("12345678901234"));assertTrue(text.contains("00003-00000002"));assertTrue(text.contains("COMPROBANTE ANULADO"));assertTrue(text.contains("23/11/2026"));
        muestra("factura-individual-homologacion.pdf",bytes);
    }
    @Test void pdfIndividualMantieneDetallesEnVariasPaginas() throws Exception {
        pedido.setDetalles(java.util.stream.IntStream.range(0,80).mapToObj(i->new FacturaDetalleRequestDTO(pedido.getDetalles().get(0).getListaPrecioArticuloId(),BigDecimal.ONE)).toList());
        servicio.crear(pedido);Long id=servicio.buscarGestion(null,null,null,null).get(0).getId();
        byte[] bytes=export.pdfIndividual(id);var reader=new com.lowagie.text.pdf.PdfReader(bytes);
        try {assertTrue(reader.getNumberOfPages()>1);}finally {reader.close();}
        assertTrue(textoPdf(bytes).contains("9.680,00"));muestra("factura-individual-multipagina.pdf",bytes);
    }

    @Test void clienteObligatorioEHistorialPorIdentidad() {
        Long clienteId=pedido.getClienteId();
        servicio.crear(pedido);
        var otro=clientes.save(new Cliente("Consumidor Final","12345678"));
        pedido.setClienteId(otro.getId()); servicio.crear(pedido);
        assertEquals(1,servicio.buscarPorCliente(clienteId).size());
        assertEquals(1,servicio.buscarPorCliente(otro.getId()).size());
        pedido.setClienteId(null);
        assertThrows(ReglaNegocioException.class,()->servicio.crear(pedido));
        pedido.setClienteId(Long.MAX_VALUE);
        assertThrows(RecursoNoEncontradoException.class,()->servicio.crear(pedido));
    }
    @Test void usaElPrecioSeleccionadoYSuListaSinElegirElUltimo() throws Exception {
        Long original=pedido.getDetalles().get(0).getListaPrecioArticuloId();
        var precio=precios.findById(original).orElseThrow();
        var alternativo=precios.save(new ListaPrecioArticulo(precio.getArticulo(),listas.save(new ListaPrecio("Mayorista prueba")),new BigDecimal("200.00")));
        var primero=servicio.crear(pedido);
        assertEquals(363d,primero.getImporteTotal(),0.001);
        Long id=servicio.buscarGestion(null,null,null,null).get(0).getId();
        assertEquals(original,servicio.obtenerParaEdicion(id).getFactura().getDetalles().get(0).getListaPrecioArticuloId());
        assertEquals(precio.getArticulo().getId(),repositorio.findById(id).orElseThrow().getDetalles().get(0).getArticulo().getId());
        pedido.getDetalles().get(0).setListaPrecioArticuloId(alternativo.getId());
        assertEquals(726d,servicio.crear(pedido).getImporteTotal(),0.001);
        assertTrue(textoPdf(export.pdfIndividual(id)).contains("363,00"));
    }
    @Test void rechazaPreciosInexistentesEInactivos() {
        var detalle=pedido.getDetalles().get(0);var precio=precios.findById(detalle.getListaPrecioArticuloId()).orElseThrow();
        precio.setActivo(false);assertThrows(ReglaNegocioException.class,()->servicio.crear(pedido));
        precio.setActivo(true);precio.getListaPrecio().setActivo(false);assertThrows(ReglaNegocioException.class,()->servicio.crear(pedido));
        detalle.setListaPrecioArticuloId(Long.MAX_VALUE);assertThrows(RecursoNoEncontradoException.class,()->servicio.crear(pedido));
    }
    @Test void validacionesFrenanImportesErroneos() {
        pedido.setImporteCobrado(new BigDecimal("100000"));
        assertThrows(ReglaNegocioException.class,()->servicio.crear(pedido));
        assertThrows(ReglaNegocioException.class,()->servicio.buscarFacturasFiltradas(
            LocalDate.now(),LocalDate.now().minusDays(2),null,null));
    }
}
