package com.example.demo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.model.Perfume;
import java.util.List;
import com.example.demo.service.CarritoService;

@RestController 
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")

public class carritoController {
    private final CarritoService carritoService;

    public carritoController(CarritoService carritoService){
        this.carritoService = carritoService;
    }

    @GetMapping("/carrito")
    public List<Perfume> carrito() {
        return carritoService.obtenerCarrito();
    }
}
