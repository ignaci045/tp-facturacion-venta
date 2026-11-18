package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name="articulo") @Getter @Setter @NoArgsConstructor @RequiredArgsConstructor
@EqualsAndHashCode(callSuper=true, onlyExplicitlyIncluded=true)
public class Articulo extends AuditoriaApp {
    @Column(unique=true) private String codigo;
    @ManyToOne private Rubro rubro;
    @ManyToOne private Marca marca;
    @Column(nullable=false) @NonNull private String descripcion;
    @Column(nullable=false,precision=5,scale=2) @NonNull private BigDecimal alicuotaIva;
    @Column(nullable=false) private boolean activo=true;

}
