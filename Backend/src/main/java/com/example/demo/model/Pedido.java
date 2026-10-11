package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;
    @Column(name = "cliente_email")
    private String clienteEmail;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    private String clienteNombre;
    private String destinatarioEnvio;
    private String direccionEnvio;
    private String telefono;

    private BigDecimal subtotal;
    private BigDecimal envio;
    private BigDecimal descuento;
    private BigDecimal total;

    @ElementCollection
    @CollectionTable(name = "pedido_productos", joinColumns = @JoinColumn(name = "pedido_id"))
    private List<ItemProducto> productos = new ArrayList<>();

    public enum EstadoPedido {
        PENDIENTE,
        PAGADO,
        RECHAZADO,
        CANCELADO
    }

    // Clase embebida para representar cada producto dentro del pedido histórico
    @Embeddable
    public static class ItemProducto {
        private String productoId;
        private String nombre;
        private String marca;
        private Integer cantidad;
        private BigDecimal precioUnitario;

        public ItemProducto() {}

        public ItemProducto(String productoId, String nombre, String marca, Integer cantidad, BigDecimal precioUnitario) {
            this.productoId = productoId;
            this.nombre = nombre;
            this.marca = marca;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
        }

        public String getProductoId() { return productoId; }
        public void setProductoId(String productoId) { this.productoId = productoId; }

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }

        public String getMarca() { return marca; }
        public void setMarca(String marca) { this.marca = marca; }

        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

        public BigDecimal getPrecioUnitario() { return precioUnitario; }
        public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    }

    // Constructor vacío requerido por JPA
    public Pedido() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public String getClienteEmail() { return clienteEmail; }
    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getDestinatarioEnvio() { return destinatarioEnvio; }
    public void setDestinatarioEnvio(String destinatarioEnvio) { this.destinatarioEnvio = destinatarioEnvio; }

    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getEnvio() { return envio; }
    public void setEnvio(BigDecimal envio) { this.envio = envio; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public List<ItemProducto> getProductos() { return productos; }
    public void setProductos(List<ItemProducto> productos) { this.productos = productos; }
}