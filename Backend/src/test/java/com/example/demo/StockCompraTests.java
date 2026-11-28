package com.example.demo;

import com.example.demo.dto.CarritoRespuesta;
import com.example.demo.model.Pedido;
import com.example.demo.model.Perfume;
import com.example.demo.model.Usuario;
import com.example.demo.notificaciones.NotificacionCompraRepository;
import com.example.demo.repository.*;
import com.example.demo.service.CompraService;
import com.example.demo.service.PagoService;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:stock_compras;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "notificaciones.compra.habilitadas=false", "stripe.public.key=", "stripe.secret.key="})
@Import(StockCompraTests.Config.class)
class StockCompraTests {
    @Autowired CompraService compras;
    @Autowired PerfumeRepository perfumes;
    @Autowired PedidoRepository pedidos;
    @Autowired UsuarioRepository usuarios;
    @Autowired NotificacionCompraRepository correos;
    @Autowired PagosPrueba pagos;
    private final String email = "stock@example.test";
    private Long perfumeId;

    @TestConfiguration
    static class Config {
        @Bean @Primary PagosPrueba pagosPrueba() { return new PagosPrueba(); }
    }
    static class PagosPrueba extends PagoService {
        final Map<String, PaymentIntent> pagos = new ConcurrentHashMap<>();
        @Override public PaymentIntent obtenerPago(String id) { return pagos.get(id); }
    }
    @BeforeEach
    void preparar() {
        correos.deleteAll(); pedidos.deleteAll(); perfumes.deleteAll(); pagos.pagos.clear();
        if (!usuarios.existsByEmail(email)) usuarios.save(new Usuario("Cliente prueba", email, "hash_prueba"));
        perfumeId = perfume("Eros", 3);
    }
    private Long perfume(String nombre, int stock) {
        Perfume p = new Perfume(); p.setNombre(nombre); p.setMarca("Versace"); p.setStock(stock); p.setPrecio(100.0);
        return perfumes.save(p).getId();
    }
    private CarritoRespuesta.Linea linea(Long id, int cantidad) {
        return new CarritoRespuesta.Linea(id, "Perfume", "Versace", "", new BigDecimal("100.00"), 3, cantidad);
    }
    private Pedido pedido(CarritoRespuesta.Linea... productos) {
        BigDecimal subtotal = java.util.Arrays.stream(productos)
                .map(p -> p.precio().multiply(BigDecimal.valueOf(p.cantidad()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        Pedido pedido = compras.crearPedidoPendiente(email, "Calle 10", "Centro", "Oaxaca", "68000", "9511234567",
                new CarritoRespuesta(List.of(productos), subtotal));
        PaymentIntent pago = new PaymentIntent();
        pago.setId("pi_stock_" + pedido.getId()); pago.setStatus("succeeded"); pago.setCurrency("mxn");
        pago.setAmount(subtotal.movePointRight(2).longValueExact()); pago.setLivemode(false);
        pago.setMetadata(Map.of("pedidoId", pedido.getId().toString()));
        pagos.pagos.put(pago.getId(), pago);
        return pedido;
    }
    private void confirmar(Pedido pedido) throws Exception {
        compras.confirmarCompraYPago(pedido.getId(), "pi_stock_" + pedido.getId(), email);
    }
    private int stock(Long id) { return perfumes.findById(id).orElseThrow().getStock(); }

    @Test
    void tresExistenciasMenosUnaCompraDejaDosSinRepetirElDescuento() throws Exception {
        Pedido p = pedido(linea(perfumeId, 1));
        assertEquals(3, stock(perfumeId));
        confirmar(p);
        assertEquals(2, stock(perfumeId));
        assertEquals(Pedido.EstadoPedido.PAGADO, pedidos.findById(p.getId()).orElseThrow().getEstado());
        assertEquals(2, correos.count());
        confirmar(p);
        assertEquals(2, stock(perfumeId));
        assertEquals(2, correos.count());
    }
    @Test
    void noDescuentaPagosPendientesNiPedidosCancelados() throws Exception {
        Pedido p = pedido(linea(perfumeId, 1));
        pagos.pagos.get("pi_stock_" + p.getId()).setStatus("requires_payment_method");
        assertThrows(IllegalStateException.class, () -> confirmar(p));
        assertEquals(3, stock(perfumeId));
        pagos.pagos.get("pi_stock_" + p.getId()).setStatus("succeeded");
        p.setEstado(Pedido.EstadoPedido.CANCELADO); pedidos.save(p);
        assertThrows(IllegalStateException.class, () -> confirmar(p));
        assertEquals(3, stock(perfumeId));
        assertEquals(0, correos.count());
    }
    @Test
    void faltaDeStockRevierteTodosLosProductosYElPedido() throws Exception {
        Long agotado = perfume("Agotado", 1);
        Pedido p = pedido(linea(perfumeId, 1), linea(agotado, 1));
        Perfume productoAgotado = perfumes.findById(agotado).orElseThrow();
        productoAgotado.setStock(0); perfumes.save(productoAgotado);
        assertThrows(IllegalStateException.class, () -> confirmar(p));
        assertEquals(3, stock(perfumeId)); assertEquals(0, stock(agotado));
        assertEquals(Pedido.EstadoPedido.PENDIENTE, pedidos.findById(p.getId()).orElseThrow().getEstado());
        assertEquals(0, correos.count());
    }
    @Test
    void sumaLasUnidadesDeUnMismoPerfumeEnVariasLineas() throws Exception {
        Pedido p = pedido(linea(perfumeId, 1), linea(perfumeId, 1));
        confirmar(p); assertEquals(1, stock(perfumeId));
    }
    @Test
    void dosConfirmacionesSimultaneasDelMismoPedidoSoloDescuentanUnaVez() throws Exception {
        Pedido p = pedido(linea(perfumeId, 1));
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> { confirmar(p); return true; });
            var b = pool.submit(() -> { confirmar(p); return true; });
            assertTrue(a.get()); assertTrue(b.get());
        }
        assertEquals(2, stock(perfumeId)); assertEquals(2, correos.count());
    }
    @Test
    void dosComprasSimultaneasDeLaUltimaUnidadNoDejanStockNegativo() throws Exception {
        Perfume producto = perfumes.findById(perfumeId).orElseThrow(); producto.setStock(1); perfumes.save(producto);
        Pedido uno = pedido(linea(perfumeId, 1)); Pedido dos = pedido(linea(perfumeId, 1));
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> { try { confirmar(uno); return true; } catch (IllegalStateException ex) { return false; } });
            var b = pool.submit(() -> { try { confirmar(dos); return true; } catch (IllegalStateException ex) { return false; } });
            assertNotEquals(a.get(), b.get());
        }
        assertEquals(0, stock(perfumeId)); assertEquals(2, correos.count());
        assertEquals(1, pedidos.findAll().stream().filter(p -> p.getEstado() == Pedido.EstadoPedido.PAGADO).count());
    }
    @Test
    void falloAlRegistrarLaNotificacionTambienRevierteElStock() throws Exception {
        Pedido p = pedido(linea(perfumeId, 1));
        p.setSubtotal(BigDecimal.ZERO); pedidos.save(p);
        assertThrows(IllegalArgumentException.class, () -> confirmar(p));
        assertEquals(3, stock(perfumeId));
        assertEquals(Pedido.EstadoPedido.PENDIENTE, pedidos.findById(p.getId()).orElseThrow().getEstado());
        assertEquals(0, correos.count());
    }
}
