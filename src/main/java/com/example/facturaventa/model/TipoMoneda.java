package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="tipo_moneda") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper=true, onlyExplicitlyIncluded=true)
public class TipoMoneda extends AuditoriaApp {
    @Column(nullable=false, unique=true) private String codigo;
    @Column(nullable=false) private String descripcion;

}
