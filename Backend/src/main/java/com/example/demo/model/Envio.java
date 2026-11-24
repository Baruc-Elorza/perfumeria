package com.example.demo.model;


import java.sql.Date;

import jakarta.persistence.*;

@Entity
@Table(name = "envio")
public class Envio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
//no registramos paquetería porque solo usa DHL siempre
    private String numeroGuia;
    private Date fechaEnvio;
    private double costoEnvio;

    public Envio(){}
    public Envio(String n, Date f, double c){
        numeroGuia=n;
        fechaEnvio=f;
        costoEnvio=c;
    }

    public Long getId(){return id;}

    public String getNumeroGuia(){return numeroGuia;}
    public void setNumeroGuia(String n){numeroGuia=n;}

    public Date getFechaEnvio(){return fechaEnvio;}
    public void setFechaEnvio(Date f){fechaEnvio=f;}

    public double getCostoEnvio(){return costoEnvio;}
    public void setCostoEnvio(Double c){costoEnvio=c;}
}
