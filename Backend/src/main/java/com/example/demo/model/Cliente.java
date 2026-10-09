package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
@Entity
@Table(name="cliente")
public class Cliente extends Usuario{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Cliente(String nombre, String email, String contraseña){
        super(nombre, email, contraseña);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
}
