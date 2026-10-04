package com.example.demo.notificaciones;

import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class NotificacionCompraService {
    private final NotificacionCompraRepository repository;
    private final Validator validator;

    public NotificacionCompraService(NotificacionCompraRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    /**
     * HU-05/HU-16 deben llamarlo en la misma transacción que confirma el pedido,
     * después de verificar el pago con el proveedor y bloquear el pedido.
     * No envía correo aquí: si hay rollback, también se revierte el pendiente.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(CompraParaNotificar compra) {
        if (compra == null || !validator.validate(compra).isEmpty()) {
            throw new IllegalArgumentException("La notificación necesita los datos completos y válidos del pedido.");
        }
        if (compra.estadoPago() != CompraParaNotificar.EstadoPago.EXITOSO) {
            throw new IllegalArgumentException("Solo se notifican pedidos con pago exitoso verificado.");
        }
        BigDecimal suma = compra.productos().stream()
                .map(p -> p.precioUnitario().multiply(BigDecimal.valueOf(p.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.compareTo(compra.subtotal()) != 0
                || compra.subtotal().add(compra.envio()).subtract(compra.descuento()).compareTo(compra.total()) != 0) {
            throw new IllegalArgumentException("Los importes del pedido no coinciden.");
        }
        var existente = repository.findByPedidoId(compra.pedidoId());
        if (existente.isPresent()) {
            if (!existente.get().referenciaPago.equals(compra.referenciaPago())) {
                throw new IllegalArgumentException("El pedido ya tiene otra referencia de pago notificada.");
            }
            return;
        }
        repository.save(new NotificacionCompra(compra.pedidoId(), compra.referenciaPago(),
                "Compra confirmada - Pedido " + compra.pedidoId(), redactar(compra)));
    }

    private String redactar(CompraParaNotificar c) {
        StringBuilder texto = new StringBuilder("Se confirmó el pago del siguiente pedido.\n\n");
        texto.append("Pedido: ").append(c.pedidoId())
                .append("\nPago: EXITOSO\nReferencia: ").append(c.referenciaPago())
                .append("\nFecha de pago (UTC): ").append(c.pagadoEn())
                .append("\nCliente: ").append(c.cliente())
                .append("\nRecibe: ").append(c.destinatarioEnvio())
                .append("\nDirección de envío: ").append(c.direccionEnvio())
                .append("\nTeléfono: ").append(c.telefono() == null || c.telefono().isBlank() ? "No proporcionado" : c.telefono())
                .append("\n\nProductos:\n");
        for (var p : c.productos()) {
            texto.append("- ").append(p.nombre()).append(" / ").append(p.marca())
                    .append(" (ID: ").append(p.productoId()).append(")")
                    .append(" | Cantidad: ").append(p.cantidad())
                    .append(" | Precio unitario: ").append(dinero(p.precioUnitario()))
                    .append(" | Importe: ").append(dinero(p.precioUnitario().multiply(BigDecimal.valueOf(p.cantidad()))))
                    .append("\n");
        }
        return texto.append("\nSubtotal: ").append(dinero(c.subtotal()))
                .append("\nEnvío: ").append(dinero(c.envio()))
                .append("\nDescuento: ").append(dinero(c.descuento()))
                .append("\nTotal pagado: ").append(dinero(c.total()))
                .append("\n\nPreparar y etiquetar los productos para su envío.\n").toString();
    }

    private String dinero(BigDecimal valor) {
        return "$" + valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString() + " MXN";
    }
}
