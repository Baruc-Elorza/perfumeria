package com.example.demo.model;

import jakarta.persistence.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Entity 
@Table(name = "carrito")
public class Carrito {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.BIGINT)
    private BigInteger id;

    @OneToOne
    @JoinColumn(name = "cliente_id", unique = true) //un carrito corresponde a un solo cliente
    private Cliente cliente;

    @Column(nullable = false)
    private double subtotal;

    @OneToMany(mappedBy = "carrito")
    private List<CarritoPerfume> perfumes = new ArrayList<>();

    public Carrito(){}

    public Carrito(double subtotal){
        this.subtotal=subtotal;
    }

    public BigInteger getId(){return id;}
    
    public double getSubtotal(){return subtotal;}
    public void setSubtotal(double subtotal){this.subtotal=subtotal;}
}
