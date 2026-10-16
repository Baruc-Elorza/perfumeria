package com.example.demo.service;

import com.example.demo.model.Direccion;
import com.example.demo.repository.DireccionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DireccionService {

    @Autowired
    private DireccionRepository direccionRepository;

    public List<Direccion> obtenerDireccionesPorUsuario(Long usuarioId) {
        return direccionRepository.findByUsuarioId(usuarioId);
    }

    public Direccion guardarDireccion(Direccion direccion) {
        // Lógica para guardar la dirección en la base de datos
        return direccionRepository.save(direccion);
    }

    public void eliminarDireccion(Long id) {
        direccionRepository.deleteById(id);
    }
}