package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
@Entity @Table(name="factura_venta") @Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(callSuper=true, onlyExplicitlyIncluded=true)
@ToString(onlyExplicitlyIncluded=true)
public class FacturaVenta extends AuditoriaApp {
    @Column(unique=true) private Long numero;
    @Temporal(TemporalType.DATE) @Column(nullable=false) private Date fechaEmision;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="punto_venta_id",nullable=false) private PuntoVenta puntoVenta;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="cliente_id") private Cliente cliente;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="condicion_iva_id",nullable=false) private CondicionIva condicionIva;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tipo_moneda_id",nullable=false) private TipoMoneda tipoMoneda;
    @Column(nullable=false) private double importeNeto;
    @Column(nullable=false) private double importeIva;
    @Column(nullable=false) private double importeCobrado;
    @Column(nullable=false) private double importeSaldo;
    @Column(nullable=false) private double importeTotal;
    // Numeración fiscal independiente del número interno; solo se asigna al obtener CAE.
    private Integer puntoVentaFiscal;
    private Integer tipoComprobanteFiscal;
    private Long numeroFiscal;
    private String cae;
    @Temporal(TemporalType.DATE) private Date caeFechaVencimiento;
    private String resultadoAfip;
    private String motivoRechazo;
    @Column(nullable=false) private String estado="EMITIDA";
    @Temporal(TemporalType.TIMESTAMP) private Date fechaAnulacion;
    @Column(length=1200) private String observaciones;
    @OneToMany(mappedBy="factura",cascade=CascadeType.ALL,orphanRemoval=true)
    private List<FacturaVentaDetalle> detalles = new ArrayList<>();
    public void agregarDetalle(FacturaVentaDetalle detalle) {detalle.setFactura(this); detalles.add(detalle);}
}
