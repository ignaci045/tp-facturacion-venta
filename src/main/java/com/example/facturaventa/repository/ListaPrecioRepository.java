package com.example.facturaventa.repository;
import com.example.facturaventa.model.ListaPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ListaPrecioRepository extends JpaRepository<ListaPrecio,Long> {Optional<ListaPrecio> findByDenominacion(String denominacion);}
