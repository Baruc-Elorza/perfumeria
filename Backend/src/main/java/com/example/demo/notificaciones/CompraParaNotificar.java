package com.example.demo.notificaciones;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

import java.time.Instant;
import java.util.List;

/** Datos históricos del pedido pagado, obtenidos del backend, nunca del navegador. */
public record CompraParaNotificar(
        @NotNull Long pedidoId,
        @NotBlank @Size(max = 150) String referenciaPago,
        @NotNull EstadoPago estadoPago,
        @NotNull Instant pagadoEn,
        @NotBlank @Size(max = 200) String cliente,
        @NotBlank @Email @Size(max = 254) String clienteEmail,
        @NotBlank @Size(max = 200) String destinatarioEnvio,
        @NotBlank @Size(max = 1000) String direccionEnvio,
        @Size(max = 50) String telefono,
        @NotEmpty @Size(max = 200) List<@NotNull @Valid Producto> productos,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal subtotal,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal envio,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal descuento,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal total,
        @NotNull @Pattern(regexp = "MXN") String moneda) {

    public enum EstadoPago { PENDIENTE, EXITOSO, RECHAZADO, CANCELADO }

    public record Producto(
            @NotNull Long productoId,
            @NotBlank @Size(max = 255) String nombre,
            @NotBlank @Size(max = 255) String marca,
            @Min(1) int cantidad,
            @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal precioUnitario) {}
}
