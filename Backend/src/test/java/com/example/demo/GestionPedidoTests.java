package com.example.demo;

import com.example.demo.dto.CarritoRespuesta;
import com.example.demo.model.Pedido;
import com.example.demo.model.Usuario;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.CompraService;
import com.example.demo.service.GestionPedidoService;
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
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gestion_pedidos;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "notificaciones.compra.habilitadas=false", "stripe.public.key=", "stripe.secret.key="})
@Import(GestionPedidoTests.Config.class)
class GestionPedidoTests {
    @Autowired GestionPedidoService gestion;
    @Autowired CompraService compras;
    @Autowired PedidoRepository pedidos;
    @Autowired UsuarioRepository usuarios;
    @Autowired com.example.demo.repository.PerfumeRepository perfumes;
    private Long perfumeId;
    @Autowired PagoPrueba pagos;
    private final String email = "comprador@example.test";

    @TestConfiguration
    static class Config {
        @Bean @Primary PagoPrueba pagoPrueba() { return new PagoPrueba(); }
    }
    static class PagoPrueba extends PagoService {
        int creados;
        int cancelados;
        boolean fallar;
        PaymentIntent intent;
        @Override public PaymentIntent crearPago(BigDecimal monto, String id) {
            creados++;
            intent = new PaymentIntent();
            intent.setId("pi_prueba_" + id);
            intent.setClientSecret("secreto_ficticio");
            intent.setStatus("requires_payment_method");
            return intent;
        }
        @Override public PaymentIntent obtenerPago(String id) { return intent; }
        @Override public void cancelarPago(String id) {
            if (fallar) throw new IllegalStateException("Fallo de conexión simulado");
            cancelados++;
            intent.setStatus("canceled");
        }
    }
    @BeforeEach
    void preparar() {
        pedidos.deleteAll();
        perfumes.deleteAll();
        var perfume = new com.example.demo.model.Perfume();
        perfume.setNombre("Eros"); perfume.setMarca("Versace"); perfume.setPrecio(1500.0); perfume.setStock(3);
        perfumeId = perfumes.save(perfume).getId();
        if (!usuarios.existsByEmail(email)) usuarios.save(new Usuario("Comprador", email, "hash_prueba"));
        pagos.creados = 0; pagos.cancelados = 0; pagos.fallar = false;
    }
    Pedido pedido() {
        return compras.crearPedidoPendiente(email, "Calle 10", "Centro", "Oaxaca", "68000", "9511234567",
                new CarritoRespuesta(List.of(new CarritoRespuesta.Linea(perfumeId, "Eros", "Versace", "",
                        new BigDecimal("1500.00"), 3, 2)), new BigDecimal("3000.00")));
    }
    @Test
    void rechazaCrearPedidoSobreStockEIniciarPagoSiLasExistenciasBajaron() throws Exception {
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> compras.crearPedidoPendiente(
                email, "Calle", "Centro", "Oaxaca", "68000", "9511234567",
                new CarritoRespuesta(List.of(new CarritoRespuesta.Linea(perfumeId, "Eros", "Versace", "",
                        new BigDecimal("1500.00"), 3, 4)), new BigDecimal("6000.00")))).getStatusCode().value());
        Pedido p = pedido();
        var perfume = perfumes.findById(perfumeId).orElseThrow(); perfume.setStock(1); perfumes.save(perfume);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> gestion.iniciarPago(p.getId(), email)).getStatusCode().value());
        assertEquals(0, pagos.creados);
    }
    @Test
    void rechazaRetomarPagoCuandoElStockYaNoAlcanza() throws Exception {
        Pedido p = pedido(); gestion.iniciarPago(p.getId(), email);
        var perfume = perfumes.findById(perfumeId).orElseThrow(); perfume.setStock(0); perfumes.save(perfume);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> gestion.iniciarPago(p.getId(), email)).getStatusCode().value());
        assertEquals(1, pagos.creados);
    }

    @Test
    void reanudarReutilizaElPagoYCancelarInvalidaElIntento() throws Exception {
        Pedido p = pedido();
        var primero = gestion.iniciarPago(p.getId(), email);
        var segundo = gestion.iniciarPago(p.getId(), email);
        assertEquals(primero.paymentIntentId(), segundo.paymentIntentId());
        assertEquals(1, pagos.creados);
        assertEquals(primero.paymentIntentId(), pedidos.findById(p.getId()).orElseThrow().getPaymentIntentId());
        gestion.cancelar(p.getId(), email);
        assertEquals(1, pagos.cancelados);
        assertEquals(Pedido.EstadoPedido.CANCELADO, pedidos.findById(p.getId()).orElseThrow().getEstado());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> gestion.iniciarPago(p.getId(), email)).getStatusCode().value());
    }
    @Test
    void cancelaPendienteSinHaberAbiertoStripe() throws Exception {
        Pedido p = pedido();
        gestion.cancelar(p.getId(), email);
        assertEquals(Pedido.EstadoPedido.CANCELADO, pedidos.findById(p.getId()).orElseThrow().getEstado());
        assertEquals(0, pagos.creados);
        assertEquals(0, pagos.cancelados);
    }
    @Test
    void bloqueaOtrasCuentasYPedidosQueYaNoSonPendientes() throws Exception {
        Pedido p = pedido();
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> gestion.cancelar(p.getId(), "otro@example.test")).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> gestion.iniciarPago(p.getId(), "otro@example.test")).getStatusCode().value());
        p.setEstado(Pedido.EstadoPedido.PAGADO); pedidos.save(p);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> gestion.cancelar(p.getId(), email)).getStatusCode().value());
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> gestion.iniciarPago(p.getId(), email)).getStatusCode().value());
    }
    @Test
    void noCancelaSiStripeEstaProcesandoElPagoOFalla() throws Exception {
        Pedido p = pedido(); gestion.iniciarPago(p.getId(), email);
        for (String estado : List.of("processing", "succeeded", "requires_capture")) {
            pagos.intent.setStatus(estado);
            assertEquals(409, assertThrows(ResponseStatusException.class, () -> gestion.cancelar(p.getId(), email)).getStatusCode().value());
        }
        pagos.intent.setStatus("requires_payment_method"); pagos.fallar = true;
        assertThrows(IllegalStateException.class, () -> gestion.cancelar(p.getId(), email));
        assertEquals(Pedido.EstadoPedido.PENDIENTE, pedidos.findById(p.getId()).orElseThrow().getEstado());
    }
}
