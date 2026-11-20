package com.example.demo.service;

import com.example.demo.dto.UsuarioAdminRespuesta;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import java.util.Comparator;
import java.util.List;
import java.math.BigInteger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdministracionUsuariosService {
    private final UsuarioRepository usuarios;
    private final UsuarioDetailsService roles;

    public AdministracionUsuariosService(UsuarioRepository usuarios, UsuarioDetailsService roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminRespuesta> listar(String actor) {
        return usuarios.findAll().stream().sorted(Comparator.comparing(Usuario::getId))
                .map(u -> respuesta(u, actor)).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioAdminRespuesta detalle(BigInteger id, String actor) {
        return respuesta(usuarios.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe.")), actor);
    }

    @Transactional
    public UsuarioAdminRespuesta cambiarEstado(BigInteger id, boolean activo, String actor) {
        Usuario u = usuarios.bloquearPorId(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe."));
        if (u.getEmail().equalsIgnoreCase(actor)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes cambiar el estado de tu propia cuenta.");
        }
        if (roles.esAdministrador(u.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta pantalla solo cambia el estado de clientes.");
        }
        if (u.getActivo() != activo) {
            u.setActivo(activo);
            // Desbloquear no reactiva sesiones anteriores.
            u.revocarSesiones();
        }
        return respuesta(u, actor);
    }

    private UsuarioAdminRespuesta respuesta(Usuario u, String actor) {
        boolean admin = roles.esAdministrador(u.getEmail());
        return new UsuarioAdminRespuesta(u.getId(), u.getNombre(), u.getEmail(), u.getActivo(),
                admin, !admin && !u.getEmail().equalsIgnoreCase(actor));
    }
}
