package com.example.demo.repository;

import com.example.demo.model.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigInteger;

public interface MarcaRepository extends JpaRepository<Marca, BigInteger> {
}
