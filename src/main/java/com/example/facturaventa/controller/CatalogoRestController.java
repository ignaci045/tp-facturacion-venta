package com.example.facturaventa.controller;
import com.example.facturaventa.dto.CatalogosDTO;
import com.example.facturaventa.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/catalogos") @RequiredArgsConstructor
@CrossOrigin(origins={"http://localhost:5173","http://127.0.0.1:5173"})
public class CatalogoRestController {
    private final CatalogoService servicio;
    @GetMapping public CatalogosDTO listar(){return servicio.obtener();}
}
