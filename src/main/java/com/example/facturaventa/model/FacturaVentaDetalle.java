package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="factura_venta_detalle") @Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(callSuper=true,onlyExplicitlyIncluded=true)
@ToString(onlyExplicitlyIncluded=true)
public class FacturaVentaDetalle extends AuditoriaApp {
    @Column(length=1200) private String descripcion;
    @Column(precision=5,scale=2,nullable=false) private BigDecimal porcentajeBonificacion=BigDecimal.ZERO;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="factura_id",nullable=false) private FacturaVenta factura;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="lista_precio_articulo_id",nullable=false) private ListaPrecioArticulo listaPrecioArticulo;
    @Transient public Articulo getArticulo(){return listaPrecioArticulo==null?null:listaPrecioArticulo.getArticulo();}
    @Column(nullable=false,precision=14,scale=3) private BigDecimal cantidad;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal precioUnitarioNeto;
    @Column(nullable=false,precision=5,scale=2) private BigDecimal alicuotaIva;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal importeNeto;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal importeIva;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal importeTotal;
}
