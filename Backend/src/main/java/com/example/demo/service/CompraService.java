package com.example.demo.service;

import com.example.demo.model.Pedido;
import com.example.demo.notificaciones.CompraParaNotificar;
import com.example.demo.notificaciones.NotificacionCompraService;
import com.example.demo.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompraService {

    private final PedidoRepository pedidoRepository;
    private final NotificacionCompraService notificacionCompraService;

    public CompraService(PedidoRepository pedidoRepository, NotificacionCompraService notificacionCompraService) {
        this.pedidoRepository = pedidoRepository;
        this.notificacionCompraService = notificacionCompraService;
    }

    @Transactional
    public void confirmarCompraYPago(String pedidoId, String referenciaPagoProveedor) {
        // 1. Cargar el pedido de la base de datos
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + pedidoId));

        // 2. Prevenir procesamiento duplicado si ya fue pagado
        if (pedido.getEstado() == Pedido.EstadoPedido.PAGADO) {
            return;
        }

        // 3. Actualizar estado a PAGADO
        pedido.setEstado(Pedido.EstadoPedido.PAGADO);
        pedidoRepository.save(pedido);

        // 4. Mapear productos con comprobación de nulos
        List<CompraParaNotificar.Producto> productosNotificacion = Collections.emptyList();
        if (pedido.getProductos() != null) {
            productosNotificacion = pedido.getProductos().stream()
                    .map(item -> new CompraParaNotificar.Producto(
                            item.getProductoId() != null ? item.getProductoId() : "",
                            item.getNombre() != null ? item.getNombre() : "",
                            item.getMarca() != null ? item.getMarca() : "",
                            item.getCantidad(),
                            item.getPrecioUnitario() != null ? item.getPrecioUnitario() : BigDecimal.ZERO
                    ))
                    .collect(Collectors.toList());
        }

        // 5. Construir el DTO para el servicio de notificaciones (HU-13)
        CompraParaNotificar datosNotificacion = new CompraParaNotificar(
                pedido.getId(),
                referenciaPagoProveedor,
                CompraParaNotificar.EstadoPago.EXITOSO,
                Instant.now(),
                pedido.getClienteNombre() != null ? pedido.getClienteNombre() : "",
                pedido.getDestinatarioEnvio() != null ? pedido.getDestinatarioEnvio() : "",
                pedido.getDireccionEnvio() != null ? pedido.getDireccionEnvio() : "",
                pedido.getTelefono() != null ? pedido.getTelefono() : "",
                productosNotificacion,
                pedido.getSubtotal() != null ? pedido.getSubtotal() : BigDecimal.ZERO,
                pedido.getEnvio() != null ? pedido.getEnvio() : BigDecimal.ZERO,
                pedido.getDescuento() != null ? pedido.getDescuento() : BigDecimal.ZERO,
                pedido.getTotal() != null ? pedido.getTotal() : BigDecimal.ZERO,
                "MXN"
        );

        // 6. Registrar notificación en la misma transacción activa
        notificacionCompraService.registrar(datosNotificacion);
    }
}