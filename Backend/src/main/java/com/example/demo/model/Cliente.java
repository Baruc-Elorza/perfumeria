package com.example.demo.model;

import java.math.BigInteger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;

@Entity
@Table(name="Cliente")
@PrimaryKeyJoinColumn(name="Usuario_id")
public class Cliente extends Usuario {
    private String telefono;

    @OneToMany
    private BigInteger idDireccion;

    @OneToOne
    private BigInteger idHistorial;

    public Cliente() {
        super();
    }

    public Cliente(String nombre, String email, String contrasenia) {
        super(nombre, email, contrasenia);
    }

    //faltan setters y getters
}