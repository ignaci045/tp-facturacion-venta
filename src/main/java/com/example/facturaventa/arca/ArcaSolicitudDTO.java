package com.example.facturaventa.arca;

import jakarta.validation.constraints.*;

/** Datos fiscales explícitos: no son equivalentes a los IDs internos del TP. */
public record ArcaSolicitudDTO(
    @NotNull @Min(1) @Max(99999) Integer puntoVentaFiscal,
    @NotNull Integer tipoComprobante,
    @NotNull Integer tipoDocumento,
    @NotNull @Min(0) Long numeroDocumento,
    @NotNull Integer condicionIvaReceptor
) {}
