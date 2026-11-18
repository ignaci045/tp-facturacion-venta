package com.example.facturaventa.arca;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/arca")
public class ArcaController {
    private final ArcaService arca;
    public ArcaController(ArcaService arca){this.arca=arca;}
    @GetMapping("/estado") public ArcaService.Estado estado(){return arca.estado();}
    @GetMapping("/diagnostico") public ArcaService.Diagnostico diagnostico() throws Exception {return arca.diagnosticar();}
    @PostMapping("/facturas/{id}/emitir") public ArcaService.Resultado emitir(
        @PathVariable Long id,@Valid @RequestBody ArcaSolicitudDTO datos) throws Exception {return arca.emitir(id,datos);}
}
