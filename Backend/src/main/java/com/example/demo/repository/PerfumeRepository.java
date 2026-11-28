package com.example.demo.repository;

import com.example.demo.model.Perfume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface PerfumeRepository extends JpaRepository<Perfume, Long> {
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update Perfume p set p.stock = p.stock - :cantidad where p.id = :id and p.stock >= :cantidad")
    int descontarExistencias(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("cantidad") int cantidad);

    List<Perfume> findByNombreContainingIgnoreCase(String nombre);
}
