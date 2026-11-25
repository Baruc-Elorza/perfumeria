package com.example.demo.notificaciones;



import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notificacion_compra",
        uniqueConstraints = @UniqueConstraint(name = "uk_compra_tipo", columnNames = {"pedido_id", "tipo"}),
        indexes = @Index(name = "idx_notificacion_pendiente", columnList = "estado,proximoIntento"))
public class NotificacionCompra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 100)
    Long pedidoId;
    @Column(nullable = false, length = 20)
    String tipo;
    @Column(nullable = false, length = 150)
    String referenciaPago;
    @Column(nullable = false)
    String asunto;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT")
    String cuerpo;
    @Column(length = 254)
    String destinatario;
    @Column(nullable = false, length = 20)
    String estado = "PENDIENTE";
    @Column(nullable = false)
    int intentos;
    @Column(nullable = false)
    Instant creadoEn = Instant.now();
    @Column(nullable = false)
    Instant proximoIntento = Instant.now();
    Instant enviadoEn;
    @Column(length = 100)
    String ultimoError;

    protected NotificacionCompra() {}

    NotificacionCompra(Long pedidoId, String referenciaPago, String asunto, String cuerpo,
            String tipo, String destinatario) {
        this.pedidoId = pedidoId;
        this.referenciaPago = referenciaPago;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.tipo = tipo;
        this.destinatario = destinatario;
    }
}
