package com.example.facturaventa.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter @Setter @NoArgsConstructor
public abstract class EntityId {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identidad persistente: dos entidades nuevas no son iguales aunque id sea null.
    // Se mantiene manualmente por la semántica especial de JPA; Lombok genera los accesores.
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EntityId e)) return false;
        return getId() != null && getId().equals(e.getId())
            && org.hibernate.Hibernate.getClass(this).equals(org.hibernate.Hibernate.getClass(other));
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
