package com.example.demo.model;

import java.math.BigInteger;

import jakarta.persistence.*;

@Entity
@Table(name="marca")
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.BIGINT)
    private BigInteger id;

    private String nombre;


    public Marca(){}
    public Marca(String n){
        nombre=n;
    }

    public BigInteger getId(){return id;}
    public void setId(BigInteger id){this.id=id;}

    public String getNombre(){return nombre;}
    public void setNombre(String nombre){this.nombre=nombre;}
}