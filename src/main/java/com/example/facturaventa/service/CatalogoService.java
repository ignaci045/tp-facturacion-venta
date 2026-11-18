package com.example.facturaventa.service;
import com.example.facturaventa.dto.CatalogosDTO;
import com.example.facturaventa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class CatalogoService {
    private final ClienteRepository clientes;
    private final PuntoVentaRepository puntosVenta;
    private final CondicionIvaRepository condicionesIva;
    private final TipoMonedaRepository tiposMoneda;
    private final ListaPrecioArticuloRepository precios;
    private final ListaPrecioRepository listas;
    @Transactional(readOnly=true)
    public CatalogosDTO obtener() {
        var disponibles=precios.findAllByActivoTrueOrderByIdDesc().stream()
            .filter(p->p.getArticulo().isActivo()&&p.getListaPrecio().isActivo())
            .sorted(Comparator.comparing((com.example.facturaventa.model.ListaPrecioArticulo p)->p.getArticulo().getDescripcion()).thenComparing(p->p.getListaPrecio().getDenominacion()).thenComparing(com.example.facturaventa.model.ListaPrecioArticulo::getId))
            .map(p->new CatalogosDTO.PrecioArticuloOpcionDTO(p.getId(),p.getArticulo().getId(),p.getArticulo().getDescripcion(),p.getListaPrecio().getId(),p.getListaPrecio().getDenominacion(),p.getPrecioNeto(),p.getArticulo().getAlicuotaIva(),p.getArticulo().getCodigo(),p.getArticulo().getRubro()==null?null:p.getArticulo().getRubro().getDenominacion(),p.getArticulo().getMarca()==null?null:p.getArticulo().getMarca().getDenominacion())).toList();
        return new CatalogosDTO(
            clientes.findAll().stream().map(c->new CatalogosDTO.OpcionDTO(c.getId(),c.getDenominacion())).toList(),
            puntosVenta.findAll().stream().map(p->new CatalogosDTO.OpcionDTO(p.getId(),p.getDescripcion())).toList(),
            condicionesIva.findAll().stream().map(c->new CatalogosDTO.OpcionDTO(c.getId(),c.getDenominacion())).toList(),
            tiposMoneda.findAll().stream().map(t->new CatalogosDTO.OpcionDTO(t.getId(),t.getCodigo()+" - "+t.getDescripcion())).toList(),
            listas.findAll().stream().filter(com.example.facturaventa.model.ListaPrecio::isActivo).map(l->new CatalogosDTO.OpcionDTO(l.getId(),l.getDenominacion())).toList(),
            disponibles);
    }
}
