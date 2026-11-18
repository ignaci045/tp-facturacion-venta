const BACKEND = window.location.port === '5173' ? 'http://localhost:8080' : '';
const BASE = `${BACKEND}/api/facturas`;
async function solicitar(url, init) {
    let response;
    try {
        response = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...init });
    }
    catch {
        throw new Error('No se pudo conectar con Spring Boot. Verificá que esté funcionando en el puerto 8080.');
    }
    if (!response.ok) {
        const datos = await response.json().catch(() => ({ error: `Error HTTP ${response.status}` }));
        throw new Error(datos.error || `Error HTTP ${response.status}`);
    }
    if (response.status === 204)
        return undefined;
    return await response.json();
}
export function queryString(filtros) {
    const params = new URLSearchParams();
    for (const [clave, valor] of Object.entries(filtros)) {
        if (valor !== undefined && valor !== null && valor !== '')
            params.set(clave, String(valor));
    }
    return params.toString() ? `?${params.toString()}` : '';
}
export class FacturaService {
    diagnosticoArca() {
        return solicitar(BACKEND + '/api/arca/diagnostico');
    }
    estadoArca() {
        return solicitar(`${BACKEND}/api/arca/estado`);
    }
    emitirArca(id, datos) {
        return solicitar(`${BACKEND}/api/arca/facturas/${id}/emitir`, { method: 'POST', body: JSON.stringify(datos) });
    }
    // Métodos solicitados literalmente por el segundo TP
    obtenerFacturas(filtros = {}) {
        return solicitar(BASE + queryString(filtros));
    }
    crearFactura(factura) {
        return solicitar(BASE, { method: 'POST', body: JSON.stringify(factura) });
    }
    actualizarEstado(id, estado, observaciones) {
        return solicitar(`${BASE}/${id}/estado`, { method: 'PUT', body: JSON.stringify({ estado, observaciones: observaciones ?? null }) });
    }
    eliminarFactura(id) {
        return solicitar(`${BASE}/${id}`, { method: 'DELETE' });
    }
    // Soporte adicional: edición completa y acciones por ID sin modificar FacturaReporteDTO
    obtenerFacturasGestion(filtros = {}) {
        return solicitar(`${BASE}/gestion${queryString(filtros)}`);
    }
    obtenerFactura(id) {
        return solicitar(`${BASE}/${id}`);
    }
    actualizarFactura(id, factura) {
        return solicitar(`${BASE}/${id}`, { method: 'PUT', body: JSON.stringify(factura) });
    }
    obtenerCatalogos() { return solicitar(`${BACKEND}/api/catalogos`); }
    urlPdfFactura(id) { return BASE + '/' + id + '/pdf'; }
    urlExportar(tipo, filtros) { return `${BASE}/reporte.${tipo}${queryString(filtros)}`; }
}
