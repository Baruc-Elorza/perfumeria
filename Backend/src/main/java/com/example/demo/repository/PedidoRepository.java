package com.example.demo.repository;

import com.example.demo.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;


@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteEmailOrderByFechaCreacionDesc(String clienteEmail);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Pedido p where p.id = :id and p.cliente.email = :email")
    Optional<Pedido> bloquearPorIdYClienteEmail(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("email") String email);

    Optional<Pedido> findByIdAndClienteEmail(Long id, String clienteEmail);

    List<Pedido> findByEstadoInOrderByFechaCreacionDesc(Collection<Pedido.EstadoPedido> estados);
}
