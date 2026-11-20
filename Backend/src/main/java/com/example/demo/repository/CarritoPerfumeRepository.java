package com.example.demo.repository;

import com.example.demo.model.CarritoPerfume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.math.BigInteger;

public interface CarritoPerfumeRepository extends JpaRepository<CarritoPerfume, BigInteger> {
    boolean existsByPerfumeId(BigInteger perfumeId);
    List<CarritoPerfume> findByCarritoIdOrderByIdAsc(BigInteger carritoId);
}
