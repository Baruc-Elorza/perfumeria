package com.example.demo.notificaciones;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificacionCompraRepository extends JpaRepository<NotificacionCompra, Long> {
    Optional<NotificacionCompra> findByPedidoId(String pedidoId);

    @Query("select n.id from NotificacionCompra n where n.estado = 'PENDIENTE' and n.proximoIntento <= :ahora order by n.id")
    List<Long> pendientes(@Param("ahora") Instant ahora, Pageable pagina);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NotificacionCompra n where n.id = :id")
    Optional<NotificacionCompra> bloquear(@Param("id") Long id);
}
