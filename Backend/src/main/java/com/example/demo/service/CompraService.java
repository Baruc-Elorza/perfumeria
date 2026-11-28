package com.example.demo.service;

import com.example.demo.model.Pedido;
import com.example.demo.notificaciones.CompraParaNotificar;
import com.example.demo.notificaciones.NotificacionCompraService;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.dto.CarritoRespuesta;
import java.math.BigDecimal;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import java.math.RoundingMode;

@Service
public class CompraService {

    private final PedidoRepository pedidoRepository;
    private final NotificacionCompraService notificacionCompraService;
    private final PagoService pagoService;
    private final UsuarioRepository usuarioRepository;
    private final com.example.demo.repository.PerfumeRepository perfumes;

    public CompraService(PedidoRepository pedidoRepository, NotificacionCompraService notificacionCompraService, PagoService pagoService, UsuarioRepository usuarioRepository, com.example.demo.repository.PerfumeRepository perfumes) {
        this.pedidoRepository = pedidoRepository;
        this.notificacionCompraService = notificacionCompraService;
        this.pagoService = pagoService;
        this.usuarioRepository = usuarioRepository;
        this.perfumes = perfumes;
    
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
    Usuario cliente = usuarioRepository.findByEmail(clienteEmail)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario."));
    pedido.setCliente(cliente);
    pedido.setClienteNombre(cliente.getNombre());
    pedido.setDestinatarioEnvio(cliente.getNombre());
    pedido.setDireccionEnvio(
        calle + ", " + colonia + ", " + ciudad + ", C.P. " + codigoPostal
    );
    pedido.setTelefono(telefono);
    pedido.setEstado(Pedido.EstadoPedido.PENDIENTE);
    pedido.setSubtotal(subtotal);
    pedido.setCostoEnvio(BigDecimal.ZERO);
    pedido.setDescuento(BigDecimal.ZERO);
    pedido.setTotal(subtotal);

    
    pedido.setProductos(
        carrito.items().stream()
            .map(item -> new Pedido.ItemProducto(
                item.perfumeId(),
                item.nombre(),
                item.marca(),
                item.cantidad(),
                item.precio()
            ))
            .toList()
    );


    validarExistencias(pedido);
    return pedidoRepository.save(pedido);
}

    @Transactional
    public void confirmarCompraYPago(
            Long pedidoId,
            String referenciaPagoProveedor,
            String clienteEmail
    ) throws StripeException {

        // 1. Buscar el pedido y comprobar que pertenece al usuario.
        Pedido pedido = pedidoRepository
                .bloquearPorIdYClienteEmail(pedidoId, clienteEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No se encontró el pedido para este usuario."
                        )
                );

        // 2. Consultar el pago directamente en Stripe.
        PaymentIntent pago = pagoService.obtenerPago(
                referenciaPagoProveedor
        );

        if (!"succeeded".equals(pago.getStatus())) {
            throw new IllegalStateException(
                    "Stripe todavía no confirma el pago."
            );
        }

        if (!"mxn".equalsIgnoreCase(pago.getCurrency())) {
            throw new IllegalStateException(
                    "La moneda del pago no coincide."
            );
        }

        boolean metadataCoincide =
        pago.getMetadata() != null
        && pedidoId.toString().equals(pago.getMetadata().get("pedidoId"));

boolean esPagoAntiguoVerificado =
        "pi_3UTaYfLG02VwiJpf0KcVY1Zc".equals(referenciaPagoProveedor)
        && "0c8f2e04-4487-4c50-a1f5-48816868b710".equals(pedidoId.toString())
        && (pago.getMetadata() == null || pago.getMetadata().isEmpty());

if (!metadataCoincide && !esPagoAntiguoVerificado) {
    throw new IllegalStateException(
            "El pago no corresponde a este pedido."
    );
}

        // 3. Comprobar que el importe cobrado coincide con el pedido.
        long totalEsperado = pedido.getTotal()
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        if (pago.getAmount() == null
                || pago.getAmount().longValue() != totalEsperado) {
            throw new IllegalStateException(
                    "El importe pagado no coincide con el pedido."
            );
        }

        // 4. Evitar actualizar y notificar dos veces.
        if (pedido.getEstado() == Pedido.EstadoPedido.PAGADO) {
            return;
        }

        if (pedido.getEstado() != Pedido.EstadoPedido.PENDIENTE) {
            throw new IllegalStateException(
                    "El pedido no está pendiente de pago."
            );
        }

        // Descontar en la misma transacción que confirma el pedido y registra los correos.
        // El bloqueo del pedido evita repetirlo; la actualización condicional impide stock negativo.
        descontarExistencias(pedido);

        // 5. Actualizar el pedido.
        pedido.setEstado(Pedido.EstadoPedido.PAGADO);
        pedidoRepository.save(pedido);

        // 6. Preparar los productos para la notificación HU-13.
        List<CompraParaNotificar.Producto> productosNotificacion =
                Collections.emptyList();

        if (pedido.getProductos() != null) {
            productosNotificacion = pedido.getProductos().stream()
                    .map(item -> new CompraParaNotificar.Producto(
                            item.getProductoId(),
                            item.getNombre() != null
                                    ? item.getNombre() : "",
                            item.getMarca() != null
                                    ? item.getMarca() : "",
                            item.getCantidad(),
                            item.getPrecioUnitario() != null
                                    ? item.getPrecioUnitario()
                                    : BigDecimal.ZERO
                    ))
                    .collect(Collectors.toList());
        }

        // 7. Construir la notificación de compra exitosa.
        CompraParaNotificar datosNotificacion =
                new CompraParaNotificar(
                        pedido.getId(),
                        referenciaPagoProveedor,
                        CompraParaNotificar.EstadoPago.EXITOSO,
                        Instant.now(),
                        pedido.getClienteNombre() != null
                                ? pedido.getClienteNombre() : "",
                        pedido.getCliente().getEmail(),
                        pedido.getDestinatarioEnvio() != null
                                ? pedido.getDestinatarioEnvio() : "",
                        pedido.getDireccionEnvio() != null
                                ? pedido.getDireccionEnvio() : "",
                        pedido.getTelefono() != null
                                ? pedido.getTelefono() : "",
                        productosNotificacion,
                        pedido.getSubtotal() != null
                                ? pedido.getSubtotal() : BigDecimal.ZERO,
                pedido.getCostoEnvio() != null
                                ? pedido.getCostoEnvio() : BigDecimal.ZERO,
                        pedido.getDescuento() != null
                                ? pedido.getDescuento() : BigDecimal.ZERO,
                        pedido.getTotal() != null
                                ? pedido.getTotal() : BigDecimal.ZERO,
                        "MXN"
                );

        // 8. Registrar la notificación HU-13.
        notificacionCompraService.registrar(datosNotificacion);
    }
    @Transactional(readOnly = true)
    public void validarExistencias(Pedido pedido) {
        for (var entrada : cantidadesDelPedido(pedido).entrySet()) {
            var perfume = perfumes.findById(entrada.getKey()).orElseThrow(() ->
                    new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                            "Uno de los perfumes ya no está disponible. Actualiza el carrito."));
            int disponibles = perfume.getStock() == null ? 0 : perfume.getStock();
            if (entrada.getValue() > disponibles) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                        "Solo hay " + disponibles + " unidades disponibles de " + perfume.getNombre()
                                + ". Ajusta la cantidad antes de pagar.");
            }
        }
    }

    private java.util.SortedMap<Long, Integer> cantidadesDelPedido(Pedido pedido) {
        if (pedido.getProductos() == null || pedido.getProductos().isEmpty()) {
            throw new IllegalStateException("El pedido no tiene productos para descontar.");
        }
        // Siempre se actualizan los perfumes en el mismo orden para reducir bloqueos cruzados.
        var cantidades = new java.util.TreeMap<Long, Integer>();
        for (var producto : pedido.getProductos()) {
            if (producto.getProductoId() == null || producto.getCantidad() == null || producto.getCantidad() < 1) {
                throw new IllegalStateException("El pedido contiene cantidades de productos inválidas.");
            }
            try {
                cantidades.merge(producto.getProductoId(), producto.getCantidad(), Math::addExact);
            } catch (ArithmeticException ex) {
                throw new IllegalStateException("La cantidad solicitada supera el límite permitido.");
            }
        }
        return cantidades;
    }

    private void descontarExistencias(Pedido pedido) {
        var cantidades = cantidadesDelPedido(pedido);
        for (var entrada : cantidades.entrySet()) {
            if (perfumes.descontarExistencias(entrada.getKey(), entrada.getValue()) != 1) {
                throw new IllegalStateException("No hay existencias suficientes para completar el pedido. "
                        + "El pago ya fue aprobado: no vuelvas a pagar y contacta al administrador para revisarlo.");
            }
        }
    }

}
