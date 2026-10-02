package com.example.demo.repository;

import com.example.demo.model.CarritoPerfume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarritoPerfumeRepository extends JpaRepository<CarritoPerfume, Long> {
    boolean existsByPerfumeId(Long perfumeId);
    List<CarritoPerfume> findByCarritoIdOrderByIdAsc(Long carritoId);
}
