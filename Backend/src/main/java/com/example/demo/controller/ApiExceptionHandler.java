package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> datosInvalidos(org.springframework.web.bind.MethodArgumentNotValidException error) {
        return ResponseEntity.badRequest().body(Map.of("message", "Revisa los campos obligatorios y su formato."));
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> formatoInvalido() {
        return ResponseEntity.badRequest().body(Map.of("message", "La solicitud contiene datos con formato inválido."));
    }
    @ExceptionHandler(com.stripe.exception.StripeException.class)
    ResponseEntity<Map<String, String>> falloPasarela() {
        return ResponseEntity.status(502).body(Map.of("message", "No se pudo verificar la operación con Stripe. Actualiza el pedido antes de intentarlo de nuevo."));
    }
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
