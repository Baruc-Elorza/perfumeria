package com.example.demo.controller;

import com.example.demo.model.Pedido;
import com.example.demo.repository.PedidoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;

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
    
    @PatchMapping("/{id}/confirmar-entrega")
    public ResponseEntity<?> confirmarEntrega(
            @PathVariable String id,
            Authentication authentication) {

        Pedido pedido = pedidoRepository
                .findByIdAndClienteEmail(id, authentication.getName())
                .orElse(null);

        if (pedido == null) {
            return ResponseEntity.notFound().build();
        }

        if (pedido.getEstado() != Pedido.EstadoPedido.ENVIADO) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                        "message",
                        "Solo puedes confirmar la recepción de un pedido enviado."
                ));
        }

        pedido.setEstado(Pedido.EstadoPedido.ENTREGADO);

        return ResponseEntity.ok(pedidoRepository.save(pedido));
    }

    @GetMapping("/admin")
    public List<Pedido> obtenerPedidosParaEnvio() {
        return pedidoRepository.findByEstadoInOrderByFechaCreacionDesc(List.of(
                Pedido.EstadoPedido.PAGADO,
                Pedido.EstadoPedido.ENVIADO));
    }

    @PatchMapping("/{id}/guia")
    public ResponseEntity<?> guardarGuia(
            @PathVariable String id,
            @Valid @RequestBody GuiaRequest request) {
        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();
        if (pedido.getEstado() != Pedido.EstadoPedido.PAGADO
                && pedido.getEstado() != Pedido.EstadoPedido.ENVIADO) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Solo se puede registrar una guía para pedidos pagados."));
        }

        pedido.setNumeroGuia(request.numeroGuia().trim());
        pedido.setEstado(Pedido.EstadoPedido.ENVIADO);
        return ResponseEntity.ok(pedidoRepository.save(pedido));
    }

    public record GuiaRequest(@NotBlank @Size(max = 100) String numeroGuia) {}
}
