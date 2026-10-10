
package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.UsuarioRegistroDTO;
import com.example.demo.dto.UsuarioRespuestaDTO;
import com.example.demo.model.Usuario;
import com.example.demo.service.UsuarioServicio;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioServicio usuarioServicio;

    public UsuarioController(UsuarioServicio usuarioServicio) {
        this.usuarioServicio = usuarioServicio;
    }

    // Convertir entidad a DTO de respuesta
    private UsuarioRespuestaDTO convertirDTO(Usuario usuario) {
        return new UsuarioRespuestaDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail()
        );
    }

    // Obtener todos los usuarios
    @GetMapping
    public ResponseEntity<List<UsuarioRespuestaDTO>> getUsuarios() {
        List<UsuarioRespuestaDTO> usuarios = usuarioServicio.getUsuarios()
                .stream()
                .map(this::convertirDTO)
                .toList();

        return ResponseEntity.ok(usuarios);
    }

    // Obtener usuario por ID
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioRespuestaDTO> getUsuario(
            @PathVariable Long id) {

        return usuarioServicio.getUsuario(id)
                .map(this::convertirDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Registrar usuario
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(
            @Valid @RequestBody UsuarioRegistroDTO registroDTO) {

        if (usuarioServicio.emailRepetido(registroDTO.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El correo ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(registroDTO.getNombre());
        usuario.setEmail(registroDTO.getEmail());
        usuario.setContrasenia(registroDTO.getContrasenia());

        Usuario usuarioGuardado = usuarioServicio.registrar(usuario);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(convertirDTO(usuarioGuardado));
    }

    // Comprobar si el correo existe
    @GetMapping("/existe-email")
    public ResponseEntity<Boolean> emailRepetido(
            @RequestParam String email) {

        return ResponseEntity.ok(
                usuarioServicio.emailRepetido(email)
        );
    }
}
