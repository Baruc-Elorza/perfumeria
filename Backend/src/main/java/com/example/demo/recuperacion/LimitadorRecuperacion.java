package com.example.demo.recuperacion;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Límites para la instancia local; nunca se usa la existencia de una cuenta como criterio. */
@Component
public class LimitadorRecuperacion {
    private final Map<String, Ventana> ventanas = new HashMap<>();
    private static class Ventana {
        Instant vence;
        int solicitudes;
        Ventana(Instant vence) { this.vence = vence; }
    }
    public synchronized boolean permitir(String clave, int maximo, Duration duracion) {
        Instant ahora = Instant.now();
        ventanas.entrySet().removeIf(e -> !e.getValue().vence.isAfter(ahora));
        Ventana ventana = ventanas.get(clave);
        if (ventana == null) {
            if (ventanas.size() >= 20000) return false;
            ventana = new Ventana(ahora.plus(duracion));
            ventanas.put(clave, ventana);
        }
        if (ventana.solicitudes >= maximo) return false;
        ventana.solicitudes++;
        return true;
    }
}
