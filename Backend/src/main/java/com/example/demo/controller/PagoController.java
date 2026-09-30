package com.example.demo.controller;

import com.example.demo.service.PagoService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "http://localhost:4200")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @Value("${stripe.public.key}")
    private String publicKey;

    @PostMapping("/crear")
    public ResponseEntity<?> crearPago(@RequestParam Long monto)
            throws StripeException {

        PaymentIntent intent =
                pagoService.crearPago(monto);

        Map<String, String> response =
                new HashMap<>();

        response.put(
                "clientSecret",
                intent.getClientSecret()
        );

        response.put(
                "paymentIntentId",
                intent.getId()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/estado/{id}")
    public ResponseEntity<?> estadoPago(
            @PathVariable String id)
            throws StripeException {

        String estado =
                pagoService.verificarPago(id);

        Map<String, String> response =
                new HashMap<>();

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
            response.put(
                    "mensaje",
                    "Pago en proceso"
            );
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    public ResponseEntity<?> obtenerConfiguracion() {

        Map<String, String> response =
                new HashMap<>();

        response.put("publicKey", publicKey);

        return ResponseEntity.ok(response);
    }
}