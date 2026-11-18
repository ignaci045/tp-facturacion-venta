package com.example.facturaventa.dto;
import java.math.BigDecimal;
import java.util.List;
public record CatalogosDTO(List<OpcionDTO> clientes,List<OpcionDTO> puntosVenta,List<OpcionDTO> condicionesIva,
 List<OpcionDTO> tiposMoneda,List<OpcionDTO> listasPrecio,List<PrecioArticuloOpcionDTO> listaPreciosArticulos) {
 public record OpcionDTO(Long id,String descripcion) {}
 public record PrecioArticuloOpcionDTO(Long id,Long articuloId,String descripcion,Long listaPrecioId,String listaPrecioDenominacion,BigDecimal precioNeto,BigDecimal alicuotaIva,String codigo,String rubro,String marca) {}
}
