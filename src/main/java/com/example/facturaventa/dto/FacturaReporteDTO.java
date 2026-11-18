package com.example.facturaventa.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.util.Date;

/** Los siete campos requeridos por el primer TP. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FacturaReporteDTO {
    private Long numeroFactura;
    @JsonFormat(pattern="yyyy-MM-dd",timezone="UTC") private Date fechaEmision;
    private String clienteDenominacion;
    private String condicionIva;
    private String puntoVentaDescripcion;
    private double importeTotal;
    private long cantidadItems;
}
