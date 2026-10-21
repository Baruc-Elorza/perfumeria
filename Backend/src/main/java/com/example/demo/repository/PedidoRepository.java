package com.example.demo.repository;

import com.example.demo.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, String> {

    List<Pedido> findByClienteEmailOrderByFechaCreacionDesc(String clienteEmail);

    Optional<Pedido> findByIdAndClienteEmail(String id, String clienteEmail);

    List<Pedido> findByEstadoInOrderByFechaCreacionDesc(Collection<Pedido.EstadoPedido> estados);
}
