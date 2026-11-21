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
    private final String administrador;

    public NotificacionCompraService(NotificacionCompraRepository repository, Validator validator,
            @org.springframework.beans.factory.annotation.Value("${notificaciones.compra.destinatario:}") String administrador) {
        this.repository = repository;
        this.validator = validator;
        this.administrador = administrador;
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
        var existentes = repository.findAllByPedidoId(compra.pedidoId());
        if (existentes.stream().anyMatch(n -> !n.referenciaPago.equals(compra.referenciaPago()))) {
            throw new IllegalArgumentException("El pedido ya tiene otra referencia de pago notificada.");
        }
        String detalle = redactar(compra);
        if (existentes.stream().noneMatch(n -> "COMPRADOR".equals(n.tipo))) {
            repository.save(new NotificacionCompra(compra.pedidoId(), compra.referenciaPago(),
                    "[PRUEBA] Confirmación de tu compra - Pedido " + compra.pedidoId(),
                    "Hola " + compra.cliente() + ", gracias por tu compra de prueba.\n"
                            + "No se ha realizado ningún cargo real.\n\n" + detalle,
                    "COMPRADOR", compra.clienteEmail()));
        }
        if (existentes.stream().noneMatch(n -> "ADMIN".equals(n.tipo))) {
            repository.save(new NotificacionCompra(compra.pedidoId(), compra.referenciaPago(),
                    "[PRUEBA] Nueva compra - Pedido " + compra.pedidoId(),
                    "Nueva compra simulada. No se ha realizado ningún cargo real.\n\n" + detalle
                            + "\nResumen de prueba para administración; no realizar un envío real.\n",
                    "ADMIN", administrador == null || administrador.isBlank() ? null : administrador.trim()));
        }
    }

    private String redactar(CompraParaNotificar c) {
        StringBuilder texto = new StringBuilder("Se confirmó el pago del siguiente pedido.\n\n");
        texto.append("Pedido: ").append(c.pedidoId())
                .append("\nPago: EXITOSO\nReferencia: ").append(c.referenciaPago())
                .append("\nFecha de pago (UTC): ").append(c.pagadoEn())
                .append("\nCliente: ").append(c.cliente())
                .append("\nCorreo del comprador: ").append(c.clienteEmail())
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
                .append("\n").toString();
    }

    private String dinero(BigDecimal valor) {
        return "$" + valor.setScale(2, RoundingMode.UNNECESSARY).toPlainString() + " MXN";
    }
}
