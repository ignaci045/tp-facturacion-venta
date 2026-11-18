package com.example.facturaventa.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FacturaRequestDTO {
    @NotNull private LocalDate fechaEmision;
    @NotNull @Positive private Long puntoVentaId;
    @NotNull @Positive private Long clienteId;
    @NotNull @Positive private Long condicionIvaId;
    @NotNull @Positive private Long tipoMonedaId;
    @NotNull @DecimalMin("0.00") private BigDecimal importeCobrado = BigDecimal.ZERO;
    @Size(max=1200) private String observaciones;
    @NotEmpty private List<@NotNull @Valid FacturaDetalleRequestDTO> detalles;
}
