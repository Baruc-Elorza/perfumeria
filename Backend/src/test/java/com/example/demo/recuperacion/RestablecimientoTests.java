package com.example.demo.recuperacion;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:restablecimiento_tests;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "notificaciones.compra.habilitadas=false", "stripe.public.key=", "stripe.secret.key=",
        "app.recuperacion.remitente=tienda@example.test", "app.recuperacion.url-base=http://localhost:4200",
        "app.recuperacion.max-solicitudes-ip=1000", "app.recuperacion.max-validaciones-ip=1000"})
@Import(RestablecimientoTests.Config.class)
class RestablecimientoTests {
    @Value("${local.server.port}") int port;
    @Autowired UsuarioRepository usuarios;
    @Autowired TokenRestablecimientoRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired CorreoCapturado correo;
    @Autowired @Qualifier("executorRecuperacion") ThreadPoolTaskExecutor executor;
    private final JsonMapper json = JsonMapper.builder().build();
    private String email;
    private Long usuarioId;
    private final String anterior = "AnteriorSegura123";
    private final String nueva = "Mi nueva frase segura 123";

    @TestConfiguration
    static class Config {
        @Bean @Primary CorreoCapturado correoCapturado() { return new CorreoCapturado(); }
    }
    static class CorreoCapturado extends JavaMailSenderImpl {
        final List<SimpleMailMessage> mensajes = new CopyOnWriteArrayList<>();
        @Override public void send(SimpleMailMessage mensaje) { mensajes.add(new SimpleMailMessage(mensaje)); }
    }
    @BeforeEach
    void preparar() throws Exception {
        esperarCorreo();
        correo.mensajes.clear();
        tokens.deleteAll();
        email = "prueba-" + UUID.randomUUID() + "@example.test";
        usuarioId = usuarios.save(new Usuario("Cliente", email, encoder.encode(anterior))).getId();
    }
    private HttpClient cliente() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }
    private HttpResponse<String> post(HttpClient cliente, String ruta, Map<String, String> datos) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(datos))).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> solicitar(String correo) throws Exception {
        return post(cliente(), "/api/auth/recuperacion/solicitar", Map.of("email", correo));
    }
    private void esperarCorreo() throws Exception {
        long limite = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (executor.getActiveCount() != 0 || !executor.getThreadPoolExecutor().getQueue().isEmpty()) {
            if (System.nanoTime() > limite) fail("El trabajador de correo no terminó");
            Thread.sleep(10);
        }
    }
    private String enlace() throws Exception {
        assertEquals(202, solicitar(email).statusCode());
        esperarCorreo();
        var mensaje = correo.mensajes.get(correo.mensajes.size() - 1);
        assertEquals(email, mensaje.getTo()[0]);
        assertTrue(mensaje.getText().contains("http://localhost:4200/restablecer-contrasena#token="));
        var coincidencia = Pattern.compile("token=([A-Za-z0-9_-]{43})").matcher(mensaje.getText());
        assertTrue(coincidencia.find());
        return coincidencia.group(1);
    }
    private HttpResponse<String> cambiar(String token, String password, String confirmacion) throws Exception {
        return post(cliente(), "/api/auth/recuperacion/restablecer", Map.of("token", token, "contrasenia", password, "confirmacion", confirmacion));
    }
    @Test
    void mismaRespuestaParaCorreoRegistradoInexistenteYBloqueado() throws Exception {
        var conocida = solicitar("  " + email.toUpperCase(Locale.ROOT) + "  "); esperarCorreo();
        assertEquals(1, correo.mensajes.size());
        var desconocida = solicitar("inexistente@example.test"); esperarCorreo();
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(); usuario.setActivo(false); usuarios.save(usuario);
        var bloqueada = solicitar(email); esperarCorreo();
        assertEquals(202, conocida.statusCode());
        assertEquals(conocida.statusCode(), desconocida.statusCode());
        assertEquals(conocida.body(), desconocida.body());
        assertEquals(conocida.body(), bloqueada.body());
        assertEquals(1, correo.mensajes.size());
        assertFalse(conocida.body().contains("token"));
        assertEquals(400, solicitar("correo-invalido").statusCode());
    }
    @Test
    void cambiaContraseniaUnaSolaVezEInvalidaLasSesionesAnteriores() throws Exception {
        var navegador = cliente();
        assertEquals(200, post(navegador, "/api/auth/login", Map.of("email", email, "contrasenia", anterior)).statusCode());
        String token = enlace();
        var guardado = tokens.findById(usuarioId).orElseThrow();
        assertNotEquals(token, guardado.hashToken);
        assertEquals(64, guardado.hashToken.length());
        assertEquals(200, post(cliente(), "/api/auth/recuperacion/validar", Map.of("token", token)).statusCode());
        assertEquals(200, cambiar(token, nueva, nueva).statusCode()); esperarCorreo();
        var usuario = usuarios.findById(usuarioId).orElseThrow();
        assertTrue(encoder.matches(nueva, usuario.getContrasenia()));
        assertNotEquals(nueva, usuario.getContrasenia());
        assertTrue(tokens.findById(usuarioId).orElseThrow().utilizado);
        assertEquals(400, cambiar(token, nueva, nueva).statusCode());
        var sesion = navegador.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/me")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, sesion.statusCode());
        assertEquals(401, post(cliente(), "/api/auth/login", Map.of("email", email, "contrasenia", anterior)).statusCode());
        assertEquals(200, post(cliente(), "/api/auth/login", Map.of("email", email, "contrasenia", nueva)).statusCode());
        assertEquals(2, correo.mensajes.size());
        assertFalse(correo.mensajes.get(1).getText().contains(nueva));
    }
    @Test
    void admiteSieteCaracteresYRechazaSeis() throws Exception {
        String token = enlace();
        assertEquals(400, cambiar(token, "Clave1", "Clave1").statusCode());
        assertEquals(200, cambiar(token, "Clave12", "Clave12").statusCode());
        esperarCorreo();
        assertTrue(encoder.matches("Clave12", usuarios.findById(usuarioId).orElseThrow().getContrasenia()));
    }

    @Test
    void rechazaEnlacesVencidosInventadosYReemplazados() throws Exception {
        String viejo = enlace();
        String actual = enlace();
        assertNotEquals(viejo, actual);
        assertEquals(400, cambiar(viejo, nueva, nueva).statusCode());
        assertEquals(400, cambiar("A".repeat(43), nueva, nueva).statusCode());
        var registro = tokens.findById(usuarioId).orElseThrow(); registro.venceEn = Instant.now().minusSeconds(1); tokens.save(registro);
        assertEquals(400, cambiar(actual, nueva, nueva).statusCode());
        assertTrue(encoder.matches(anterior, usuarios.findById(usuarioId).orElseThrow().getContrasenia()));
    }
    @Test
    void validaLongitudConfirmacionMismaContraseniaYTamanioBcryptSinConsumirEnlace() throws Exception {
        String token = enlace();
        assertEquals(400, cambiar(token, "corta", "corta").statusCode());
        assertEquals(400, cambiar(token, nueva, "No coincide mi frase").statusCode());
        assertEquals(400, cambiar(token, anterior, anterior).statusCode());
        assertEquals(400, cambiar(token, "é".repeat(40), "é".repeat(40)).statusCode());
        assertEquals(400, cambiar(token, "a".repeat(65), "a".repeat(65)).statusCode());
        assertEquals(400, cambiar(token, "Frase con control\n123", "Frase con control\n123").statusCode());
        assertEquals(200, post(cliente(), "/api/auth/recuperacion/validar", Map.of("token", token)).statusCode());
        assertFalse(tokens.findById(usuarioId).orElseThrow().utilizado);
    }
    @Test
    void unaCuentaBloqueadaOConSesionesRevocadasNoPuedeUsarUnEnlaceAntiguo() throws Exception {
        String token = enlace();
        Usuario usuario = usuarios.findById(usuarioId).orElseThrow(); usuario.setActivo(false); usuarios.save(usuario);
        assertEquals(400, cambiar(token, nueva, nueva).statusCode());
        usuario.setActivo(true); usuario.revocarSesiones(); usuarios.save(usuario);
        assertEquals(400, cambiar(token, nueva, nueva).statusCode());
    }
    @Test
    void dosSolicitudesConcurrentesSoloPuedenUsarElEnlaceUnaVez() throws Exception {
        String token = enlace();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> cambiar(token, nueva, nueva).statusCode());
            var b = pool.submit(() -> cambiar(token, nueva, nueva).statusCode());
            assertEquals(List.of(200, 400), java.util.stream.Stream.of(a.get(), b.get()).sorted().toList());
        }
        esperarCorreo();
    }
    @Test
    void limitaCorreosSinRevelarSiLaCuentaExiste() throws Exception {
        var primera = solicitar(email); solicitar(email); solicitar(email); esperarCorreo();
        assertEquals(3, correo.mensajes.size());
        var cuarta = solicitar(email); esperarCorreo();
        assertEquals(primera.body(), cuarta.body());
        assertEquals(3, correo.mensajes.size());
        var limites = new LimitadorRecuperacion();
        assertTrue(limites.permitir("ip", 2, Duration.ofMinutes(15)));
        assertTrue(limites.permitir("ip", 2, Duration.ofMinutes(15)));
        assertFalse(limites.permitir("ip", 2, Duration.ofMinutes(15)));
        assertTrue(limites.permitir("otra-ip", 2, Duration.ofMinutes(15)));
    }
}
