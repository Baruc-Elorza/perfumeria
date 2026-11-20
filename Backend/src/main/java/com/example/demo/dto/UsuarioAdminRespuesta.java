package com.example.demo.dto;

import java.math.BigInteger;

/** Nunca incluir contraseña ni su hash. */
public record UsuarioAdminRespuesta(BigInteger id, String nombre, String email,
                                    boolean activo, boolean administrador, boolean puedeCambiarEstado) {}
