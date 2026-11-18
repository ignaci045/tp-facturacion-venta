package com.example.facturaventa.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@MappedSuperclass
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public abstract class AuditoriaApp extends EntityId {
    @Column(nullable=false, updatable=false)
    private Instant creadoEn;
    @Column(nullable=false)
    private Instant modificadoEn;

    @PrePersist void alCrear() { creadoEn=Instant.now(); modificadoEn=creadoEn; }
    @PreUpdate void alActualizar() { modificadoEn=Instant.now(); }
}
