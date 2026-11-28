package com.example.demo.recuperacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import java.net.URI;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CorreoRecuperacion {
    private static final Logger log = LoggerFactory.getLogger(CorreoRecuperacion.class);
    private final RestablecimientoService restablecimiento;
    private final JavaMailSender mail;
    private final ThreadPoolTaskExecutor executor;
    private final String base;
    private final String remitente;

    public CorreoRecuperacion(RestablecimientoService restablecimiento, JavaMailSender mail,
            @Qualifier("executorRecuperacion") ThreadPoolTaskExecutor executor,
            @Value("${app.recuperacion.url-base:http://localhost:4200}") String base,
            @Value("${app.recuperacion.remitente:${notificaciones.compra.remitente:}}") String remitente) {
        this.restablecimiento = restablecimiento;
        this.mail = mail;
        this.executor = executor;
        URI uri = URI.create(base);
        boolean local = "localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && local))) {
            throw new IllegalArgumentException("Configura una URL pública HTTPS; HTTP solo se admite en localhost.");
        }
        this.base = base.replaceAll("/+$", "");
        this.remitente = remitente;
    }

    public void solicitar(String email) {
        ejecutar(() -> restablecimiento.generar(email).ifPresent(datos -> enviar(datos.email(),
                "Restablecer tu contraseña - Perfumería",
                "Recibimos una solicitud para restablecer tu contraseña.\n\n"
                        + "Abre este enlace para elegir una nueva:\n" + base + "/restablecer-contrasena#token=" + datos.token()
                        + "\n\nEl enlace vence en 15 minutos, solo puede usarse una vez y reemplaza los enlaces anteriores."
                        + "\nSi no solicitaste este cambio, ignora este correo. Tu contraseña no se ha modificado.")));
    }
    public void avisarCambio(String email) {
        try {
            ejecutar(() -> enviar(email, "Tu contraseña fue actualizada - Perfumería",
                    "Tu contraseña de Perfumería fue restablecida y cerramos las sesiones anteriores.\n"
                            + "Si no realizaste este cambio, solicita un nuevo enlace de recuperación y contacta al administrador."));
        } catch (ResponseStatusException ex) { log.warn("No se pudo programar el aviso de cambio de contraseña."); }
    }
    private void ejecutar(Runnable trabajo) {
        try {
            executor.execute(() -> {
                try { trabajo.run(); }
                catch (RuntimeException ex) { log.warn("Falló una operación de recuperación de contraseña ({}).", ex.getClass().getSimpleName()); }
            });
        } catch (RejectedExecutionException ex) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Hay demasiadas solicitudes. Inténtalo más tarde.");
        }
    }
    private void enviar(String email, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(email);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        mail.send(mensaje);
        log.info("Correo de recuperación aceptado por SMTP.");
    }

    @Configuration
    static class ExecutorConfig {
        @Bean("executorRecuperacion")
        ThreadPoolTaskExecutor executorRecuperacion() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(2);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("recuperacion-");
            return executor;
        }
    }
}
