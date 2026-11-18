package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor
public class Domicilio extends EntityId {
 private String nombreCalle;
 private String numeroCalle;
 private String localidad;
 private String provincia;
 private String codigoPostal;
}
