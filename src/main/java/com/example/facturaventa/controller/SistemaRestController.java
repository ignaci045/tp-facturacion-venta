package com.example.facturaventa.controller;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/sistema")
public class SistemaRestController {
 private final ApplicationAvailability availability;
 public SistemaRestController(ApplicationAvailability availability){this.availability=availability;}
 public record Estado(String version,boolean listo) {}
 @GetMapping("/estado") public Estado estado(){return new Estado("2026.11.13-arranque1",availability.getReadinessState()==ReadinessState.ACCEPTING_TRAFFIC);}
}
