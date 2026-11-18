package com.example.facturaventa.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor @RequiredArgsConstructor
public class Rubro extends AuditoriaApp {
 @NonNull @Column(nullable=false,unique=true) private String denominacion;
}
