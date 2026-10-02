package com.example.demo.controller;

import com.example.demo.model.Perfume;
import com.example.demo.service.PerfumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perfumes")
@CrossOrigin(origins = "http://localhost:4200")
public class PerfumeController {

    private final PerfumeService perfumeService;

    public PerfumeController(PerfumeService perfumeService) {
        this.perfumeService = perfumeService;
    }

    // GET: Obtener el catálogo completo o buscar perfumes por nombre (H.U.-01 y H.U.-03)
    @GetMapping
    public List<Perfume> listar(@RequestParam(required = false) String nombre) {
        return perfumeService.listar(nombre);
    }

    // GET: Obtener el detalle de un perfume por ID (H.U.-09)
    @GetMapping("/{id}")
    public ResponseEntity<Perfume> obtenerDetalle(@PathVariable Long id) {
        return perfumeService.obtenerPorId(id)
                .map(perfume -> ResponseEntity.ok(perfume))
                .orElse(ResponseEntity.notFound().build());
    }

    // POST: Registrar un nuevo perfume
    @PostMapping
    public ResponseEntity<Perfume> agregar(@RequestBody Perfume perfume) {
        Perfume nuevo = perfumeService.guardar(perfume);
        return ResponseEntity.ok(nuevo);
    }

    // PUT: Actualizar un perfume existente
    @PutMapping("/{id}")
    public ResponseEntity<Perfume> actualizar(@PathVariable Long id, @RequestBody Perfume perfume) {
        return ResponseEntity.ok(perfumeService.actualizar(id, perfume));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        perfumeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
