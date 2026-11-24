package com.example.demo.controller;

import com.example.demo.dto.UsuarioAdminRespuesta;
import com.example.demo.service.AdministracionUsuariosService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuariosController {
    private final AdministracionUsuariosService servicio;
    public AdminUsuariosController(AdministracionUsuariosService servicio) { this.servicio = servicio; }

    @GetMapping
    public List<UsuarioAdminRespuesta> listar(Authentication auth) {
        return servicio.listar(auth.getName());
    }

    @GetMapping("/{id}")
    public UsuarioAdminRespuesta detalle(@PathVariable Long id, Authentication auth) {
        return servicio.detalle(id, auth.getName());
    }

    @PatchMapping("/{id}/estado")
    public UsuarioAdminRespuesta estado(@PathVariable Long id, @Valid @RequestBody Estado datos,
                                       Authentication auth) {
        return servicio.cambiarEstado(id, datos.activo(), auth.getName());
    }

    public record Estado(@NotNull Boolean activo) {}
}
