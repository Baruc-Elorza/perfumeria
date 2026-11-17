
package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final com.example.demo.repository.UsuarioRepository usuarios;

    public AuthController(AuthenticationManager authenticationManager, com.example.demo.repository.UsuarioRepository usuarios) {
        this.authenticationManager = authenticationManager;
        this.usuarios = usuarios;
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String contrasenia
    ) {}

    public record LoginResponse(String mensaje, String email, boolean administrador) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest datos,
            HttpServletRequest request) {

        long versionSesion = usuarios.findByEmail(datos.email())
                .map(com.example.demo.model.Usuario::getVersionSesion).orElse(-1L);
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                datos.email(),
                                datos.contrasenia()
                        )
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        var session = request.getSession(false);
        if (session != null) {
            request.changeSessionId();
            session = request.getSession(false);
        } else {
            session = request.getSession(true);
        }
        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                context
        );
        session.setAttribute("versionSesion", versionSesion);

        return ResponseEntity.ok(
                new LoginResponse(
                        "Inicio de sesión correcto",
                        authentication.getName(),
                        esAdministrador(authentication)
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse> me(
            @AuthenticationPrincipal UserDetails usuario) {
        return ResponseEntity.ok(new LoginResponse(
                "Sesión activa",
                usuario.getUsername(),
                usuario.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(
                new LoginResponse("Sesión cerrada", "", false)
        );
    }

    private boolean esAdministrador(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
