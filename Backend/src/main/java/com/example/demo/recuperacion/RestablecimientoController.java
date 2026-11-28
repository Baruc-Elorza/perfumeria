package com.example.demo.recuperacion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/recuperacion")
public class RestablecimientoController {
    public static final String MENSAJE = "Si el correo corresponde a una cuenta activa, recibirás un enlace para restablecer tu contraseña. Revisa también la carpeta de spam.";
    private final RestablecimientoService servicio;
    private final CorreoRecuperacion correo;
    private final LimitadorRecuperacion limites;
    private final int maximoIp;
    private final int maximoValidaciones;

    public RestablecimientoController(RestablecimientoService servicio, CorreoRecuperacion correo, LimitadorRecuperacion limites,
            @Value("${app.recuperacion.max-solicitudes-ip:20}") int maximoIp,
            @Value("${app.recuperacion.max-validaciones-ip:30}") int maximoValidaciones) {
        this.servicio = servicio; this.correo = correo; this.limites = limites;
        this.maximoIp = maximoIp; this.maximoValidaciones = maximoValidaciones;
    }
    public record Solicitud(@NotBlank @Email @Size(max = 254) String email) {
        public Solicitud { if (email != null) email = email.trim().toLowerCase(Locale.ROOT); }
        @Override public String toString() { return "Solicitud[protegida]"; }
    }
    public record Enlace(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) {
        @Override public String toString() { return "Enlace[protegido]"; }
    }
    public record NuevaContrasenia(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
            @NotBlank @Size(min = 7, max = 64) String contrasenia,
            @NotBlank @Size(min = 7, max = 64) String confirmacion) {
        @Override public String toString() { return "NuevaContrasenia[protegida]"; }
    }
    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitar(@Valid @RequestBody Solicitud datos, HttpServletRequest request) {
        limitar("solicitar:" + request.getRemoteAddr(), maximoIp, Duration.ofHours(1));
        if (limites.permitir("correo:" + RestablecimientoService.hash(datos.email()), 3, Duration.ofMinutes(15))) {
            correo.solicitar(datos.email());
        }
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(Map.of("mensaje", MENSAJE));
    }
    @PostMapping("/validar")
    public ResponseEntity<?> validar(@Valid @RequestBody Enlace datos, HttpServletRequest request) {
        limitar("token:" + request.getRemoteAddr(), maximoValidaciones, Duration.ofMinutes(15));
        servicio.validar(datos.token());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("valido", true));
    }
    @PostMapping("/restablecer")
    public ResponseEntity<?> restablecer(@Valid @RequestBody NuevaContrasenia datos, HttpServletRequest request) {
        limitar("token:" + request.getRemoteAddr(), maximoValidaciones, Duration.ofMinutes(15));
        String email = servicio.restablecer(datos.token(), datos.contrasenia(), datos.confirmacion());
        var sesion = request.getSession(false);
        if (sesion != null) sesion.invalidate();
        SecurityContextHolder.clearContext();
        correo.avisarCambio(email);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("mensaje", "Contraseña actualizada. Inicia sesión con tu nueva contraseña."));
    }
    private void limitar(String clave, int maximo, Duration ventana) {
        if (!limites.permitir(clave, maximo, ventana)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Inténtalo de nuevo más tarde.");
        }
    }
}
