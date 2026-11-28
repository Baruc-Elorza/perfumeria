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
    @Column(columnDefinition = "BIGINT")
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", columnDefinition = "BIGINT")
    private Usuario cliente;

    @Column(nullable = false)
    private String clienteNombre;

    @Column(nullable = false)
    private String destinatarioEnvio;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;
    
    private String direccionEnvio; //se obtiene con el constructor cliente.getDireccion();
    private String telefono;

    @Column(length = 150)
    private String paymentIntentId;
    private BigDecimal subtotal;
    private BigDecimal descuento;
    private BigDecimal total;
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @OneToOne
    @JoinColumn(name= "id_envio", nullable = true, columnDefinition = "BIGINT")
    private Envio envio;

    @ElementCollection
    @CollectionTable(name = "pedido_productos", joinColumns = @JoinColumn(name = "pedido_id", columnDefinition = "BIGINT"))
    private List<ItemProducto> productos = new ArrayList<>();

    public enum EstadoPedido {
        PENDIENTE,
        PAGADO,
        RECHAZADO,
        CANCELADO,
        ENVIADO,
        ENTREGADO
    }

    // Clase embebida para representar cada producto dentro del pedido histórico
    @Embeddable
    public static class ItemProducto {
        private Long productoId;
        private String nombre;
        private String marca;
        private Integer cantidad;
        private BigDecimal precioUnitario;

        public ItemProducto() {}

        public ItemProducto(Long productoId, String nombre, String marca, Integer cantidad, BigDecimal precioUnitario) {
            this.productoId = productoId;
            this.nombre = nombre;
            this.marca = marca;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
        }

        public Long getProductoId() { return productoId; }
        public void setProductoId(Long productoId) { this.productoId = productoId; }

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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getDireccionEnvio() { return direccionEnvio; }
    public void setDireccionEnvio(String direccionEnvio) { this.direccionEnvio = direccionEnvio; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getPaymentIntentId() { return paymentIntentId; }
    public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public Envio getEnvio() { return envio; }
    public void setEnvio(Envio envio) { this.envio = envio; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public Usuario getCliente() { return cliente; }
    public void setCliente(Usuario cliente) { this.cliente = cliente; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getDestinatarioEnvio() { return destinatarioEnvio; }
    public void setDestinatarioEnvio(String destinatarioEnvio) { this.destinatarioEnvio = destinatarioEnvio; }

    public BigDecimal getCostoEnvio() { return costoEnvio; }
    public void setCostoEnvio(BigDecimal costoEnvio) { this.costoEnvio = costoEnvio; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public List<ItemProducto> getProductos() { return productos; }
    public void setProductos(List<ItemProducto> productos) { this.productos = productos; }
}
