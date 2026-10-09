package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name="usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio") 
    @Column(nullable = false)
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Column(nullable = false)
    private String email;

    @NotBlank(message = "La contraseña es obligtoria")
    @Column(nullable = false)
    private String contraseña;

    public Usuario(){}

    public Usuario(String nombre, String email, String contraseña){
        this.nombre = nombre;
        this.email = email;
        this.contraseña = contraseña;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
