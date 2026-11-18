package com.example.facturaventa.controller;

import com.example.facturaventa.dto.*;
import com.example.facturaventa.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
@CrossOrigin(origins={"http://localhost:5173","http://127.0.0.1:5173"})
public class FacturaRestController {
    private final FacturaService facturas;
    private final ReporteExportService export;

    @GetMapping
    public List<FacturaReporteDTO> listar(
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required=false) String estado,
        @RequestParam(required=false) Double montoMinimo) {
        return facturas.buscarFacturasFiltradas(fechaDesde,fechaHasta,estado,montoMinimo);
    }

    @GetMapping("/gestion")
    public List<FacturaGestionDTO> gestion(
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required=false) String estado,
        @RequestParam(required=false) Double montoMinimo) {
        return facturas.buscarGestion(fechaDesde,fechaHasta,estado,montoMinimo);
    }

    @GetMapping("/{id}") public FacturaEdicionDTO obtener(@PathVariable Long id){return facturas.obtenerParaEdicion(id);}
    @PostMapping public ResponseEntity<FacturaReporteDTO> crear(@Valid @RequestBody FacturaRequestDTO entrada) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturas.crear(entrada));
    }
    @PutMapping("/{id}") public FacturaReporteDTO actualizar(@PathVariable Long id,@Valid @RequestBody FacturaRequestDTO entrada) {
        return facturas.actualizar(id,entrada);
    }
    @PutMapping("/{id}/estado") public FacturaReporteDTO estado(@PathVariable Long id,@Valid @RequestBody ActualizarEstadoDTO entrada) {
        return facturas.actualizarEstado(id,entrada);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        facturas.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping(value="/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdfIndividual(@PathVariable Long id) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=factura-"+id+".pdf")
            .body(export.pdfIndividual(id));
    }
    @GetMapping(value="/reporte.pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required=false) String estado, @RequestParam(required=false) Double montoMinimo) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=reporte-facturas.pdf")
            .body(export.pdf(fechaDesde,fechaHasta,estado,montoMinimo));
    }
    @GetMapping(value="/reporte.xlsx",produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> excel(
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
        @RequestParam(required=false) String estado,@RequestParam(required=false) Double montoMinimo) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=reporte-facturas.xlsx")
            .body(export.excel(fechaDesde,fechaHasta,estado,montoMinimo));
    }
}
