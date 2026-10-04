package com.example.demo.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Instant;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "notificaciones.compra.habilitadas", havingValue = "true")
public class CorreoCompraScheduler {
    private static final Logger log = LoggerFactory.getLogger(CorreoCompraScheduler.class);
    private final NotificacionCompraRepository repository;
    private final CorreoCompraWorker worker;

    public CorreoCompraScheduler(NotificacionCompraRepository repository, CorreoCompraWorker worker) {
        this.repository = repository;
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${notificaciones.compra.intervalo-ms:30000}")
    public void procesar() {
        for (Long id : repository.pendientes(Instant.now(), PageRequest.of(0, 10))) {
            try {
                worker.enviar(id);
            } catch (RuntimeException ex) {
                log.error("No se pudo procesar la notificación {} ({}).", id, ex.getClass().getSimpleName());
            }
        }
    }
}
