package com.example.facturaventa.repository;
import com.example.facturaventa.model.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ArticuloRepository extends JpaRepository<Articulo, Long> {}
