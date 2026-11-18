package com.example.demo.model;

import java.math.BigInteger;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name="marca")
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private BigInteger id;

    private String nombre;

    @OneToMany(mappedBy = "marca")
    private List<Perfume>perfumes;

    public Marca(){}
    public Marca(String n){
        nombre=n;
    }

    public BigInteger getId(){return id;}
    public void setId(BigInteger id){this.id=id;}

    public String getNombre(){return nombre;}
    public void setNombre(String nombre){this.nombre=nombre;}
}