package com.example.facturaventa.dto;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FacturaEdicionDTO {
    private Long id;
    private String estado;
    private FacturaRequestDTO factura;
}
