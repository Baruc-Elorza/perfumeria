package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    ResponseEntity<Map<String, String>> cuentaBloqueada() {
        return ResponseEntity.status(401).body(Map.of("message", "Tu cuenta está bloqueada. Contacta al administrador."));
    }
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    ResponseEntity<Map<String, String>> autenticacionFallida() {
        return ResponseEntity.status(401).body(Map.of("message", "No se pudo iniciar sesión con esas credenciales."));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> conflicto() {
        return ResponseEntity.status(409).body(Map.of("message",
                "No se pudo guardar el cambio porque el producto tiene datos relacionados o inválidos. Actualiza la lista e inténtalo de nuevo."));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> responder(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode())
                .body(Map.of("message", error.getReason() == null ? "No se pudo completar la solicitud." : error.getReason()));
    }
}
