import { FacturaService } from './services/FacturaService.js';
const api = new FacturaService();
let catalogos = { clientes: [], puntosVenta: [], condicionesIva: [], tiposMoneda: [], listasPrecio: [], listaPreciosArticulos: [] };
let renglones = [];
let editando = null;
let filtradas = [];
let pagina = 1;
let paginaClientes = 0;
let cuentaId = null;
let cuentaNombre = '';
let editandoCliente = null;
let editandoArticulo = null;
let paginaGestionArticulos = 1;
let paginaArticulo = 1;
const TAM_PAGINA = 10;
const TAM_ARTICULOS = 8;
let articuloParaCambiar = null;
const preciosSeleccionados = new Set();
function abrirVista(tipo) {
    $('vistaArticulos').hidden = true;
    $('vistaClientes').hidden = true;
    cuentaId = null;
    $('tituloListado').textContent = 'Listado de comprobantes';
    $('vistaBusqueda').hidden = tipo !== 'buscar';
    $('vistaListado').hidden = tipo !== 'buscar';
    $('seccionAlta').hidden = tipo !== 'alta';
    $('seccionAlta').closest('main')?.querySelector('.inicio')?.setAttribute('hidden', '');
    if (tipo === 'buscar')
        $('numeroBuscado').focus();
    if (tipo === 'alta')
        $('fechaEmision').focus();
}
function cerrarVista() {
    $('vistaArticulos').hidden = true;
    $('vistaClientes').hidden = true;
    cuentaId = null;
    $('vistaBusqueda').hidden = true;
    $('vistaListado').hidden = true;
    $('seccionAlta').hidden = true;
    $('seccionAlta').closest('main')?.querySelector('.inicio')?.removeAttribute('hidden');
}
function mostrarArticulos(indice = null) {
    preciosSeleccionados.clear();
    articuloParaCambiar = indice;
    paginaArticulo = 1;
    $('busquedaArticulo').value = '';
    $('selectorArticulos').hidden = false;
    dibujarArticulos();
    $('busquedaArticulo').focus();
}
function dibujarArticulos() {
    const texto = normalizar($('busquedaArticulo').value.trim());
    const listaId = $('listaPrecioBuscada').value;
    const lista = catalogos.listaPreciosArticulos.filter(a => (!listaId || String(a.listaPrecioId) === listaId) && (normalizar(a.descripcion + ' ' + a.listaPrecioDenominacion + ' ' + (a.codigo ?? '') + ' ' + (a.rubro ?? '') + ' ' + (a.marca ?? '')).includes(texto) || String(a.id) === texto));
    const articulos = [...new Map(lista.map(a => [a.articuloId, { id: a.articuloId, descripcion: a.descripcion, iva: a.alicuotaIva, codigo: a.codigo, rubro: a.rubro, marca: a.marca }])).values()];
    const columnas = catalogos.listasPrecio.filter(l => !listaId || String(l.id) === listaId);
    const total = Math.max(1, Math.ceil(articulos.length / TAM_ARTICULOS));
    paginaArticulo = Math.min(paginaArticulo, total);
    const filas = articulos.slice((paginaArticulo - 1) * TAM_ARTICULOS, paginaArticulo * TAM_ARTICULOS).map(a => `<tr data-articulo-id="${a.id}"><th scope="row">${seguro(a.descripcion)}<small class="precio-lista">${seguro(a.codigo)} · ${seguro(a.rubro)} · ${seguro(a.marca)}</small><small class="precio-lista">IVA ${a.iva}%</small></th>${columnas.map(l => {
        const opciones = lista.filter(p => p.articuloId === a.id && p.listaPrecioId === l.id);
        return `<td>${opciones.map(p => `<label class="precio-celda"><input type="${articuloParaCambiar === null ? 'checkbox' : 'radio'}" name="precioSeleccionado" aria-label="${seguro(a.descripcion + ' · ' + l.descripcion + ' · Precio #' + p.id)}" data-precio-id="${p.id}" ${preciosSeleccionados.has(p.id) ? 'checked' : ''}><span>${peso(p.precioNeto)}<small class="precio-lista">Precio #${p.id}</small></span></label>`).join('') || '<span class="precio-ausente">Sin precio</span>'}</td>`;
    }).join('')}</tr>`).join('');
    $('resultadosArticulo').innerHTML = filas ? `<table class="tabla-precios"><caption>Precios netos por lista</caption><thead><tr><th scope="col">Artículo</th>${columnas.map(l => `<th scope="col">${seguro(l.descripcion)}</th>`).join('')}</tr></thead><tbody>${filas}</tbody></table>` : '<p class="vacio">No hay artículos con ese nombre o precio.</p>';
    $('estadoArticulo').textContent = `Página ${paginaArticulo} de ${total} · ${articulos.length} artículos · ${lista.length} precios`;
    $('articuloAnterior').disabled = paginaArticulo <= 1;
    $('articuloSiguiente').disabled = paginaArticulo >= total;
    $('seleccionarPagina').hidden = articuloParaCambiar !== null;
    $('ayudaSelector').textContent = articuloParaCambiar === null ? 'Marcá varios precios y agregalos juntos. La selección se conserva al buscar y cambiar de página o lista.' : 'Elegí un precio para reemplazar el de este renglón.';
    actualizarSeleccion();
}
function actualizarSeleccion() {
    const cantidad = preciosSeleccionados.size;
    $('cantidadSeleccionados').textContent = `${cantidad} ${cantidad === 1 ? 'precio seleccionado' : 'precios seleccionados'}`;
    const boton = $('confirmarArticulos');
    boton.disabled = cantidad === 0;
    boton.textContent = articuloParaCambiar === null ? `Agregar seleccionados (${cantidad})` : 'Reemplazar precio';
    $('limpiarSeleccionArticulos').disabled = cantidad === 0;
}
function dibujarFacturas() {
    const total = Math.max(1, Math.ceil(filtradas.length / TAM_PAGINA));
    pagina = Math.min(pagina, total);
    dibujarFilas();
    $('estadoPagina').textContent = `Página ${pagina} de ${total} · ${filtradas.length} resultados`;
    $('paginaAnterior').disabled = pagina <= 1;
    $('paginaSiguiente').disabled = pagina >= total;
}
const $ = (id) => document.getElementById(id);
const normalizar = (texto) => texto.toLocaleLowerCase('es').normalize('NFD').replace(/[\u0300-\u0302\u0304-\u036f]/g, '');
const num = (v) => Number(v.replace(',', '.'));
const peso = (v) => new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(v);
const seguro = (v) => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] ?? c));
const hoy = () => {
    const d = new Date();
    const p = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
};
function aviso(mensaje, fallo = false) {
    const elemento = $('mensaje');
    elemento.textContent = mensaje;
    elemento.className = `mensaje visible ${fallo ? 'error' : 'exito'}`;
    setTimeout(() => elemento.classList.remove('visible'), 5200);
}
async function ejecutar(fn) {
    try {
        await fn();
    }
    catch (error) {
        aviso(error instanceof Error ? error.message : 'Error desconocido', true);
    }
}
function opciones(id, lista, primera) {
    const select = $(id);
    select.innerHTML = (primera === undefined ? '' : `<option value="">${seguro(primera)}</option>`)
        + lista.map(op => `<option value="${op.id}">${seguro(op.descripcion)}</option>`).join('');
}
function filtros() {
    const desde = $('fechaDesde').value;
    const hasta = $('fechaHasta').value;
    const estado = $('filtroEstado').value;
    const importe = $('montoMinimo').value;
    return {
        ...(desde ? { fechaDesde: desde } : {}), ...(hasta ? { fechaHasta: hasta } : {}),
        ...(estado ? { estado: estado } : {}),
        ...(importe !== '' ? { montoMinimo: num(importe) } : {})
    };
}
function coincideNumero(f, texto) {
    if (!texto)
        return true;
    if (/^\d+$/.test(texto)) {
        return Number(texto) === f.numeroFactura || Number(texto) === f.numeroFiscal;
    }
    const fiscal = /^(\d+)-(\d+)$/.exec(texto);
    return fiscal !== null && Number(fiscal[1]) === f.puntoVentaFiscal && Number(fiscal[2]) === f.numeroFiscal;
}
async function refrescar() {
    if (cuentaId !== null) {
        filtradas = await clientesApi('/' + cuentaId + '/facturas');
        pagina = 1;
        dibujarFacturas();
        return;
    }
    filtradas = await api.obtenerFacturasGestion(filtros());
    const numero = $('numeroBuscado').value.trim().replace(/^#/, '');
    const cliente = normalizar($('clienteBuscado').value.trim());
    filtradas = filtradas.filter(f => coincideNumero(f, numero) && normalizar(f.clienteDenominacion).includes(cliente));
    pagina = 1;
    dibujarFacturas();
}
function dibujarFilas() {
    const tbody = $('filasFacturas');
    tbody.innerHTML = '';
    for (const f of filtradas.slice((pagina - 1) * TAM_PAGINA, pagina * TAM_PAGINA)) {
        const fila = document.createElement('tr');
        const anulada = f.estado === 'ANULADA';
        const autorizado = Boolean(f.cae);
        const fiscal = autorizado ? `<br><small>Fiscal ${seguro(f.puntoVentaFiscal)}-${String(f.numeroFiscal).padStart(8, '0')}<br>CAE ${seguro(f.cae)}</small>` : '';
        fila.innerHTML = `<td><strong>#${seguro(f.numeroFactura)}</strong>${fiscal}</td><td>${seguro(f.fechaEmision)}</td>
      <td>${seguro(f.clienteDenominacion)}</td><td>${seguro(f.condicionIva)}</td>
      <td>${seguro(f.puntoVentaDescripcion)}</td><td class="num">${f.cantidadItems}</td>
      <td class="num"><strong>${peso(f.importeTotal)}</strong></td>
      <td><span class="estado ${anulada ? 'anulada' : 'emitida'}">${seguro(f.estado)}</span></td>
      <td><div class="acciones"><button type="button" class="link" data-accion="pdf" data-id="${f.id}" title="Descargar sólo esta factura">PDF</button><button type="button" class="link" data-accion="arca" data-id="${f.id}" ${anulada || autorizado ? 'disabled' : ''} title="Autorizar factura en homologación de ARCA">ARCA (prueba)</button><button type="button" class="link" data-accion="editar" data-id="${f.id}" ${anulada || autorizado ? 'disabled' : ''}>Editar</button>
      <button type="button" class="link warning" data-accion="anular" data-id="${f.id}" ${anulada || autorizado ? 'disabled' : ''}>Anular</button>
      <button type="button" class="link danger" data-accion="eliminar" data-id="${f.id}" ${autorizado ? 'disabled' : ''}>Eliminar</button></div></td>`;
        tbody.appendChild(fila);
    }
    $('vacio').hidden = filtradas.length !== 0;
    $('cantidadFacturas').textContent = String(filtradas.length);
    $('importeFacturas').textContent = peso(filtradas.reduce((t, f) => t + f.importeTotal, 0));
    $('cantidadAnuladas').textContent = String(filtradas.filter(f => f.estado === 'ANULADA').length);
}
function redibujarRenglones() {
    const cuerpo = $('detalles');
    cuerpo.innerHTML = '';
    renglones.forEach((item, indice) => {
        const articulo = catalogos.listaPreciosArticulos.find(a => a.id === item.listaPrecioArticuloId);
        const linea = document.createElement('tr');
        linea.innerHTML = `<td><div class="articulo-seleccionado"><span>${seguro(articulo?.descripcion ?? 'Precio no disponible')}<small class="precio-lista">${seguro(articulo?.listaPrecioDenominacion ?? 'Lista no disponible')} · Precio #${item.listaPrecioArticuloId}</small></span><button type="button" class="btn btn-secondary" data-accion="cambiar" data-i="${indice}">Buscar…</button></div></td>
      <td><textarea aria-label="Descripción de renglón ${indice + 1}" data-col="descripcion" data-i="${indice}" maxlength="1200" rows="2">${seguro(item.descripcion ?? articulo?.descripcion ?? '')}</textarea></td>
      <td><input aria-label="Cantidad de renglón ${indice + 1}" data-col="cantidad" data-i="${indice}" type="number" min="0.001" step="0.001" value="${item.cantidad}"></td>
      <td class="num">${peso(articulo?.precioNeto ?? 0)}</td><td><input aria-label="Bonificación de renglón ${indice + 1}" data-col="porcentajeBonificacion" data-i="${indice}" type="number" min="0" max="100" step="0.01" value="${item.porcentajeBonificacion ?? 0}"></td><td class="num">${articulo?.alicuotaIva ?? 0}%</td>
      <td class="num total-renglon">${peso((articulo?.precioNeto ?? 0) * item.cantidad * (1 - (item.porcentajeBonificacion ?? 0) / 100) * (1 + (articulo?.alicuotaIva ?? 0) / 100))}</td>
      <td><button type="button" class="link danger" data-accion="quitar" data-i="${indice}" aria-label="Quitar renglón">Quitar</button></td>`;
        cuerpo.appendChild(linea);
    });
    recalcular();
}
function recalcular() {
    let neto = 0, iva = 0;
    for (const r of renglones) {
        const a = catalogos.listaPreciosArticulos.find(x => x.id === r.listaPrecioArticuloId);
        if (a) {
            const base = Math.round(a.precioNeto * r.cantidad * (1 - (r.porcentajeBonificacion ?? 0) / 100) * 100) / 100;
            neto += base;
            iva += Math.round(base * a.alicuotaIva) / 100;
        }
    }
    $('netoPrevio').textContent = peso(neto);
    $('ivaPrevia').textContent = peso(iva);
    $('totalPrevio').textContent = peso(neto + iva);
}
function poblarCatalogos() {
    opciones('puntoVentaId', catalogos.puntosVenta);
    opciones('condicionIvaId', catalogos.condicionesIva);
    opciones('tipoMonedaId', catalogos.tiposMoneda);
    opciones('listaPrecioBuscada', catalogos.listasPrecio, 'Todas las listas');
    filtrarClientes();
}
async function nuevaFactura() {
    const boton = $('abrirAlta');
    boton.disabled = true;
    try {
        catalogos = await api.obtenerCatalogos();
        poblarCatalogos();
        limpiar();
        abrirVista('alta');
        if (!catalogos.puntosVenta.length || !catalogos.condicionesIva.length || !catalogos.tiposMoneda.length)
            aviso('Faltan catálogos para facturar. Reiniciá el servidor con los datos de demostración habilitados.', true);
    }
    finally {
        boton.disabled = false;
    }
}
function limpiar() {
    editando = null;
    $('formFactura').reset();
    filtrarClientes();
    $('fechaEmision').value = hoy();
    $('importeCobrado').value = '0';
    $('tituloFactura').textContent = 'Nueva factura';
    $('guardarFactura').textContent = 'Crear factura';
    $('cancelarEdicion').hidden = true;
    renglones = [];
    redibujarRenglones();
}
function formulario() {
    const requerido = (id) => {
        const v = $(id).value;
        if (!v)
            throw new Error(`Completá el campo ${id}`);
        return Number(v);
    };
    if (!renglones.length)
        throw new Error('Agregá al menos un artículo');
    if (renglones.some(d => !Number.isFinite(d.porcentajeBonificacion ?? 0) || (d.porcentajeBonificacion ?? 0) < 0 || (d.porcentajeBonificacion ?? 0) > 100))
        throw new Error('La bonificación debe estar entre 0 y 100%');
    if (renglones.some(d => !Number.isFinite(d.cantidad) || d.cantidad <= 0))
        throw new Error('Las cantidades deben ser positivas');
    return { fechaEmision: $('fechaEmision').value,
        puntoVentaId: requerido('puntoVentaId'), clienteId: requerido('clienteId'),
        condicionIvaId: requerido('condicionIvaId'), tipoMonedaId: requerido('tipoMonedaId'),
        importeCobrado: num($('importeCobrado').value),
        observaciones: $('observaciones').value || null,
        detalles: renglones.map(d => ({ ...d, descripcion: d.descripcion ?? catalogos.listaPreciosArticulos.find(a => a.id === d.listaPrecioArticuloId)?.descripcion ?? null, porcentajeBonificacion: d.porcentajeBonificacion ?? 0 })) };
}
async function accionEnFila(event) {
    const btn = event.target.closest('button[data-accion]');
    if (!btn)
        return;
    const id = Number(btn.dataset.id);
    if (btn.dataset.accion === 'pdf') {
        window.open(api.urlPdfFactura(id), '_blank', 'noopener');
        return;
    }
    if (btn.dataset.accion === 'arca') {
        const status = await api.estadoArca();
        if (!status.configurado)
            throw new Error('ARCA no configurada: revisá el archivo INTEGRACION_ARCA.md');
        // Estos valores son datos fiscales, NO identificadores internos de catálogos.
        const pedir = (texto, defecto) => { const v = prompt(texto, defecto); if (v === null)
            return null; const n = Number(v); if (!Number.isSafeInteger(n) || n < 0)
            throw new Error(`Valor no válido: ${texto}`); return n; };
        const puntoVentaFiscal = pedir('Punto de venta fiscal habilitado en ARCA:', '1');
        if (puntoVentaFiscal === null)
            return;
        const tipoComprobante = pedir('Tipo de comprobante fiscal: 1=A, 6=B, 11=C', '6');
        if (tipoComprobante === null)
            return;
        const tipoDocumento = pedir('Documento receptor: 80=CUIT, 96=DNI, 99=Sin identificar', '99');
        if (tipoDocumento === null)
            return;
        const numeroDocumento = pedir('Número de documento (0 cuando no se identifica)', '0');
        if (numeroDocumento === null)
            return;
        const condicionIvaReceptor = pedir('Código de condición IVA receptor según tabla ARCA (ej.: 5=Consumidor Final)', '5');
        if (condicionIvaReceptor === null)
            return;
        if (!confirm('Se enviará este comprobante al entorno de HOMOLOGACIÓN de ARCA. ¿Continuar?'))
            return;
        const r = await api.emitirArca(id, { puntoVentaFiscal, tipoComprobante, tipoDocumento, numeroDocumento, condicionIvaReceptor });
        aviso(`Autorizada en homologación: comprobante ${r.numeroFiscal}, CAE ${r.cae}`);
        await refrescar();
    }
    if (btn.dataset.accion === 'editar') {
        const datos = await api.obtenerFactura(id);
        editando = id;
        const factura = datos.factura;
        $('buscarClienteAlta').value = '';
        filtrarClientes();
        for (const key of ['fechaEmision', 'puntoVentaId', 'clienteId', 'condicionIvaId', 'tipoMonedaId', 'importeCobrado', 'observaciones']) {
            const campo = $(key);
            const valor = factura[key];
            campo.value = valor === null ? '' : String(valor);
        }
        renglones = factura.detalles.map(x => ({ ...x }));
        redibujarRenglones();
        $('tituloFactura').textContent = `Editar factura #${filtradas.find(x => x.id === id)?.numeroFactura ?? id}`;
        $('guardarFactura').textContent = 'Guardar cambios';
        $('cancelarEdicion').hidden = false;
        abrirVista('alta');
    }
    if (btn.dataset.accion === 'anular') {
        if (!confirm('¿Anular esta factura? Quedará registrada como ANULADA.'))
            return;
        await api.actualizarEstado(id, 'ANULADA', 'Anulada desde la interfaz');
        if (editando === id)
            limpiar();
        aviso('Factura anulada correctamente');
        await refrescar();
    }
    if (btn.dataset.accion === 'eliminar') {
        if (!confirm('¿ELIMINAR DEFINITIVAMENTE esta factura y todos sus ítems? Esta acción no se puede deshacer.'))
            return;
        await api.eliminarFactura(id);
        if (editando === id)
            limpiar();
        aviso('Factura eliminada definitivamente');
        await refrescar();
    }
}
function filtrarClientes() {
    const selector = $('clienteId');
    const seleccionado = selector.value;
    const texto = normalizar($('buscarClienteAlta').value.trim());
    const clientes = catalogos.clientes.filter(c => String(c.id) === seleccionado || normalizar(c.descripcion).includes(texto));
    opciones('clienteId', clientes, 'Seleccioná un cliente existente');
    selector.value = seleccionado;
}
async function clientesApi(ruta, opciones) {
    const r = await fetch('/api/clientes' + ruta, opciones);
    const datos = await r.json();
    if (!r.ok)
        throw new Error(datos.error ?? 'No se pudo consultar clientes');
    return datos;
}
async function abrirClientes() {
    cerrarVista();
    document.querySelector('.inicio')?.setAttribute('hidden', '');
    $('vistaClientes').hidden = false;
    $('cuentaCliente').textContent = '';
    paginaClientes = 0;
    await cargarClientes();
}
async function cargarClientes() {
    cuentaId = null;
    $('vistaListado').hidden = true;
    $('cuentaCliente').textContent = '';
    const q = $('consultaCliente').value.trim();
    const datos = await clientesApi('?q=' + encodeURIComponent(q) + '&page=' + paginaClientes + '&size=10');
    $('filasClientes').innerHTML = datos.content.map(c => `<tr><td>#${c.id}</td><td>${seguro(c.denominacion)}</td><td>${seguro(c.documento)}<br>${seguro(c.tipoCliente ?? 'MINORISTA')}</td><td>${seguro([c.contacto?.email, c.contacto?.telefono, c.contacto?.celular].filter(Boolean).join(' · '))}</td><td>${seguro([c.domicilio?.nombreCalle, c.domicilio?.numeroCalle, c.domicilio?.localidad, c.domicilio?.provincia, c.domicilio?.codigoPostal].filter(Boolean).join(' '))}</td><td><button type="button" class="btn btn-secondary" data-editar-cliente="${c.id}">Editar</button><button type="button" class="btn btn-secondary" data-cuenta="${c.id}" data-nombre="${seguro(c.denominacion)}">Ver facturas</button></td></tr>`).join('') || '<tr><td colspan="6">No se encontraron clientes.</td></tr>';
    $('estadoClientes').textContent = 'Página ' + (paginaClientes + 1) + ' de ' + Math.max(1, datos.totalPages) + ' · ' + datos.totalElements + ' clientes';
    $('clientesAnterior').disabled = paginaClientes === 0;
    $('clientesSiguiente').disabled = paginaClientes + 1 >= datos.totalPages;
}
function limpiarCliente() { editandoCliente = null; $('formCliente').reset(); $('guardarCliente').textContent = 'Crear cliente'; $('cancelarCliente').hidden = true; }
function dibujarGestionArticulos() {
    const texto = normalizar($('buscarArticuloGestion').value);
    const lista = [...new Map(catalogos.listaPreciosArticulos.map(a => [a.articuloId, a])).values()].filter(a => normalizar([a.codigo, a.descripcion, a.rubro, a.marca].join(' ')).includes(texto));
    const paginas = Math.max(1, Math.ceil(lista.length / 10));
    paginaGestionArticulos = Math.min(paginaGestionArticulos, paginas);
    $('filasArticulos').innerHTML = lista.slice((paginaGestionArticulos - 1) * 10, paginaGestionArticulos * 10).map(a => `<tr><td>${seguro(a.codigo)}</td><td>${seguro(a.descripcion)}</td><td>${seguro(a.rubro)}</td><td>${seguro(a.marca)}</td><td><button type="button" class="btn btn-secondary" data-editar-articulo="${a.articuloId}">Editar</button></td></tr>`).join('');
    $('estadoGestionArticulos').textContent = `Página ${paginaGestionArticulos} de ${paginas} · ${lista.length} artículos`;
    $('gestionArticuloAnterior').disabled = paginaGestionArticulos <= 1;
    $('gestionArticuloSiguiente').disabled = paginaGestionArticulos >= paginas;
}
async function iniciar() {
    $('abrirArticulos').addEventListener('click', () => { void ejecutar(async () => { cerrarVista(); document.querySelector('.inicio')?.setAttribute('hidden', ''); catalogos = await api.obtenerCatalogos(); $('vistaArticulos').hidden = false; paginaGestionArticulos = 1; dibujarGestionArticulos(); }); });
    $('buscarArticuloGestion').addEventListener('input', () => { paginaGestionArticulos = 1; dibujarGestionArticulos(); });
    $('gestionArticuloAnterior').addEventListener('click', () => { paginaGestionArticulos--; dibujarGestionArticulos(); });
    $('gestionArticuloSiguiente').addEventListener('click', () => { paginaGestionArticulos++; dibujarGestionArticulos(); });
    $('cancelarArticulo').addEventListener('click', () => { editandoArticulo = null; $('formArticulo').hidden = true; });
    $('filasArticulos').addEventListener('click', e => { const boton = e.target.closest('[data-editar-articulo]'); if (!boton)
        return; const a = catalogos.listaPreciosArticulos.find(a => a.articuloId === Number(boton.dataset.editarArticulo)); editandoArticulo = a.articuloId; for (const k of ['codigo', 'descripcion', 'rubro', 'marca'])
        $(k + 'Articulo').value = a[k] ?? ''; $('formArticulo').hidden = false; });
    $('formArticulo').addEventListener('submit', e => { e.preventDefault(); void ejecutar(async () => { if (editandoArticulo === null)
        return; const datos = Object.fromEntries(['codigo', 'descripcion', 'rubro', 'marca'].map(k => [k, $(k + 'Articulo').value])); const r = await fetch('/api/articulos/' + editandoArticulo, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(datos) }); if (!r.ok) {
        const error = await r.json();
        throw new Error(error.error ?? 'No se pudo guardar el artículo');
    } catalogos = await api.obtenerCatalogos(); $('formArticulo').hidden = true; editandoArticulo = null; dibujarGestionArticulos(); aviso('Artículo actualizado'); }); });
    $('comprobarArca').addEventListener('click', () => {
        void ejecutar(async () => {
            const boton = $('comprobarArca');
            boton.disabled = true;
            $('resultadoArca').textContent = 'Comprobando conexión con ARCA...';
            try {
                const r = await api.diagnosticoArca();
                $('resultadoArca').textContent = r.detalle + (r.puntosVenta.length ? ' · Puntos: ' + r.puntosVenta.map(p => p.numero + ' (' + p.emisionTipo + ', bloqueado: ' + p.bloqueado + ')').join(', ') : '');
            }
            catch (e) {
                $('resultadoArca').textContent = e instanceof Error ? e.message : 'Error de conexión';
                throw e;
            }
            finally {
                boton.disabled = false;
            }
        });
    });
    $('abrirClientes').addEventListener('click', () => { void ejecutar(abrirClientes); });
    $('formBuscarClientes').addEventListener('submit', e => { e.preventDefault(); paginaClientes = 0; void ejecutar(cargarClientes); });
    $('clientesAnterior').addEventListener('click', () => { paginaClientes--; void ejecutar(cargarClientes); });
    $('clientesSiguiente').addEventListener('click', () => { paginaClientes++; void ejecutar(cargarClientes); });
    $('cancelarCliente').addEventListener('click', limpiarCliente);
    $('formCliente').addEventListener('submit', e => {
        e.preventDefault();
        void ejecutar(async () => {
            const campos = { denominacion: 'nombreNuevoCliente', documento: 'documentoNuevoCliente', tipoCliente: 'tipoNuevoCliente', email: 'emailNuevoCliente', telefono: 'telefonoNuevoCliente', celular: 'celularNuevoCliente', nombreCalle: 'calleNuevoCliente', numeroCalle: 'numeroNuevoCliente', localidad: 'localidadNuevoCliente', provincia: 'provinciaNuevoCliente', codigoPostal: 'postalNuevoCliente' };
            const entrada = Object.fromEntries(Object.entries(campos).map(([k, id]) => [k, $(id).value]));
            await clientesApi(editandoCliente === null ? '' : '/' + editandoCliente, { method: editandoCliente === null ? 'POST' : 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(entrada) });
            limpiarCliente();
            $('formCliente').reset();
            catalogos = await api.obtenerCatalogos();
            filtrarClientes();
            $('consultaCliente').value = '';
            paginaClientes = 0;
            await cargarClientes();
            aviso('Datos del cliente guardados. Ya podés seleccionarlo al facturar.');
        });
    });
    $('filasClientes').addEventListener('click', e => {
        const editar = e.target.closest('button[data-editar-cliente]');
        if (editar) {
            void ejecutar(async () => {
                const c = await clientesApi('/' + editar.dataset.editarCliente);
                editandoCliente = Number(editar.dataset.editarCliente);
                const valores = { nombreNuevoCliente: c.denominacion, documentoNuevoCliente: c.documento, tipoNuevoCliente: c.tipoCliente ?? 'MINORISTA', emailNuevoCliente: c.contacto?.email, telefonoNuevoCliente: c.contacto?.telefono, celularNuevoCliente: c.contacto?.celular, calleNuevoCliente: c.domicilio?.nombreCalle, numeroNuevoCliente: c.domicilio?.numeroCalle, localidadNuevoCliente: c.domicilio?.localidad, provinciaNuevoCliente: c.domicilio?.provincia, postalNuevoCliente: c.domicilio?.codigoPostal };
                for (const [id, v] of Object.entries(valores))
                    $(id).value = v ?? '';
                $('guardarCliente').textContent = 'Guardar cliente';
                $('cancelarCliente').hidden = false;
                $('formCliente').scrollIntoView({ block: 'start' });
            });
            return;
        }
        const b = e.target.closest('button[data-cuenta]');
        if (!b)
            return;
        void ejecutar(async () => {
            cuentaId = Number(b.dataset.cuenta);
            cuentaNombre = b.dataset.nombre ?? '';
            $('cuentaCliente').textContent = 'Cuenta #' + cuentaId + ' · ' + cuentaNombre + ' · Todas las facturas vinculadas';
            $('tituloListado').textContent = 'Facturas de ' + cuentaNombre;
            $('vistaListado').hidden = false;
            await refrescar();
        });
    });
    $('buscarClienteAlta').addEventListener('input', filtrarClientes);
    limpiar();
    $('abrirBusqueda').addEventListener('click', () => { abrirVista('buscar'); void ejecutar(refrescar); });
    $('abrirAlta').addEventListener('click', () => { void ejecutar(nuevaFactura); });
    document.querySelectorAll('.cerrar-vista').forEach(b => b.addEventListener('click', cerrarVista));
    $('paginaAnterior').addEventListener('click', () => { if (pagina > 1) {
        pagina--;
        dibujarFacturas();
    } });
    $('paginaSiguiente').addEventListener('click', () => { pagina++; dibujarFacturas(); });
    $('cerrarArticulos').addEventListener('click', () => { $('selectorArticulos').hidden = true; });
    $('selectorArticulos').addEventListener('click', e => { if (e.target === $('selectorArticulos'))
        $('selectorArticulos').hidden = true; });
    document.addEventListener('keydown', e => { if (e.key === 'Escape' && !$('selectorArticulos').hidden)
        $('selectorArticulos').hidden = true; });
    $('listaPrecioBuscada').addEventListener('change', () => { paginaArticulo = 1; dibujarArticulos(); });
    $('busquedaArticulo').addEventListener('input', () => { paginaArticulo = 1; dibujarArticulos(); });
    $('articuloAnterior').addEventListener('click', () => { if (paginaArticulo > 1) {
        paginaArticulo--;
        dibujarArticulos();
    } });
    $('articuloSiguiente').addEventListener('click', () => { paginaArticulo++; dibujarArticulos(); });
    $('resultadosArticulo').addEventListener('change', e => {
        const campo = e.target;
        if (!campo.matches('input[data-precio-id]'))
            return;
        if (articuloParaCambiar !== null)
            preciosSeleccionados.clear();
        const id = Number(campo.dataset.precioId);
        if (campo.checked)
            preciosSeleccionados.add(id);
        else
            preciosSeleccionados.delete(id);
        actualizarSeleccion();
    });
    $('seleccionarPagina').addEventListener('click', () => {
        $('resultadosArticulo').querySelectorAll('input[data-precio-id]').forEach(c => preciosSeleccionados.add(Number(c.dataset.precioId)));
        dibujarArticulos();
    });
    $('limpiarSeleccionArticulos').addEventListener('click', () => { preciosSeleccionados.clear(); dibujarArticulos(); });
    $('confirmarArticulos').addEventListener('click', () => {
        if (!preciosSeleccionados.size)
            return;
        if (articuloParaCambiar === null) {
            for (const listaPrecioArticuloId of preciosSeleccionados) {
                const existente = renglones.find(r => r.listaPrecioArticuloId === listaPrecioArticuloId);
                if (existente)
                    existente.cantidad += 1;
                else
                    renglones.push({ listaPrecioArticuloId, cantidad: 1, descripcion: catalogos.listaPreciosArticulos.find(a => a.id === listaPrecioArticuloId)?.descripcion, porcentajeBonificacion: 0 });
            }
        }
        else {
            const r = renglones[articuloParaCambiar];
            const anterior = catalogos.listaPreciosArticulos.find(a => a.id === r.listaPrecioArticuloId)?.descripcion;
            r.listaPrecioArticuloId = preciosSeleccionados.values().next().value;
            if (!r.descripcion || r.descripcion === anterior)
                r.descripcion = catalogos.listaPreciosArticulos.find(a => a.id === r.listaPrecioArticuloId)?.descripcion;
        }
        preciosSeleccionados.clear();
        $('selectorArticulos').hidden = true;
        redibujarRenglones();
    });
    $('formFiltros').addEventListener('submit', e => { e.preventDefault(); void ejecutar(refrescar); });
    $('limpiarFiltros').addEventListener('click', () => {
        $('formFiltros').reset();
        void ejecutar(refrescar);
    });
    $('exportarPdf').addEventListener('click', () => window.open(api.urlExportar('pdf', filtros()), '_blank'));
    $('exportarExcel').addEventListener('click', () => window.open(api.urlExportar('xlsx', filtros()), '_blank'));
    $('agregarDetalle').addEventListener('click', () => {
        if (!catalogos.listaPreciosArticulos.length)
            return aviso('No hay artículos con precio activo', true);
        mostrarArticulos();
    });
    $('detalles').addEventListener('click', e => {
        const boton = e.target.closest('button[data-accion="quitar"]');
        if (boton) {
            renglones.splice(Number(boton.dataset.i), 1);
            redibujarRenglones();
            return;
        }
        const cambiar = e.target.closest('button[data-accion="cambiar"]');
        if (cambiar)
            mostrarArticulos(Number(cambiar.dataset.i));
    });
    $('detalles').addEventListener('input', e => {
        const input = e.target;
        const indice = Number(input.dataset.i);
        if (!Number.isInteger(indice) || !renglones[indice])
            return;
        if (input.dataset.col === 'cantidad')
            renglones[indice].cantidad = num(input.value);
        if (input.dataset.col === 'descripcion')
            renglones[indice].descripcion = input.value;
        if (input.dataset.col === 'porcentajeBonificacion')
            renglones[indice].porcentajeBonificacion = num(input.value);
        const item = renglones[indice], a = catalogos.listaPreciosArticulos.find(x => x.id === item.listaPrecioArticuloId);
        const celda = input.closest('tr')?.querySelector('.total-renglon');
        if (celda) {
            const neto = Math.round((a?.precioNeto ?? 0) * item.cantidad * (1 - (item.porcentajeBonificacion ?? 0) / 100) * 100) / 100;
            celda.textContent = peso(neto + Math.round(neto * (a?.alicuotaIva ?? 0)) / 100);
        }
        recalcular();
    });
    $('formFactura').addEventListener('submit', e => {
        e.preventDefault();
        void ejecutar(async () => {
            const dato = formulario();
            if (editando === null) {
                const resultado = await api.crearFactura(dato);
                aviso(`Factura #${resultado.numeroFactura} creada`);
            }
            else {
                await api.actualizarFactura(editando, dato);
                aviso('Factura actualizada');
            }
            limpiar();
            cerrarVista();
        });
    });
    $('cancelarEdicion').addEventListener('click', () => { limpiar(); cerrarVista(); });
    $('filasFacturas').addEventListener('click', e => { void ejecutar(() => accionEnFila(e)); });
    catalogos = await api.obtenerCatalogos();
    poblarCatalogos();
    limpiar();
}
void ejecutar(iniciar);
