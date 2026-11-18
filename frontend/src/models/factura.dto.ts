/** Espejo estricto de los DTOs Java: los Date/LocalDate llegan al navegador como ISO yyyy-MM-dd. */
export interface FacturaReporteDTO {
  numeroFactura: number;
  fechaEmision: string;
  clienteDenominacion: string;
  condicionIva: string;
  puntoVentaDescripcion: string;
  importeTotal: number;
  cantidadItems: number;
}
export interface FacturaDetalleRequestDTO {
  listaPrecioArticuloId: number;
  cantidad: number;
  descripcion?: string | null;
  porcentajeBonificacion?: number;
}
export interface FacturaRequestDTO {
  fechaEmision: string;
  puntoVentaId: number;
  clienteId: number | null;
  condicionIvaId: number;
  tipoMonedaId: number;
  importeCobrado: number;
  observaciones: string | null;
  detalles: FacturaDetalleRequestDTO[];
}
export interface FacturaFiltrosDTO {
  fechaDesde?: string;
  fechaHasta?: string;
  estado?: 'EMITIDA' | 'ANULADA';
  montoMinimo?: number;
}
/** DTO adicional, solo para acciones CRUD: el reporte ejecutivo mantiene EXACTAMENTE sus 7 campos. */
export interface FacturaGestionDTO extends FacturaReporteDTO {id: number; estado: string; cae: string|null; puntoVentaFiscal: number|null; numeroFiscal: number|null;}
export interface FacturaEdicionDTO {id: number; estado: string; factura: FacturaRequestDTO;}
export interface OpcionDTO {id: number; descripcion: string;}
export interface PrecioArticuloOpcionDTO extends OpcionDTO {articuloId:number; listaPrecioId:number; listaPrecioDenominacion:string; precioNeto:number; alicuotaIva:number; codigo:string|null; rubro:string|null; marca:string|null;}
export interface CatalogosDTO {
  clientes: OpcionDTO[];
  puntosVenta: OpcionDTO[];
  condicionesIva: OpcionDTO[];
  tiposMoneda: OpcionDTO[];
  listasPrecio: OpcionDTO[];
  listaPreciosArticulos: PrecioArticuloOpcionDTO[];
}
