package com.example.demo.controller;

import com.example.demo.model.Direccion;
import com.example.demo.service.DireccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigInteger;
import java.util.List;

@RestController
@RequestMapping("/api/direcciones")
public class DireccionController {
    private final DireccionService direcciones;

    public DireccionController(DireccionService direcciones) {
        this.direcciones = direcciones;
    }

    @GetMapping
    public List<Direccion> listar(Authentication sesion) {
        return direcciones.listar(sesion.getName());
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Direccion> listarPorUsuario(@PathVariable BigInteger usuarioId, Authentication sesion) {
        return direcciones.listarPorUsuario(usuarioId, sesion.getName());
    }

    @PostMapping
    public ResponseEntity<Direccion> guardar(@Valid @RequestBody Direccion direccion, Authentication sesion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(direcciones.crear(direccion, sesion.getName()));
    }

    @PutMapping("/{id}")
    public Direccion actualizar(@PathVariable BigInteger id, @Valid @RequestBody Direccion direccion,
                               Authentication sesion) {
        return direcciones.actualizar(id, direccion, sesion.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable BigInteger id, Authentication sesion) {
        direcciones.eliminar(id, sesion.getName());
        return ResponseEntity.noContent().build();
    }
}
