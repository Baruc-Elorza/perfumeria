package com.example.demo.controller;

import com.example.demo.model.Perfume;
import com.example.demo.repository.PerfumeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perfumes")
@CrossOrigin(origins = "http://localhost:4200")
public class PerfumeController {

    @Autowired
    private PerfumeRepository perfumeRepository;

    // GET: Obtener lista completa de perfumes
    @GetMapping
    public List<Perfume> listar() {
        return perfumeRepository.findAll();
    }

    // POST: Registrar un nuevo perfume
    @PostMapping
    public ResponseEntity<Perfume> agregar(@RequestBody Perfume perfume) {
        Perfume nuevo = perfumeRepository.save(perfume);
        return ResponseEntity.ok(nuevo);
    }

    // DELETE: Eliminar un perfume por su ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (perfumeRepository.existsById(id)) {
            perfumeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}