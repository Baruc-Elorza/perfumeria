package com.example.demo.service;

import com.example.demo.model.Pedido;
import com.example.demo.repository.PedidoRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GestionPedidoService {
    private final PedidoRepository pedidos;
    private final PagoService pagos;
    private final CompraService compras;

    public GestionPedidoService(PedidoRepository pedidos, PagoService pagos, CompraService compras) {
        this.pedidos = pedidos;
        this.pagos = pagos;
        this.compras = compras;
    }

    private Pedido pendiente(Long id, String email) {
        Pedido pedido = pedidos.bloquearPorIdYClienteEmail(id, email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el pedido en tu cuenta."));
        if (pedido.getEstado() != Pedido.EstadoPedido.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya no está pendiente de pago.");
        }
        return pedido;
    }

    @Transactional(rollbackFor = StripeException.class)
    public InicioPago iniciarPago(Long id, String email) throws StripeException {
        Pedido pedido = pendiente(id, email);
        PaymentIntent intent;
        if (pedido.getPaymentIntentId() == null) {
            compras.validarExistencias(pedido);
            intent = pagos.crearPago(pedido.getTotal(), id.toString());
            pedido.setPaymentIntentId(intent.getId());
        } else {
            intent = pagos.obtenerPago(pedido.getPaymentIntentId());
            if (!"succeeded".equals(intent.getStatus()) && !"canceled".equals(intent.getStatus())) {
                compras.validarExistencias(pedido);
            }
        }
        if ("canceled".equals(intent.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El intento de pago está cancelado. Cancela este pedido y crea uno nuevo.");
        }
        boolean confirmado = "succeeded".equals(intent.getStatus());
        if (confirmado) compras.confirmarCompraYPago(id, intent.getId(), email);
        return new InicioPago(confirmado ? null : intent.getClientSecret(), intent.getId(), confirmado);
    }

    @Transactional(rollbackFor = StripeException.class)
    public void cancelar(Long id, String email) throws StripeException {
        Pedido pedido = pendiente(id, email);
        if (pedido.getPaymentIntentId() != null) {
            PaymentIntent intent = pagos.obtenerPago(pedido.getPaymentIntentId());
            if ("succeeded".equals(intent.getStatus()) || "processing".equals(intent.getStatus())
                    || "requires_capture".equals(intent.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El pago ya fue aprobado o está en proceso. Revisa su estado antes de cancelar.");
            }
            if (!"canceled".equals(intent.getStatus())) pagos.cancelarPago(intent.getId());
        }
        pedido.setEstado(Pedido.EstadoPedido.CANCELADO);
    }

    public record InicioPago(String clientSecret, String paymentIntentId, boolean pagoConfirmado) {}
}
