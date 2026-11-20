package com.example.demo.dto;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public record CarritoRespuesta(List<Linea> items, BigDecimal subtotal) {
    public record Linea(BigInteger perfumeId, String nombre, String marca, String imagenUrl,
                        BigDecimal precio, int stock, int cantidad) {}
}
