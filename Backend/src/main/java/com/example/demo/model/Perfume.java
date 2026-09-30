package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "perfumes")
public class Perfume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String marca;

    @Column(length = 500)
    private String notasTop; // Requerido para H.U.-09
    private String notasMiddle;
    private String notasBase;
    private Double precio;
    private Integer stock;
    private String imagenUrl;

    public Perfume() {}

    public Perfume(String nombre, String marca, String notasTop, String notasMiddle, String notasBase, Double precio, Integer stock, String imagenUrl) {
        this.nombre = nombre;
        this.marca = marca;
        this.notasTop = notasTop;
        this.notasMiddle = notasMiddle;
        this.notasBase = notasBase;
        this.precio = precio;
        this.stock = stock;
        this.imagenUrl = imagenUrl;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getNotasTop() { return notasTop; }
    public void setNotasTop(String notasTop) { this.notasTop = notasTop; }

    public String getNotasMiddle() { return notasMiddle; }
    public void setNotasMiddle(String notasMiddle) { this.notasMiddle = notasMiddle; }

    public String getNotasBase() { return notasBase; }
    public void setNotasBase(String notasBase) { this.notasBase = notasBase; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}