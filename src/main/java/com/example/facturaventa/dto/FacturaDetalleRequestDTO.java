package com.example.facturaventa.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FacturaDetalleRequestDTO {
    @NotNull @Positive private Long listaPrecioArticuloId;
    @NotNull @DecimalMin(value="0.0",inclusive=false) private BigDecimal cantidad;
    @Size(max=1200) private String descripcion;
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer=3,fraction=2) private BigDecimal porcentajeBonificacion=BigDecimal.ZERO;
    public FacturaDetalleRequestDTO(Long precio,BigDecimal cantidad){this.listaPrecioArticuloId=precio;this.cantidad=cantidad;}
}
