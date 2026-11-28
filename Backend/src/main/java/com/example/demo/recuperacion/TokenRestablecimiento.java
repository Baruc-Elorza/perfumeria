package com.example.demo.recuperacion;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recuperacion_contrasena")
public class TokenRestablecimiento {
    @Id
    Long usuarioId;
    @Column(nullable = false, unique = true, length = 64)
    String hashToken;
    @Column(nullable = false)
    Instant venceEn;
    @Column(nullable = false)
    boolean utilizado;
    @Column(nullable = false)
    long versionSesion;

    protected TokenRestablecimiento() {}
    TokenRestablecimiento(Long usuarioId) { this.usuarioId = usuarioId; }
}
