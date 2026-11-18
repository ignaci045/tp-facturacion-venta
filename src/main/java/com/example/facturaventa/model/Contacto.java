package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor
public class Contacto extends EntityId {
 private String email;
 private String telefono;
 private String celular;
}
