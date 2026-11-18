package com.example.facturaventa.exception;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;

@RestControllerAdvice
public class ManejadorErrores {
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String,String>> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error",ex.getMessage()));
    }
    @ExceptionHandler({ReglaNegocioException.class, ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String,String>> datosIncorrectos(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error",ex.getMessage()==null?"Petición inválida":ex.getMessage()));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,String>> duplicado(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","El documento ya está registrado o los datos entran en conflicto"));
    }
    @ExceptionHandler({java.io.IOException.class,java.security.GeneralSecurityException.class})
    public ResponseEntity<Map<String,String>> conexionArca(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error","No se pudo completar la conexión o leer las credenciales ARCA: "+(ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage())));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,String>> estadoInvalido(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()==null?"Operación fiscal no completada":ex.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> validacion(MethodArgumentNotValidException ex) {
        String mensaje=ex.getBindingResult().getFieldErrors().stream()
            .findFirst().map(e->e.getField()+": "+e.getDefaultMessage()).orElse("Datos inválidos");
        return ResponseEntity.badRequest().body(Map.of("error",mensaje));
    }
}
