package com.example.demo.repository;

import com.example.demo.model.Marca;
import org.springframework.data.jpa.repository.JpaRepository;



public interface MarcaRepository extends JpaRepository<Marca, Long> {
}
