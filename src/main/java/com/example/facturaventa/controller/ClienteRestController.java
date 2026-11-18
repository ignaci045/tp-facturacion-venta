package com.example.facturaventa.controller;
import com.example.facturaventa.model.*;
import com.example.facturaventa.repository.ClienteRepository;
import com.example.facturaventa.service.FacturaService;
import com.example.facturaventa.dto.FacturaGestionDTO;
import com.example.facturaventa.exception.ReglaNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/clientes") @RequiredArgsConstructor
public class ClienteRestController {
    private final ClienteRepository clientes;
    private final FacturaService facturas;
    public record Alta(@NotBlank @Size(max=255) String denominacion,
        @NotBlank @Pattern(regexp="[0-9 .-]{7,20}",message="Documento inválido") String documento,
        @Pattern(regexp="MINORISTA|MAYORISTA",message="Tipo de cliente inválido") String tipoCliente,
        @Email @Size(max=255) String email,@Size(max=60) String telefono,@Size(max=60) String celular,
        @Size(max=255) String nombreCalle,@Size(max=30) String numeroCalle,@Size(max=255) String localidad,
        @Size(max=255) String provincia,@Size(max=20) String codigoPostal) {
        public Alta(String nombre,String documento){this(nombre,documento,"MINORISTA",null,null,null,null,null,null,null,null);}
    }
    @GetMapping public Page<Cliente> buscar(@RequestParam(defaultValue="") String q,
        @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) {
        if(page<0 || size<1 || size>100) throw new ReglaNegocioException("Paginación inválida");
        return clientes.findByDenominacionContainingIgnoreCaseOrDocumentoContainingIgnoreCase(q.trim(),q.trim(),
            PageRequest.of(page,size,Sort.by("denominacion").ascending().and(Sort.by("id"))));
    }
    @PostMapping @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Cliente> crear(@Valid @RequestBody Alta alta) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guardar(new Cliente(),alta));
    }
    @PutMapping("/{id}") @org.springframework.transaction.annotation.Transactional
    public Cliente actualizar(@PathVariable Long id,@Valid @RequestBody Alta alta) {
        return guardar(clientes.findById(id).orElseThrow(()->new com.example.facturaventa.exception.RecursoNoEncontradoException("Cliente no encontrado")),alta);
    }
    @GetMapping("/{id}") public Cliente obtener(@PathVariable Long id){return clientes.findById(id).orElseThrow(()->new com.example.facturaventa.exception.RecursoNoEncontradoException("Cliente no encontrado"));}
    private Cliente guardar(Cliente cliente,Alta alta) {
        String documento=alta.documento().replaceAll("[^0-9]", "");
        if(documento.length()<7 || documento.length()>11) throw new ReglaNegocioException("Indicá un DNI o CUIT válido");
        if(!documento.equals(cliente.getDocumento())&&clientes.existsByDocumento(documento)) throw new ReglaNegocioException("Ya existe un cliente con ese documento");
        cliente.setDenominacion(alta.denominacion().trim());cliente.setDocumento(documento);
        cliente.setTipoCliente(alta.tipoCliente()==null?"MINORISTA":alta.tipoCliente());
        Contacto c=cliente.getContacto()==null?new Contacto():cliente.getContacto();
        c.setEmail(texto(alta.email()));c.setTelefono(texto(alta.telefono()));c.setCelular(texto(alta.celular()));cliente.setContacto(c);
        Domicilio d=cliente.getDomicilio()==null?new Domicilio():cliente.getDomicilio();
        d.setNombreCalle(texto(alta.nombreCalle()));d.setNumeroCalle(texto(alta.numeroCalle()));d.setLocalidad(texto(alta.localidad()));d.setProvincia(texto(alta.provincia()));d.setCodigoPostal(texto(alta.codigoPostal()));cliente.setDomicilio(d);
        return clientes.save(cliente);
    }
    private String texto(String s){return s==null?null:s.trim();}
    @GetMapping("/{id}/facturas") public List<FacturaGestionDTO> historial(@PathVariable Long id) {
        return facturas.buscarPorCliente(id);
    }
}
