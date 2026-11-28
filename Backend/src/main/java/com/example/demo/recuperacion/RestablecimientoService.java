package com.example.demo.recuperacion;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RestablecimientoService {
    private final UsuarioRepository usuarios;
    private final TokenRestablecimientoRepository tokens;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public RestablecimientoService(UsuarioRepository usuarios, TokenRestablecimientoRepository tokens, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.encoder = encoder;
    }

    @Transactional
    public Optional<CorreoEnlace> generar(String email) {
        var encontrada = usuarios.buscarIdPorEmail(email);
        if (encontrada.isEmpty()) return Optional.empty();
        Usuario usuario = usuarios.bloquearPorId(encontrada.get()).orElse(null);
        if (usuario == null || !usuario.getActivo()) return Optional.empty();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        TokenRestablecimiento registro = tokens.findById(usuario.getId()).orElseGet(() -> new TokenRestablecimiento(usuario.getId()));
        registro.hashToken = hash(token);
        registro.venceEn = Instant.now().plus(Duration.ofMinutes(15));
        registro.utilizado = false;
        registro.versionSesion = usuario.getVersionSesion();
        tokens.save(registro);
        return Optional.of(new CorreoEnlace(usuario.getEmail(), token));
    }

    @Transactional(readOnly = true)
    public void validar(String token) {
        TokenRestablecimiento registro = buscar(token);
        Usuario usuario = usuarios.findById(registro.usuarioId).orElseThrow(RestablecimientoService::invalido);
        comprobar(registro, usuario);
    }

    @Transactional
    public String restablecer(String token, String nueva, String confirmacion) {
        validarContrasenia(nueva, confirmacion);
        TokenRestablecimiento inicial = buscar(token);
        // Mismo orden de bloqueo que al generar enlaces: primero usuario, después token.
        Usuario usuario = usuarios.bloquearPorId(inicial.usuarioId).orElseThrow(RestablecimientoService::invalido);
        TokenRestablecimiento registro = tokens.bloquear(hash(token)).orElseThrow(RestablecimientoService::invalido);
        comprobar(registro, usuario);
        if (encoder.matches(nueva, usuario.getContrasenia())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige una contraseña diferente de la anterior.");
        }
        usuario.setContrasenia(encoder.encode(nueva));
        usuario.revocarSesiones();
        registro.utilizado = true;
        return usuario.getEmail();
    }

    private TokenRestablecimiento buscar(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalido();
        return tokens.findByHashToken(hash(token)).orElseThrow(RestablecimientoService::invalido);
    }
    private void comprobar(TokenRestablecimiento registro, Usuario usuario) {
        if (registro.utilizado || !registro.venceEn.isAfter(Instant.now()) || !usuario.getActivo()
                || registro.versionSesion != usuario.getVersionSesion()) throw invalido();
    }
    static ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace es inválido, ya se utilizó o venció. Solicita uno nuevo.");
    }
    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private void validarContrasenia(String nueva, String confirmacion) {
        if (nueva == null || nueva.isBlank() || nueva.length() < 7 || nueva.length() > 64
                || nueva.getBytes(StandardCharsets.UTF_8).length > 72
                || nueva.codePoints().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Usa de 7 a 64 caracteres, hasta 72 bytes UTF-8, sin caracteres de control.");
        }
        if (!nueva.equals(confirmacion)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las contraseñas no coinciden.");
        }
    }
    public record CorreoEnlace(String email, String token) {
        @Override public String toString() { return "CorreoEnlace[protegido]"; }
    }
}
