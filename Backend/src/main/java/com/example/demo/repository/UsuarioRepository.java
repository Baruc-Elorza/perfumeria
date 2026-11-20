package com.example.demo.repository;

import java.util.Optional;
import java.math.BigInteger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Usuario;

@Repository 
public interface UsuarioRepository extends JpaRepository<Usuario, BigInteger>{
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> bloquearPorId(@org.springframework.data.repository.query.Param("id") BigInteger id);
    Optional<Usuario> findByEmail(String email); 
    boolean existsByEmail(String email);
}
