package com.example.facturaventa.repository;
import com.example.facturaventa.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    org.springframework.data.domain.Page<Cliente> findByDenominacionContainingIgnoreCaseOrDocumentoContainingIgnoreCase(
        String nombre, String documento, org.springframework.data.domain.Pageable pageable);
    boolean existsByDocumento(String documento);
}
