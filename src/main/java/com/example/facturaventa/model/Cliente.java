package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="cliente") @Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(callSuper=true, onlyExplicitlyIncluded=true)
public class Cliente extends AuditoriaApp {
    private String tipoCliente="MINORISTA";
    @OneToOne(cascade=CascadeType.ALL) private Contacto contacto;
    @OneToOne(cascade=CascadeType.ALL) private Domicilio domicilio;
    public Cliente(String denominacion,String documento){this.denominacion=denominacion;this.documento=documento;}
    @Column(nullable=false) private String denominacion;
    @Column(unique=true) private String documento;

}
