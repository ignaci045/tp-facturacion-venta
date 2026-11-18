package com.example.facturaventa.service;

import com.example.facturaventa.dto.*;
import com.example.facturaventa.exception.*;
import com.example.facturaventa.model.*;
import com.example.facturaventa.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FacturaService {
    private final FacturaVentaRepository facturas;
    private final ClienteRepository clientes;
    private final PuntoVentaRepository puntosVenta;
    private final CondicionIvaRepository condicionesIva;
    private final TipoMonedaRepository tiposMoneda;
    private final ListaPrecioArticuloRepository precios;
    @PersistenceContext private EntityManager em;

    /** Constructor JPQL: el DTO se obtiene directamente de la BD sin cargar las entidades relacionadas. */
    private static final String CAMPOS_REPORTE =
        "f.numero, f.fechaEmision, COALESCE(c.denominacion, 'Consumidor Final'), " +
        "ci.denominacion, pv.descripcion, f.importeTotal, COUNT(d)";
    private static final String FROM = " FROM FacturaVenta f " +
        " LEFT JOIN f.cliente c JOIN f.condicionIva ci JOIN f.puntoVenta pv " +
        " LEFT JOIN f.detalles d WHERE 1=1 ";
    private static final String GROUP = " GROUP BY f.id, f.estado, f.numero, f.fechaEmision, " +
        "c.denominacion, ci.denominacion, pv.descripcion, f.importeTotal, f.cae, f.puntoVentaFiscal, f.numeroFiscal ";

    @Transactional(readOnly=true)
    public List<FacturaReporteDTO> buscarFacturasFiltradas(LocalDate desde, LocalDate hasta, String estado, Double montoMinimo) {
        return consultar("SELECT new com.example.facturaventa.dto.FacturaReporteDTO("+CAMPOS_REPORTE+")",
            FacturaReporteDTO.class, desde, hasta, estado, montoMinimo, null, null);
    }

    @Transactional(readOnly=true)
    public List<FacturaGestionDTO> buscarGestion(LocalDate desde, LocalDate hasta, String estado, Double montoMinimo) {
        return consultar("SELECT new com.example.facturaventa.dto.FacturaGestionDTO(f.id, f.estado, "+CAMPOS_REPORTE+", f.cae, f.puntoVentaFiscal, f.numeroFiscal)",
            FacturaGestionDTO.class, desde, hasta, estado, montoMinimo, null, null);
    }

    @Transactional(readOnly=true)
    public List<FacturaGestionDTO> buscarPorCliente(Long clienteId) {
        clientes.findById(clienteId).orElseThrow(()->new RecursoNoEncontradoException("Cliente no encontrado"));
        return consultar("SELECT new com.example.facturaventa.dto.FacturaGestionDTO(f.id, f.estado, "+CAMPOS_REPORTE+", f.cae, f.puntoVentaFiscal, f.numeroFiscal)",
            FacturaGestionDTO.class,null,null,null,null,null,clienteId);
    }
    private <T> List<T> consultar(String select, Class<T> tipo, LocalDate desde, LocalDate hasta,
                                  String estado, Double montoMinimo, Long facturaId, Long clienteId) {
        if (desde != null && hasta != null && desde.isAfter(hasta))
            throw new ReglaNegocioException("La fecha desde debe ser anterior o igual a fecha hasta");
        if (montoMinimo != null && (!Double.isFinite(montoMinimo) || montoMinimo < 0))
            throw new ReglaNegocioException("El monto mínimo debe ser un número no negativo");
        StringBuilder jpql = new StringBuilder(select).append(FROM);
        Map<String,Object> params = new HashMap<>();
        if (desde!=null) {jpql.append(" AND f.fechaEmision >= :desde ");params.put("desde",Date.valueOf(desde));}
        if (hasta!=null) {jpql.append(" AND f.fechaEmision <= :hasta ");params.put("hasta",Date.valueOf(hasta));}
        if (estado!=null && !estado.isBlank()) {
            String normal = estado.trim().toUpperCase(Locale.ROOT);
            if (!Set.of("EMITIDA","ANULADA").contains(normal)) throw new ReglaNegocioException("Estado no válido");
            jpql.append(" AND f.estado = :estado ");params.put("estado",normal);
        }
        if (montoMinimo!=null) {jpql.append(" AND f.importeTotal >= :monto ");params.put("monto",montoMinimo);}
        if (clienteId!=null) {jpql.append(" AND c.id = :clienteId ");params.put("clienteId",clienteId);}
        if (facturaId!=null) {jpql.append(" AND f.id = :id ");params.put("id",facturaId);}
        jpql.append(GROUP).append(" ORDER BY f.fechaEmision DESC, f.id DESC");
        TypedQuery<T> query = em.createQuery(jpql.toString(),tipo);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    @Transactional(readOnly=true)
    public FacturaReporteDTO obtenerReportePorId(Long id) {
        facturas.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Factura "+id+" no encontrada"));
        return consultar("SELECT new com.example.facturaventa.dto.FacturaReporteDTO("+CAMPOS_REPORTE+")",
            FacturaReporteDTO.class,null,null,null,null,id,null).get(0);
    }

    @Transactional(readOnly=true)
    public FacturaEdicionDTO obtenerParaEdicion(Long id) {
        FacturaVenta factura = obtener(id);
        FacturaRequestDTO req = new FacturaRequestDTO();
        req.setFechaEmision(new java.sql.Date(factura.getFechaEmision().getTime()).toLocalDate());
        req.setPuntoVentaId(factura.getPuntoVenta().getId());
        req.setClienteId(factura.getCliente()==null?null:factura.getCliente().getId());
        req.setCondicionIvaId(factura.getCondicionIva().getId());
        req.setTipoMonedaId(factura.getTipoMoneda().getId());
        req.setImporteCobrado(BigDecimal.valueOf(factura.getImporteCobrado()));
        req.setObservaciones(factura.getObservaciones());
        req.setDetalles(factura.getDetalles().stream()
            .map(d->new FacturaDetalleRequestDTO(d.getListaPrecioArticulo().getId(),d.getCantidad(),d.getDescripcion(),d.getPorcentajeBonificacion())).toList());
        return new FacturaEdicionDTO(factura.getId(),factura.getEstado(),req);
    }

    @Transactional
    public FacturaReporteDTO crear(FacturaRequestDTO request) {
        FacturaVenta f = new FacturaVenta();
        mapear(f, request);
        f.setEstado("EMITIDA");
        facturas.saveAndFlush(f); // cascada en detalles, ID asignado por la BD
        f.setNumero(1000L+f.getId()); // número único para este TP, no numeración fiscal AFIP
        em.flush();
        return obtenerReportePorId(f.getId());
    }

    @Transactional
    public FacturaReporteDTO actualizar(Long id, FacturaRequestDTO request) {
        FacturaVenta f = obtener(id);
        if ("ANULADA".equals(f.getEstado())) throw new ReglaNegocioException("Una factura anulada no se puede editar");
        if (f.getCae()!=null && !f.getCae().isBlank()) throw new ReglaNegocioException("No se puede editar una factura autorizada por ARCA");
        mapear(f, request);
        em.flush();
        return obtenerReportePorId(id);
    }

    @Transactional
    public FacturaReporteDTO actualizarEstado(Long id, ActualizarEstadoDTO request) {
        FacturaVenta f=obtener(id);
        String estado=request.getEstado().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("EMITIDA","ANULADA").contains(estado))
            throw new ReglaNegocioException("Estados admitidos: EMITIDA o ANULADA");
        if ("ANULADA".equals(f.getEstado()) && "EMITIDA".equals(estado))
            throw new ReglaNegocioException("No está permitido reactivar una factura anulada");
        if (f.getCae()!=null && !f.getCae().isBlank()) throw new ReglaNegocioException("Una factura con CAE no se anula localmente: corresponde emitir una nota de crédito fiscal");
        f.setEstado(estado);
        if ("ANULADA".equals(estado) && f.getFechaAnulacion()==null) f.setFechaAnulacion(new java.util.Date());
        if (request.getObservaciones()!=null) f.setObservaciones(request.getObservaciones());
        em.flush();
        return obtenerReportePorId(id);
    }

    /** Eliminación física, tal como permite el segundo TP. La anulación lógica es PUT /{id}/estado. */
    @Transactional
    public void eliminar(Long id) {
        FacturaVenta factura=obtener(id);
        if(factura.getCae()!=null && !factura.getCae().isBlank()) throw new ReglaNegocioException("Una factura con CAE no se puede eliminar");
        facturas.delete(factura);
    }

    private FacturaVenta obtener(Long id) {
        return facturas.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Factura "+id+" no encontrada"));
    }

    private void mapear(FacturaVenta f, FacturaRequestDTO req) {
        if (req==null || req.getDetalles()==null || req.getDetalles().isEmpty())
            throw new ReglaNegocioException("Debe indicar al menos un detalle");
        if (req.getFechaEmision()==null) throw new ReglaNegocioException("La fecha de emisión es obligatoria");
        if (req.getImporteCobrado()==null || req.getImporteCobrado().signum()<0)
            throw new ReglaNegocioException("El importe cobrado no puede ser negativo");
        f.setFechaEmision(Date.valueOf(req.getFechaEmision()));
        f.setPuntoVenta(puntosVenta.findById(req.getPuntoVentaId()).orElseThrow(
            ()->new RecursoNoEncontradoException("Punto de venta no encontrado")));
        f.setCondicionIva(condicionesIva.findById(req.getCondicionIvaId()).orElseThrow(
            ()->new RecursoNoEncontradoException("Condición de IVA no encontrada")));
        f.setTipoMoneda(tiposMoneda.findById(req.getTipoMonedaId()).orElseThrow(
            ()->new RecursoNoEncontradoException("Tipo de moneda no encontrado")));
        if(req.getClienteId()==null) throw new ReglaNegocioException("Debe seleccionar un cliente previamente creado");
        f.setCliente(clientes.findById(req.getClienteId()).orElseThrow(
            ()->new RecursoNoEncontradoException("Cliente no encontrado")));
        f.setObservaciones(req.getObservaciones());

        BigDecimal neto=BigDecimal.ZERO, iva=BigDecimal.ZERO;
        // No reemplazar la lista: orphanRemoval necesita conservar la colección administrada por Hibernate.
        f.getDetalles().clear();
        for (FacturaDetalleRequestDTO d : req.getDetalles()) {
            if (d==null || d.getListaPrecioArticuloId()==null || d.getCantidad()==null || d.getCantidad().signum()<=0)
                throw new ReglaNegocioException("Todos los renglones necesitan un precio de artículo y cantidad positiva");
            ListaPrecioArticulo precio=precios.findById(d.getListaPrecioArticuloId()).orElseThrow(
                ()->new RecursoNoEncontradoException("Precio de artículo "+d.getListaPrecioArticuloId()+" no encontrado"));
            Articulo articulo=precio.getArticulo();
            if(!precio.isActivo()||!precio.getListaPrecio().isActivo())throw new ReglaNegocioException("El precio o su lista están inactivos");
            if(!articulo.isActivo())throw new ReglaNegocioException("Artículo inactivo: "+articulo.getDescripcion());
            BigDecimal bonificacion=d.getPorcentajeBonificacion();
            if(bonificacion==null||bonificacion.signum()<0||bonificacion.compareTo(new BigDecimal("100"))>0||bonificacion.stripTrailingZeros().scale()>2)
                throw new ReglaNegocioException("La bonificación debe estar entre 0 y 100%");
            if(d.getDescripcion()!=null&&d.getDescripcion().length()>1200)throw new ReglaNegocioException("La descripción admite hasta 1200 caracteres");
            BigDecimal base=precio.getPrecioNeto().multiply(d.getCantidad()).multiply(BigDecimal.ONE.subtract(bonificacion.movePointLeft(2))).setScale(2,RoundingMode.HALF_UP);
            BigDecimal impuesto=base.multiply(articulo.getAlicuotaIva())
                .divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
            FacturaVentaDetalle detalle=new FacturaVentaDetalle();
            detalle.setListaPrecioArticulo(precio);
            detalle.setCantidad(d.getCantidad());
            detalle.setDescripcion(d.getDescripcion()==null||d.getDescripcion().isBlank()?articulo.getDescripcion():d.getDescripcion().trim());
            detalle.setPorcentajeBonificacion(bonificacion.setScale(2,RoundingMode.HALF_UP));
            detalle.setPrecioUnitarioNeto(precio.getPrecioNeto());
            detalle.setAlicuotaIva(articulo.getAlicuotaIva());
            detalle.setImporteNeto(base);
            detalle.setImporteIva(impuesto);
            detalle.setImporteTotal(base.add(impuesto));
            f.agregarDetalle(detalle);
            neto=neto.add(base);
            iva=iva.add(impuesto);
        }
        BigDecimal total=neto.add(iva);
        BigDecimal cobrado=req.getImporteCobrado().setScale(2,RoundingMode.HALF_UP);
        if (cobrado.compareTo(total)>0) throw new ReglaNegocioException("Lo cobrado no puede superar el total");
        f.setImporteNeto(neto.doubleValue());
        f.setImporteIva(iva.doubleValue());
        f.setImporteTotal(total.doubleValue());
        f.setImporteCobrado(cobrado.doubleValue());
        f.setImporteSaldo(total.subtract(cobrado).doubleValue());
    }
}
