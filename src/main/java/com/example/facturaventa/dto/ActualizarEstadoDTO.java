package com.example.facturaventa.dto;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ActualizarEstadoDTO {
    @NotBlank private String estado;
    @Size(max=1200) private String observaciones;
}
