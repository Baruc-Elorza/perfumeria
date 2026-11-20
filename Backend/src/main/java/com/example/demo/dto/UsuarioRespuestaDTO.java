
package com.example.demo.dto;

import java.math.BigInteger;

public class UsuarioRespuestaDTO {

    private BigInteger id;
    private String nombre;
    private String email;

    public UsuarioRespuestaDTO() {
    }

    public UsuarioRespuestaDTO(BigInteger id, String nombre, String email) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
