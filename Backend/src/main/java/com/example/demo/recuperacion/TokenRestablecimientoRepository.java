package com.example.demo.recuperacion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TokenRestablecimientoRepository extends JpaRepository<TokenRestablecimiento, Long> {
    Optional<TokenRestablecimiento> findByHashToken(String hashToken);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TokenRestablecimiento t where t.hashToken = :hash")
    Optional<TokenRestablecimiento> bloquear(@Param("hash") String hash);
}
