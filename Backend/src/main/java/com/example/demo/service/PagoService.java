
package com.example.demo.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PagoService {

    public PaymentIntent crearPago(BigDecimal monto, String pedidoId)
            throws StripeException {

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor que cero."
            );
        }

        if (pedidoId == null || pedidoId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del pedido es obligatorio."
            );
        }

        // Stripe recibe el importe en centavos para MXN.
        long montoEnCentavos = monto
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(montoEnCentavos)
                        .setCurrency("mxn")
                        .putMetadata("pedidoId", pedidoId)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams
                                        .AutomaticPaymentMethods
                                        .builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .build();

        return PaymentIntent.create(params);
    }

    public PaymentIntent obtenerPago(String paymentIntentId)
            throws StripeException {

        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del pago es obligatorio."
            );
        }

        return PaymentIntent.retrieve(paymentIntentId);
    }

    public String verificarPago(String paymentIntentId)
            throws StripeException {
        return obtenerPago(paymentIntentId).getStatus();
    }
}
