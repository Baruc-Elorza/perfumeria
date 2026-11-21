package com.example.demo.service;

import com.example.demo.model.Direccion;
import com.example.demo.model.Usuario;
import com.example.demo.repository.DireccionRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigInteger;
import java.util.List;

@Service
@Transactional
public class DireccionService {
    private final DireccionRepository direcciones;
    private final UsuarioRepository usuarios;

    public DireccionService(DireccionRepository direcciones, UsuarioRepository usuarios) {
        this.direcciones = direcciones;
        this.usuarios = usuarios;
    }

    private Usuario usuario(String email) {
        return usuarios.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debes iniciar sesión."));
    }

    public List<Direccion> listar(String email) {
        return direcciones.findByUsuarioId(usuario(email).getId());
    }

    public List<Direccion> listarPorUsuario(BigInteger id, String email) {
        if (!usuario(email).getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes consultar tus direcciones.");
        }
        return listar(email);
    }

    public Direccion crear(Direccion datos, String email) {
        if (datos.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usa la opción de editar para actualizar una dirección.");
        }
        Direccion nueva = new Direccion();
        nueva.setUsuario(usuario(email));
        copiar(datos, nueva);
        return direcciones.save(nueva);
    }

    public Direccion actualizar(BigInteger id, Direccion datos, String email) {
        Direccion propia = buscarPropia(id, email);
        copiar(datos, propia);
        return direcciones.save(propia);
    }

    public void eliminar(BigInteger id, String email) {
        direcciones.delete(buscarPropia(id, email));
    }

    private Direccion buscarPropia(BigInteger id, String email) {
        return direcciones.findByIdAndUsuarioId(id, usuario(email).getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "La dirección no existe en tu cuenta."));
    }

    private void copiar(Direccion datos, Direccion destino) {
        destino.setCalle(datos.getCalle());
        destino.setNumeroExterior(datos.getNumeroExterior());
        destino.setNumeroInterior(datos.getNumeroInterior());
        destino.setColonia(datos.getColonia());
        destino.setCiudad(datos.getCiudad());
        destino.setEstado(datos.getEstado());
        destino.setCodigoPostal(datos.getCodigoPostal());
        destino.setTelefono(datos.getTelefono());
        destino.setReferencias(datos.getReferencias());
        destino.setEsPrincipal(Boolean.TRUE.equals(datos.getEsPrincipal()));
    }
}
