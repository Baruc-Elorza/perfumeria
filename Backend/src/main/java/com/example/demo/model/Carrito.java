package com.example.demo.model;

import jakarta.persistence.*;

@Entity 
@Table(name = "carrito")
public class Carrito {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double total;

    public Carrito(){}

    public Carrito(double total){
        this.total=total;
    }

    public Long getId(){return id;}
    
    public double getTotal(){return total;}
    public void setTotal(double total){this.total=total;}
}
