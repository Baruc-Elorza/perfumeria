
package com.example.demo.controller;

import com.example.demo.model.Pedido;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.service.PagoService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(
        origins = "http://localhost:4200",
        allowCredentials = "true"
)
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Value("${stripe.public.key}")
    private String publicKey;

    @PostMapping("/crear")
    public ResponseEntity<?> crearPago(
            @RequestParam String pedidoId,
            Authentication authentication
    ) throws StripeException {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Debes iniciar sesión."));
        }

        Optional<Pedido> pedidoEncontrado =
                pedidoRepository.findByIdAndClienteEmail(
                        pedidoId,
                        authentication.getName()
                );

        if (pedidoEncontrado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "mensaje",
                            "No se encontró el pedido para este usuario."
                    ));
        }

        Pedido pedido = pedidoEncontrado.get();

        if (pedido.getEstado() != Pedido.EstadoPedido.PENDIENTE) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "mensaje",
                            "El pedido no está pendiente de pago."
                    ));
        }

        PaymentIntent intent = pagoService.crearPago(
                pedido.getTotal(),
                pedido.getId()
        );

        Map<String, String> response = new HashMap<>();
        response.put("clientSecret", intent.getClientSecret());
        response.put("paymentIntentId", intent.getId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/estado/{id}")
    public ResponseEntity<?> estadoPago(
            @PathVariable String id
    ) throws StripeException {

        String estado = pagoService.verificarPago(id);

        Map<String, String> response = new HashMap<>();

        if ("succeeded".equals(estado)) {
            response.put("estado", "PAGADO");
            response.put("mensaje", "Pago aprobado");
        } else if ("requires_payment_method".equals(estado)) {
            response.put("estado", "RECHAZADO");
            response.put(
                    "mensaje",
                    "Tarjeta rechazada. Intente nuevamente."
            );
        } else {
            response.put("estado", estado);
            response.put("mensaje", "Pago en proceso");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    public ResponseEntity<?> obtenerConfiguracion() {
        Map<String, String> response = new HashMap<>();
        response.put("publicKey", publicKey);
        return ResponseEntity.ok(response);
    }
}
