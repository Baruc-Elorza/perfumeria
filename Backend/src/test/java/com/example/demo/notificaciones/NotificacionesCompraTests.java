package com.example.demo.notificaciones;

import com.example.demo.dto.CarritoRespuesta;
import com.example.demo.model.Pedido;
import com.example.demo.model.Usuario;
import com.example.demo.repository.PedidoRepository;
import com.example.demo.repository.UsuarioRepository;
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
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:correos_tests;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "notificaciones.compra.habilitadas=true",
        "notificaciones.compra.destinatario=admin@example.test",
        "notificaciones.compra.remitente=tienda@example.test",
        "notificaciones.compra.espera-inicial-ms=3600000",
        "stripe.public.key=", "stripe.secret.key="})
@Import(NotificacionesCompraTests.Config.class)
class NotificacionesCompraTests {
    @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;
    @Autowired CompraService compras;
    @Autowired PedidoRepository pedidos;
    @Autowired UsuarioRepository usuarios;
    @Autowired NotificacionCompraRepository notificaciones;
    @Autowired CorreoCompraWorker worker;
    @Autowired PagoPrueba pagos;
    @Autowired CorreoPrueba correo;
    private final String email = "comprador@example.test";

    @TestConfiguration
    static class Config {
        @Bean @Primary PagoPrueba pagoPrueba() { return new PagoPrueba(); }
        @Bean @Primary CorreoPrueba correoPrueba() { return new CorreoPrueba(); }
    }

    static class PagoPrueba extends PagoService {
        PaymentIntent respuesta;
        @Override public PaymentIntent crearPago(BigDecimal monto, String id) {
            respuesta.setClientSecret("secreto_ficticio_para_pruebas");
            return respuesta;
        }
        @Override public PaymentIntent obtenerPago(String id) { return respuesta; }
    }

    static class CorreoPrueba extends JavaMailSenderImpl {
        List<SimpleMailMessage> enviados = new ArrayList<>();
        String fallarPara;
        @Override public void send(SimpleMailMessage mensaje) {
            if (mensaje.getTo()[0].equals(fallarPara)) throw new MailSendException("SMTP de prueba caído");
            enviados.add(new SimpleMailMessage(mensaje));
        }
    }

    @BeforeEach
    void preparar() {
        notificaciones.deleteAll();
        pedidos.deleteAll();
        if (!usuarios.existsByEmail(email)) usuarios.save(new Usuario("Cliente prueba", email, new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("test12345")));
        correo.enviados.clear();
        correo.fallarPara = null;
    }

    private Pedido pedido() {
        CarritoRespuesta carrito = new CarritoRespuesta(List.of(new CarritoRespuesta.Linea(
                BigInteger.ONE, "Eros", "Versace", "", new BigDecimal("1500.00"), 6, 2)),
                new BigDecimal("3000.00"));
        return compras.crearPedidoPendiente(email, "Calle prueba #10", "Centro", "Oaxaca", "68000", "9511234567", carrito);
    }

    private void pago(Pedido pedido) {
        PaymentIntent intent = new PaymentIntent();
        intent.setId("pi_test_" + pedido.getId());
        intent.setStatus("succeeded");
        intent.setCurrency("mxn");
        intent.setAmount(300000L);
        intent.setLivemode(false);
        intent.setMetadata(Map.of("pedidoId", pedido.getId().toString()));
        pagos.respuesta = intent;
    }

    private void confirmar(Pedido pedido) throws Exception {
        compras.confirmarCompraYPago(pedido.getId(), pagos.respuesta.getId(), email);
    }

    @Test
    void crearPagoConsultaElPedidoPorElCorreoDeSuPropietario() throws Exception {
        Pedido pedido = pedido(); pago(pedido);
        var cliente = java.net.http.HttpClient.newBuilder().cookieHandler(
                new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL)).build();
        var login = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"" + email + "\",\"contrasenia\":\"test12345\"}")).build();
        assertEquals(200, cliente.send(login, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
        var iniciar = java.net.http.HttpRequest.newBuilder(java.net.URI.create(
                "http://localhost:" + port + "/api/pagos/crear?pedidoId=" + pedido.getId()))
                .POST(java.net.http.HttpRequest.BodyPublishers.noBody()).build();
        var respuesta = cliente.send(iniciar, java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        assertTrue(respuesta.body().contains("secreto_ficticio_para_pruebas"));
        assertTrue(pedidos.findByIdAndCliente_Email(pedido.getId(), email).isPresent());
        assertTrue(pedidos.findByIdAndCliente_Email(pedido.getId(), "otro@example.test").isEmpty());
        assertEquals(1, pedidos.findByCliente_EmailOrderByFechaCreacionDesc(email).size());
    }

    @Test
    void pagoConfirmadoGeneraDosCorreosIndependientesSinDuplicarlos() throws Exception {
        Pedido pedido = pedido();
        pago(pedido);
        confirmar(pedido);
        confirmar(pedido);
        var pendientes = notificaciones.findAllByPedidoId(pedido.getId());
        assertEquals(2, pendientes.size());
        pendientes.forEach(n -> worker.enviar(n.id));
        pendientes.forEach(n -> worker.enviar(n.id));
        assertEquals(2, correo.enviados.size());
        assertEquals(java.util.Set.of(email, "admin@example.test"), correo.enviados.stream()
                .map(m -> m.getTo()[0]).collect(java.util.stream.Collectors.toSet()));
        for (var mensaje : correo.enviados) {
            assertEquals(1, mensaje.getTo().length);
            assertEquals("tienda@example.test", mensaje.getFrom());
            assertTrue(mensaje.getSubject().contains("[PRUEBA]"));
            for (String dato : List.of("Cliente prueba", email, "Eros", "Versace", "Cantidad: 2", "$3000.00 MXN",
                    "Calle prueba #10", "Centro", "Oaxaca", "68000", "9511234567", "cargo real")) {
                assertTrue(mensaje.getText().contains(dato), dato);
            }
        }
        assertTrue(notificaciones.findAllByPedidoId(pedido.getId()).stream().allMatch(n -> "ENVIADA".equals(n.estado)));
    }

    @Test
    void falloDeUnDestinatarioNoReenviaAlQueYaRecibio() throws Exception {
        Pedido pedido = pedido(); pago(pedido); confirmar(pedido);
        correo.fallarPara = email;
        var lista = notificaciones.findAllByPedidoId(pedido.getId());
        lista.forEach(n -> worker.enviar(n.id));
        assertEquals(1, correo.enviados.size());
        assertEquals("admin@example.test", correo.enviados.get(0).getTo()[0]);
        var comprador = notificaciones.findAllByPedidoId(pedido.getId()).stream()
                .filter(n -> "COMPRADOR".equals(n.tipo)).findFirst().orElseThrow();
        assertEquals("PENDIENTE", comprador.estado);
        assertEquals(1, comprador.intentos);
        assertEquals(Pedido.EstadoPedido.PAGADO, pedidos.findById(pedido.getId()).orElseThrow().getEstado());
        comprador.proximoIntento = Instant.now().minusSeconds(1);
        notificaciones.save(comprador);
        correo.fallarPara = null;
        lista.forEach(n -> worker.enviar(n.id));
        assertEquals(2, correo.enviados.size());
        assertEquals(email, correo.enviados.get(1).getTo()[0]);
    }

    @Test
    void pagoPendienteIncorrectoRealODeOtraCuentaNoGeneraCorreos() {
        Pedido pedido = pedido(); pago(pedido);
        pagos.respuesta.setStatus("requires_payment_method");
        assertThrows(IllegalStateException.class, () -> confirmar(pedido));
        pago(pedido); pagos.respuesta.setAmount(1L);
        assertThrows(IllegalStateException.class, () -> confirmar(pedido));
        pago(pedido); pagos.respuesta.setMetadata(Map.of("pedidoId", "otro"));
        assertThrows(IllegalStateException.class, () -> confirmar(pedido));
        pago(pedido); pagos.respuesta.setLivemode(true);
        assertThrows(IllegalStateException.class, () -> confirmar(pedido));
        pago(pedido);
        assertThrows(IllegalArgumentException.class, () -> compras.confirmarCompraYPago(pedido.getId(), pagos.respuesta.getId(), "otro@example.test"));
        assertEquals(0, notificaciones.count());
        assertEquals(Pedido.EstadoPedido.PENDIENTE, pedidos.findById(pedido.getId()).orElseThrow().getEstado());
    }
}
