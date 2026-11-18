package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="lista_precio") @Getter @Setter @NoArgsConstructor @RequiredArgsConstructor
@EqualsAndHashCode(callSuper=true,onlyExplicitlyIncluded=true)
public class ListaPrecio extends AuditoriaApp {
 @NonNull @Column(nullable=false,unique=true) private String denominacion;
 @Column(nullable=false) private boolean activo=true;
}
