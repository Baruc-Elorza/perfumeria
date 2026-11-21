package com.example.demo.model;

import java.math.BigInteger;

import jakarta.persistence.*;
import jakarta.validation.constraints.*; //Para validaciones de H.U.-02

@Entity
@Table(name = "perfume")
public class Perfume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private BigInteger id;

    @NotBlank(message = "El nombre es obligatorio") 
    @Column(nullable = false)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "id_marca")
    private Marca marca;

    public enum concentracionEnum{
        EDF,
        EDT,
        EDP,
        PARFUM
    }

    @Enumerated(EnumType.STRING)
    private concentracionEnum concentracion;

    @Column(length = 500)
    private String notasTop;
    
    @Column(length = 500)
    private String notasMiddle;

    @Column(length = 500)
    private String notasBase;

    @Column(length = 3)
    private int ml;

    @NotNull(message = "El precio es obligatorio") 
    @DecimalMin(value = "0.01", message = "El precio mínimo es $0.01 MXN")
    private Double precio;

    @NotNull(message = "El stock es obligatorio") 
    @Min(value = 0, message = "El stock debe ser mayor a 0") 
    private Integer stock;

    private String imagenUrl;

    public Perfume() {}

    public Perfume(String nombre, Marca marca, int ml, concentracionEnum concentracion, String notasTop, String notasMiddle,
        String notasBase, Double precio, Integer stock, String imagenUrl) {
        this.nombre = nombre;
        this.marca = marca;
        this.ml = ml;
        this.concentracion = concentracion;
        this.notasTop = notasTop;
        this.notasMiddle = notasMiddle;
        this.notasBase = notasBase;
        this.precio = precio;
        this.stock = stock;
        this.imagenUrl = imagenUrl;
    }

    public BigInteger getId() { return id; }
    public void setId(BigInteger id) { this.id = id; } // Util para Jackson/JPA

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Marca getMarca() { return marca; }
    public void setMarca(Marca marca) { this.marca = marca; }

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
