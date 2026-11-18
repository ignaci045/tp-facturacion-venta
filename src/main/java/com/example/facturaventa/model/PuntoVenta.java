package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="punto_venta") @Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(callSuper=true, onlyExplicitlyIncluded=true)
public class PuntoVenta extends AuditoriaApp {
    @Column(nullable=false) private String descripcion;

}
