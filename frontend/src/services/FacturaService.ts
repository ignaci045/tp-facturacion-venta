import type {FacturaReporteDTO, FacturaRequestDTO, FacturaFiltrosDTO, FacturaGestionDTO, FacturaEdicionDTO, CatalogosDTO} from '../models/factura.dto.js';

const BACKEND = window.location.port === '5173' ? 'http://localhost:8080' : '';
const BASE = `${BACKEND}/api/facturas`;

async function solicitar<T>(url: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try { response = await fetch(url, {headers:{'Content-Type':'application/json'},...init}); }
  catch { throw new Error('No se pudo conectar con Spring Boot. Verificá que esté funcionando en el puerto 8080.'); }
  if(!response.ok) {
    const datos = await response.json().catch(()=>({error:`Error HTTP ${response.status}`})) as {error?: string};
    throw new Error(datos.error || `Error HTTP ${response.status}`);
  }
  if(response.status === 204) return undefined as T;
  return await response.json() as T;
}
export function queryString(filtros: FacturaFiltrosDTO): string {
  const params = new URLSearchParams();
  for (const [clave,valor] of Object.entries(filtros)) {
    if (valor !== undefined && valor !== null && valor !== '') params.set(clave,String(valor));
  }
  return params.toString() ? `?${params.toString()}` : '';
}
export class FacturaService {
  diagnosticoArca(): Promise<{autenticado:boolean;ambiente:string;puntosVenta:{numero:string;emisionTipo:string;bloqueado:string}[];detalle:string}> {
    return solicitar(BACKEND+'/api/arca/diagnostico');
  }
  estadoArca(): Promise<{configurado:boolean;ambiente:string;detalle:string}> {
    return solicitar(`${BACKEND}/api/arca/estado`);
  }
  emitirArca(id:number, datos:{puntoVentaFiscal:number;tipoComprobante:number;tipoDocumento:number;numeroDocumento:number;condicionIvaReceptor:number}): Promise<{cae:string;numeroFiscal:number;vencimiento:string}> {
    return solicitar(`${BACKEND}/api/arca/facturas/${id}/emitir`,{method:'POST',body:JSON.stringify(datos)});
  }
  // Métodos solicitados literalmente por el segundo TP
  obtenerFacturas(filtros: FacturaFiltrosDTO = {}): Promise<FacturaReporteDTO[]> {
    return solicitar<FacturaReporteDTO[]>(BASE + queryString(filtros));
  }
  crearFactura(factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
    return solicitar<FacturaReporteDTO>(BASE,{method:'POST',body:JSON.stringify(factura)});
  }
  actualizarEstado(id: number, estado: string, observaciones?: string): Promise<FacturaReporteDTO> {
    return solicitar<FacturaReporteDTO>(`${BASE}/${id}/estado`,{method:'PUT',body:JSON.stringify({estado,observaciones: observaciones ?? null})});
  }
  eliminarFactura(id: number): Promise<void> {
    return solicitar<void>(`${BASE}/${id}`,{method:'DELETE'});
  }
  // Soporte adicional: edición completa y acciones por ID sin modificar FacturaReporteDTO
  obtenerFacturasGestion(filtros: FacturaFiltrosDTO = {}): Promise<FacturaGestionDTO[]> {
    return solicitar<FacturaGestionDTO[]>(`${BASE}/gestion${queryString(filtros)}`);
  }
  obtenerFactura(id: number): Promise<FacturaEdicionDTO> {
    return solicitar<FacturaEdicionDTO>(`${BASE}/${id}`);
  }
  actualizarFactura(id: number, factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
    return solicitar<FacturaReporteDTO>(`${BASE}/${id}`,{method:'PUT',body:JSON.stringify(factura)});
  }
  obtenerCatalogos(): Promise<CatalogosDTO> {return solicitar<CatalogosDTO>(`${BACKEND}/api/catalogos`);}
  urlPdfFactura(id:number): string {return BASE+'/'+id+'/pdf';}
  urlExportar(tipo: 'pdf'|'xlsx', filtros:FacturaFiltrosDTO): string {return `${BASE}/reporte.${tipo}${queryString(filtros)}`;}
}
