package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public record CarritoRespuesta(List<Linea> items, BigDecimal subtotal) {
    public record Linea(Long perfumeId, String nombre, String marca, String imagenUrl,
                        BigDecimal precio, int stock, int cantidad) {}
}
