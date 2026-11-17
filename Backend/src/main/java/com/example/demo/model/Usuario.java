package com.example.demo.model;

import java.math.BigInteger;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name="usuario")
@Inheritance(strategy = InheritanceType.JOINED)
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private BigInteger id;

    @NotBlank(message = "El nombre es obligatorio") 
    @Column(nullable = false)
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Column(nullable = false)
    private String email;

    @NotBlank(message = "La contraseña es obligtoria")
    private String contrasenia;

    private boolean activo;

    public Usuario(){}

    public Usuario(String nombre, String email, String contrasenia){
        this.nombre = nombre;
        this.email = email;
        this.contrasenia = contrasenia;
        this.activo = true;
    }

    public BigInteger getId() { return id; }
    public void setId(BigInteger id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContrasenia() { return contrasenia; }
    public void setContrasenia(String contrasenia) { this.contrasenia = contrasenia; }

    public boolean getActivo(){ return activo;}
    public void setActivo(boolean activo){this.activo=activo;}
}
