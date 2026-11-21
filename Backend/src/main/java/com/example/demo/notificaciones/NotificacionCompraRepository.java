package com.example.demo.notificaciones;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.math.BigInteger;

public interface NotificacionCompraRepository extends JpaRepository<NotificacionCompra, BigInteger> {
    List<NotificacionCompra> findAllByPedidoId(String pedidoId);

    @Query("select n.id from NotificacionCompra n where n.estado = 'PENDIENTE' and n.proximoIntento <= :ahora order by n.id")
    List<BigInteger> pendientes(@Param("ahora") Instant ahora, Pageable pagina);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NotificacionCompra n where n.id = :id")
    Optional<NotificacionCompra> bloquear(@Param("id") BigInteger id);
}
