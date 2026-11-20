package com.example.demo.repository;

import com.example.demo.model.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigInteger;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, BigInteger> {
    
    // Método para buscar todas las direcciones registradas de un usuario específico
    List<Direccion> findByUsuarioId(BigInteger usuarioId);
}
