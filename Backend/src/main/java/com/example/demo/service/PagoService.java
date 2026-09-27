package com.example.demo.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;

@Service
public class PagoService {

    public PaymentIntent crearPago(Long monto) throws StripeException {

        PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                        .setAmount(monto)
                        .setCurrency("mxn")
                        .build();

        return PaymentIntent.create(params);
    }
     public String verificarPago(String paymentIntentId)
            throws StripeException {

        PaymentIntent intent =
                PaymentIntent.retrieve(paymentIntentId);

        return intent.getStatus();
    }

}

