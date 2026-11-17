package com.example.demo.dto;

/** Nunca incluir contraseña ni su hash. */
public record UsuarioAdminRespuesta(Long id, String nombre, String email,
                                    boolean activo, boolean administrador, boolean puedeCambiarEstado) {}
