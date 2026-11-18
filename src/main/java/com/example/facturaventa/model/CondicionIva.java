package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="condicion_iva") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper=true, onlyExplicitlyIncluded=true)
public class CondicionIva extends AuditoriaApp {
    @Column(nullable=false) private String denominacion;

}
