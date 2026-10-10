package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.repository.UsuarioRepository;

import jakarta.validation.constraints.Email;

import com.example.demo.model.Usuario;

@Service 
public class UsuarioServicio {
    @Autowired 
    private UsuarioRepository usuarioRepository;

    //encriptar contraseña
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UsuarioServicio(UsuarioRepository usuarioRepository){
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> getUsuarios(){
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> getUsuario(Long id){
        return usuarioRepository.findById(id);
    }

    public Usuario registrar(Usuario user){
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setActivo(true);
        user.setContrasenia(encoder.encode(user.getContrasenia()));
        return usuarioRepository.save(user);
    }

    public void eliminarUsuario(Usuario user){
        user.setActivo(false);
    }

    public boolean emailRepetido(String email){
    if (usuarioRepository.existsByEmail(email)) {
        return true;
    } else {
        return false;
    }
    }

    public Usuario obtenerEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con el email: " + email));
    }
}
