package com.example.demo.repository;

import com.example.demo.model.Perfume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigInteger;

@Repository
public interface PerfumeRepository extends JpaRepository<Perfume, BigInteger> {
    List<Perfume> findByNombreContainingIgnoreCase(String nombre);
}
