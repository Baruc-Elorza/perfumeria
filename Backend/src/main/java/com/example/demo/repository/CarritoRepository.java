package com.example.demo.repository;

import com.example.demo.model.Carrito;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Carrito c where c.id = :id")
    Optional<Carrito> buscarParaActualizar(@Param("id") Long id);
}
