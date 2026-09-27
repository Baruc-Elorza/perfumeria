package com.example.demo.controller;

import com.example.demo.service.PagoService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.beans.factory.annotation.Autowired;
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

    response.put("estado", estado);

    return ResponseEntity.ok(response);
}
}