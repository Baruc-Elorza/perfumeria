package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*; //Para validaciones de H.U.-02

@Entity
@Table(name = "perfume")
public class Perfume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio") 
    @Column(nullable = false)
    private String nombre;

    @NotBlank(message = "La marca es obligatoria") 
    @Column(nullable = false)
    private String marca;

    @NotBlank(message = "La descripción es obligatoria") 
    @Column(length = 500)
    private String descripcion;

    @Column(length = 500)
    private String notasTop; // Para H.U.-09
    
    private String notasMiddle;
    private String notasBase;

    @NotNull(message = "El precio es obligatorio") 
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0") 
    private Double precio;

    @NotNull(message = "El stock es obligatorio") 
    @Min(value = 0, message = "El stock no puede ser negativo") 
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

    public Perfume(String nombre, String marca, String descripcion, String notasTop, String notasMiddle, String notasBase, Double precio, Integer stock, String imagenUrl) {
        this.nombre = nombre;
        this.marca = marca;
        this.descripcion = descripcion;
        this.notasTop = notasTop;
        this.notasMiddle = notasMiddle;
        this.notasBase = notasBase;
        this.precio = precio;
        this.stock = stock;
        this.imagenUrl = imagenUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; } // Util para Jackson/JPA

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getNotasTop() { return notasTop; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
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