package com.example.demo.notificaciones;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notificacion_compra",
        indexes = @Index(name = "idx_notificacion_pendiente", columnList = "estado,proximoIntento"))
public class NotificacionCompra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true, length = 100)
    String pedidoId;
    @Column(nullable = false, length = 150)
    String referenciaPago;
    @Column(nullable = false)
    String asunto;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT")
    String cuerpo;
    @Column(nullable = false, length = 20)
    String estado = "PENDIENTE";
    @Column(nullable = false)
    int intentos;
    @Column(nullable = false)
    Instant creadoEn = Instant.now();
    @Column(nullable = false)
    Instant proximoIntento = Instant.now();
    Instant enviadoEn;
    String destinatario;
    @Column(length = 100)
    String ultimoError;

    protected NotificacionCompra() {}

    NotificacionCompra(String pedidoId, String referenciaPago, String asunto, String cuerpo) {
        this.pedidoId = pedidoId;
        this.referenciaPago = referenciaPago;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
    }
}
