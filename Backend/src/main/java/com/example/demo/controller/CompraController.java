package com.example.demo.controller;

import com.example.demo.service.CompraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/compras")
@CrossOrigin(origins = "http://localhost:4200") // Permite peticiones desde Angular
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmarCompra(@RequestBody ConfirmacionPagoRequest request) {
        try {
            compraService.confirmarCompraYPago(
                request.getPedidoId(), 
                request.getReferenciaPagoProveedor()
            );
            return ResponseEntity.ok(Map.of("mensaje", "Compra y pago confirmados correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al procesar la compra"));
        }
    }

    // DTO estático para recibir los datos desde Angular
    public static class ConfirmacionPagoRequest {
        private String pedidoId;
        private String referenciaPagoProveedor;

        public String getPedidoId() {
            return pedidoId;
        }

        public void setPedidoId(String pedidoId) {
            this.pedidoId = pedidoId;
        }

        public String getReferenciaPagoProveedor() {
            return referenciaPagoProveedor;
        }

        public void setReferenciaPagoProveedor(String referenciaPagoProveedor) {
            this.referenciaPagoProveedor = referenciaPagoProveedor;
        }
    }
}