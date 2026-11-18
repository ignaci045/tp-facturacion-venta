package com.example.facturaventa.controller;
import com.example.facturaventa.model.*;
import com.example.facturaventa.repository.*;
import com.example.facturaventa.exception.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/articulos") @RequiredArgsConstructor
public class ArticuloRestController {
 private final ArticuloRepository articulos;
 private final RubroRepository rubros;
 private final MarcaRepository marcas;
 public record Datos(@NotBlank @Size(max=255) String codigo,@NotBlank @Size(max=255) String descripcion,
  @NotBlank @Size(max=255) String rubro,@NotBlank @Size(max=255) String marca){}
 @PutMapping("/{id}") @org.springframework.transaction.annotation.Transactional
 public void actualizar(@PathVariable Long id,@Valid @RequestBody Datos d){
  Articulo a=articulos.findById(id).orElseThrow(()->new RecursoNoEncontradoException("Artículo no encontrado"));
  String codigo=d.codigo().trim();
  if(articulos.findAll().stream().anyMatch(x->!x.getId().equals(id)&&codigo.equalsIgnoreCase(x.getCodigo())))throw new ReglaNegocioException("El código ya pertenece a otro artículo");
  a.setCodigo(codigo);a.setDescripcion(d.descripcion().trim());
  String rubro=d.rubro().trim(),marca=d.marca().trim();
  a.setRubro(rubros.findAll().stream().filter(x->rubro.equalsIgnoreCase(x.getDenominacion())).findFirst().orElseGet(()->rubros.save(new Rubro(rubro))));
  a.setMarca(marcas.findAll().stream().filter(x->marca.equalsIgnoreCase(x.getDenominacion())).findFirst().orElseGet(()->marcas.save(new Marca(marca))));
 }
}
