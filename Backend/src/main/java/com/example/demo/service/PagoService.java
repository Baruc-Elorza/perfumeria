package com.example.demo.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

@Service
public class PagoService {

    public PaymentIntent crearPago(Long monto) throws StripeException {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(monto * 100)
                        .setCurrency("mxn")
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .build();

        return PaymentIntent.create(params);
    }

    public PaymentIntent obtenerPago(String paymentIntentId) throws StripeException {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("El identificador del pago es obligatorio.");
        }

        return PaymentIntent.retrieve(paymentIntentId);
    }

    public String verificarPago(String paymentIntentId) throws StripeException {
        return obtenerPago(paymentIntentId).getStatus();
    }
}