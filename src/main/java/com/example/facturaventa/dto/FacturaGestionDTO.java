package com.example.facturaventa.dto;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.util.Date;

/** Vista de gestión adicional: la proyección de reporte NO se contamina con id ni estado. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FacturaGestionDTO {
    private Long id;
    private String estado;
    private Long numeroFactura;
    @JsonFormat(pattern="yyyy-MM-dd",timezone="UTC") private Date fechaEmision;
    private String clienteDenominacion;
    private String condicionIva;
    private String puntoVentaDescripcion;
    private double importeTotal;
    private long cantidadItems;
    private String cae;
    private Integer puntoVentaFiscal;
    private Long numeroFiscal;
}
