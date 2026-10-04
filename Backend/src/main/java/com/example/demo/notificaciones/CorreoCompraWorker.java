package com.example.demo.notificaciones;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@ConditionalOnProperty(name = "notificaciones.compra.habilitadas", havingValue = "true")
public class CorreoCompraWorker {
    private static final Logger log = LoggerFactory.getLogger(CorreoCompraWorker.class);
    private final NotificacionCompraRepository repository;
    private final JavaMailSender mail;
    private final String destinatario;
    private final String remitente;

    public CorreoCompraWorker(NotificacionCompraRepository repository, JavaMailSender mail,
            @Value("${notificaciones.compra.destinatario}") String destinatario,
            @Value("${notificaciones.compra.remitente}") String remitente) {
        this.repository = repository;
        this.mail = mail;
        this.destinatario = direccion(destinatario);
        this.remitente = direccion(remitente);
    }

    private String direccion(String valor) {
        try {
            if (valor == null || valor.isBlank() || valor.contains("\r") || valor.contains("\n")) {
                throw new AddressException();
            }
            InternetAddress address = new InternetAddress(valor, true);
            address.validate();
            return address.getAddress();
        } catch (AddressException ex) {
            throw new IllegalArgumentException("Configura COMPRA_EMAIL_ADMIN y COMPRA_EMAIL_FROM con correos válidos.");
        }
    }

    @Transactional
    public void enviar(Long id) {
        var n = repository.bloquear(id).orElse(null);
        if (n == null || !"PENDIENTE".equals(n.estado) || n.proximoIntento.isAfter(Instant.now())) return;
        if (n.destinatario == null) n.destinatario = destinatario;
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(n.destinatario);
        mensaje.setSubject(n.asunto);
        mensaje.setText(n.cuerpo);
        n.intentos++;
        try {
            mail.send(mensaje);
            n.estado = "ENVIADA";
            n.enviadoEn = Instant.now();
            n.ultimoError = null;
            log.info("Notificación de compra {} aceptada por SMTP.", n.id);
        } catch (MailException ex) {
            // No guardar mensajes SMTP que puedan incluir credenciales o datos personales.
            n.ultimoError = ex.getClass().getSimpleName();
            n.estado = n.intentos >= 5 ? "FALLIDA" : "PENDIENTE";
            n.proximoIntento = Instant.now().plusSeconds(60L * n.intentos);
            log.warn("Notificación de compra {}: intento {} falló ({}).", n.id, n.intentos, n.ultimoError);
        }
    }
}
