package com.example.demo.repository;

import java.util.Optional;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Usuario;

@Repository 
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> bloquearPorId(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("select u.id from Usuario u where u.email = :email")
    Optional<Long> buscarIdPorEmail(@org.springframework.data.repository.query.Param("email") String email);
    Optional<Usuario> findByEmail(String email); 
    boolean existsByEmail(String email);
}
