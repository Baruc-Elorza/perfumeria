package com.example.demo.service;

import com.example.demo.model.Pedido;
import com.example.demo.notificaciones.CompraParaNotificar;
import com.example.demo.notificaciones.NotificacionCompraService;
import com.example.demo.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.dto.CarritoRespuesta;
import java.util.UUID;

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
    public Pedido crearPedidoPendiente(
        String clienteEmail,
        String calle,
        String colonia,
        String ciudad,
        String codigoPostal,
        String telefono,
        CarritoRespuesta carrito) {

    if (clienteEmail == null || clienteEmail.isBlank()) {
        throw new IllegalArgumentException("El usuario debe iniciar sesión.");
    }

    if (carrito == null || carrito.items() == null || carrito.items().isEmpty()) {
        throw new IllegalArgumentException("El carrito está vacío.");
    }

    BigDecimal subtotal = carrito.subtotal();

    if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException("El total del pedido no es válido.");
    }

    Pedido pedido = new Pedido();
    pedido.setId(UUID.randomUUID().toString());
    pedido.setClienteEmail(clienteEmail);
    pedido.setClienteNombre(clienteEmail);
    pedido.setDestinatarioEnvio(clienteEmail);
    pedido.setDireccionEnvio(
        calle + ", " + colonia + ", " + ciudad + ", C.P. " + codigoPostal
    );
    pedido.setTelefono(telefono);
    pedido.setEstado(Pedido.EstadoPedido.PENDIENTE);
    pedido.setSubtotal(subtotal);
    pedido.setEnvio(BigDecimal.ZERO);
    pedido.setDescuento(BigDecimal.ZERO);
    pedido.setTotal(subtotal);

    
    pedido.setProductos(
        carrito.items().stream()
            .map(item -> new Pedido.ItemProducto(
                String.valueOf(item.perfumeId()),
                item.nombre(),
                item.marca(),
                item.cantidad(),
                item.precio()
            ))
            .toList()
    );


    return pedidoRepository.save(pedido);
}

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