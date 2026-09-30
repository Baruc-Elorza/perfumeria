package com.example.demo.service;

import org.springframework.stereotype.Service;
import com.example.demo.model.Perfume;

import java.util.ArrayList;
import java.util.List;

@Service
public class CarritoService {

    public List<Perfume> obtenerCarrito() {
        List<Perfume> lPerfumes = new ArrayList<>();
        lPerfumes.add(new Perfume());
        return lPerfumes;
    }
}