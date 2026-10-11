package com.example.demo.controller;

import com.example.demo.model.Pedido;
import com.example.demo.repository.PedidoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class PedidoController {

    private final PedidoRepository pedidoRepository;

    public PedidoController(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @GetMapping
    public List<Pedido> obtenerHistorial(Authentication authentication) {
        return pedidoRepository
                .findByClienteEmailOrderByFechaCreacionDesc(authentication.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtenerDetalle(
            @PathVariable String id,
            Authentication authentication) {

        return pedidoRepository
                .findByIdAndClienteEmail(id, authentication.getName())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}