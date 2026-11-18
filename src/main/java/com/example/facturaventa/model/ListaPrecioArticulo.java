package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="lista_precio_articulo") @Getter @Setter @NoArgsConstructor @RequiredArgsConstructor
@EqualsAndHashCode(callSuper=true, onlyExplicitlyIncluded=true)
public class ListaPrecioArticulo extends AuditoriaApp {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(nullable=false)
    @NonNull private Articulo articulo;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="lista_precio_id",nullable=false)
    @NonNull private ListaPrecio listaPrecio;
    @Column(nullable=false,precision=15,scale=2) @NonNull private BigDecimal precioNeto;
    @Column(nullable=false) private boolean activo=true;

}
