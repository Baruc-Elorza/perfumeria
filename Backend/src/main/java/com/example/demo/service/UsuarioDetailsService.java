
package com.example.demo.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final String correosAdministrador;

    public UsuarioDetailsService(
            UsuarioRepository usuarioRepository,
            @Value("${app.admin.emails:}") String correosAdministrador) {
        this.usuarioRepository = usuarioRepository;
        this.correosAdministrador = correosAdministrador;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Credenciales incorrectas"
                        )
                );

        boolean administrador = java.util.Arrays.stream(correosAdministrador.split(","))
                .map(String::trim)
                .anyMatch(adminEmail -> !adminEmail.isEmpty()
                        && adminEmail.equalsIgnoreCase(usuario.getEmail()));

        return User.withUsername(usuario.getEmail())
                .password(usuario.getContrasenia())
                .disabled(!usuario.getActivo())
                .roles(administrador ? "ADMIN" : "USER")
                .build();
    }
}
