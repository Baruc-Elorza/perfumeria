package com.example.demo.controller;

import com.example.demo.model.Direccion;
import com.example.demo.service.DireccionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/direcciones")
@CrossOrigin(origins = "*")
public class DireccionController {

    @Autowired
    private DireccionService direccionService;

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Direccion>> listarPorUsuario(@PathVariable Long usuarioId) {
        List<Direccion> direcciones = direccionService.obtenerDireccionesPorUsuario(usuarioId);
        return ResponseEntity.ok(direcciones);
    }

    @PostMapping
    public ResponseEntity<Direccion> guardar(@Valid @RequestBody Direccion direccion) {
        Direccion nuevaDireccion = direccionService.guardarDireccion(direccion);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaDireccion);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        direccionService.eliminarDireccion(id);
        return ResponseEntity.noContent().build();
    }
}