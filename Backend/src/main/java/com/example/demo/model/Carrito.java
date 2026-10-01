package com.example.demo.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity 
@Table(name = "carrito")
public class Carrito {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double subtotal;

    @OneToMany(mappedBy = "carrito")
    private List<CarritoPerfume> perfumes = new ArrayList<>();

    public Carrito(){}

    public Carrito(double subtotal){
        this.subtotal=subtotal;
    }

    public Long getId(){return id;}
    
    public double getSubtotal(){return subtotal;}
    public void setSubtotal(double subtotal){this.subtotal=subtotal;}
}
