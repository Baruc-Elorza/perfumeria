package com.example.demo.service;

import com.example.demo.model.Perfume;
import com.example.demo.repository.PerfumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.example.demo.repository.CarritoPerfumeRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PerfumeService {

    private final PerfumeRepository perfumeRepository;
    private final CarritoPerfumeRepository carritoPerfumeRepository;

    public PerfumeService(PerfumeRepository perfumeRepository, CarritoPerfumeRepository carritoPerfumeRepository) {
        this.perfumeRepository = perfumeRepository;
        this.carritoPerfumeRepository = carritoPerfumeRepository;
    }

    public List<Perfume> listar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return perfumeRepository.findAll();
        }

        return perfumeRepository.findByNombreContainingIgnoreCase(nombre.trim());
    }

    public Optional<Perfume> obtenerPorId(Long id) {
        return perfumeRepository.findById(id);
    }

    @Transactional
    public Perfume guardar(Perfume perfume) {
        Perfume nuevo = new Perfume();
        copiar(perfume, nuevo);
        return perfumeRepository.save(nuevo);
    }

    @Transactional
    public Perfume actualizar(Long id, Perfume datos) {
        Perfume existente = perfumeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El perfume ya no existe."));
        copiar(datos, existente);
        return perfumeRepository.save(existente);
    }

    private void copiar(Perfume datos, Perfume destino) {
        if (datos.getNombre() == null || datos.getNombre().isBlank()
                || datos.getMarca() == null || datos.getMarca().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre y marca son obligatorios.");
        }
        if (datos.getPrecio() == null || !Double.isFinite(datos.getPrecio()) || datos.getPrecio() < 0
                || datos.getStock() == null || datos.getStock() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Precio y existencias deben ser números no negativos.");
        }
        validarLongitud(datos.getNombre(), 255);
        validarLongitud(datos.getMarca(), 255);
        validarLongitud(datos.getDescripcion(), 500);
        validarLongitud(datos.getNotasTop(), 500);
        validarLongitud(datos.getNotasMiddle(), 255);
        validarLongitud(datos.getNotasBase(), 255);
        validarLongitud(datos.getImagenUrl(), 255);
        destino.setNombre(datos.getNombre().trim());
        destino.setMarca(datos.getMarca().trim());
        destino.setDescripcion(datos.getDescripcion());
        destino.setNotasTop(datos.getNotasTop());
        destino.setNotasMiddle(datos.getNotasMiddle());
        destino.setNotasBase(datos.getNotasBase());
        destino.setPrecio(datos.getPrecio());
        destino.setStock(datos.getStock());
        destino.setImagenUrl(datos.getImagenUrl());
    }

    private void validarLongitud(String valor, int maximo) {
        if (valor != null && valor.length() > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno de los campos supera su longitud permitida.");
        }
    }

    public boolean existe(Long id) {
        return perfumeRepository.existsById(id);
    }

    @Transactional
    public void eliminar(Long id) {
        Perfume perfume = perfumeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El perfume ya no existe."));
        if (carritoPerfumeRepository.existsByPerfumeId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El perfume está en un carrito. Puedes poner sus existencias en 0 para impedir nuevas compras.");
        }
        perfumeRepository.delete(perfume);
        perfumeRepository.flush();
    }
}
