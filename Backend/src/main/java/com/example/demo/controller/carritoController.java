package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import com.example.demo.dto.CarritoRespuesta;
import com.example.demo.service.CarritoService;

@RestController 
@RequestMapping("/api/carrito")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")

public class CarritoController {
    private static final String CARRITO_ID = "carritoId";
    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService){
        this.carritoService = carritoService;
    }

    @GetMapping
    public CarritoRespuesta carrito(HttpSession session) {
        return carritoService.obtenerCarrito((Long) session.getAttribute(CARRITO_ID));
    }

    @PostMapping("/items/{perfumeId}")
    public CarritoRespuesta agregar(@PathVariable Long perfumeId, @RequestBody Cantidad body,
                                    HttpSession session) {
        synchronized (session) {
            Long id = (Long) session.getAttribute(CARRITO_ID);
            if (id == null) {
                id = carritoService.crear();
                session.setAttribute(CARRITO_ID, id);
            }
            return carritoService.agregar(id, perfumeId, body.cantidad());
        }
    }

    @PatchMapping("/items/{perfumeId}")
    public CarritoRespuesta cambiarCantidad(@PathVariable Long perfumeId, @RequestBody Cantidad body,
                                            HttpSession session) {
        return carritoService.cambiarCantidad((Long) session.getAttribute(CARRITO_ID), perfumeId, body.cantidad());
    }

    @DeleteMapping("/items/{perfumeId}")
    public CarritoRespuesta eliminar(@PathVariable Long perfumeId, HttpSession session) {
        return carritoService.eliminar((Long) session.getAttribute(CARRITO_ID), perfumeId);
    }

    public record Cantidad(Integer cantidad) {}
}
