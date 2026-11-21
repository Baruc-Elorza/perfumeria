package com.example.demo;

import com.example.demo.model.Perfume;
import com.example.demo.repository.CarritoPerfumeRepository;
import com.example.demo.repository.CarritoRepository;
import com.example.demo.repository.PerfumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.admin.emails=admin@test.example", "stripe.public.key=", "stripe.secret.key="})
class DemoApplicationTests {
    @Value("${local.server.port}") int port;
    @Autowired PerfumeRepository perfumes;
    @Autowired CarritoRepository carritos;
    @Autowired CarritoPerfumeRepository lineas;
    private final JsonMapper json = JsonMapper.builder().build();
    private HttpClient cliente;
    private BigInteger disponible;
    private BigInteger agotado;

  @BeforeEach
    void preparar() {
        lineas.deleteAll();
        carritos.deleteAll();
        perfumes.deleteAll();

        Perfume perfume1 = new Perfume();
        perfume1.setNombre("Sauvage");
        perfume1.setMarca("Dior");
        perfume1.setPrecio(2450.50);
        perfume1.setStock(3);
        disponible = perfumes.save(perfume1).getId();

        Perfume perfume2 = new Perfume();
        perfume2.setNombre("Eros");
        perfume2.setMarca("Versace");
        perfume2.setPrecio(2100.0);
        perfume2.setStock(0);
        agotado = perfumes.save(perfume2).getId();

        cliente = nuevoCliente();
    }

    private HttpClient nuevoCliente() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }

    private HttpResponse<String> request(HttpClient client, String metodo, String ruta, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode contenido(HttpResponse<String> response, int estado) {
        assertEquals(estado, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    @Test
    void direccionesSeGuardanPorSesionYNoSeCompartenEntreCuentas() throws Exception {
        HttpClient admin = nuevoCliente();
        request(admin, "POST", "/api/usuarios/registro",
                "{\"nombre\":\"Admin\",\"email\":\"admin@test.example\",\"contrasenia\":\"test12345\"}");
        contenido(request(admin, "POST", "/api/auth/login",
                "{\"email\":\"admin@test.example\",\"contrasenia\":\"test12345\"}"), 200);
        String email = "direcciones-" + java.util.UUID.randomUUID() + "@test.example";
        String credenciales = "{\"email\":\"" + email + "\",\"contrasenia\":\"test12345\"}";
        HttpClient comprador = nuevoCliente();
        JsonNode cuenta = contenido(request(comprador, "POST", "/api/usuarios/registro",
                "{\"nombre\":\"Comprador\",\"email\":\"" + email + "\",\"contrasenia\":\"test12345\"}"), 201);
        contenido(request(comprador, "POST", "/api/auth/login", credenciales), 200);
        String datos = "{\"calle\":\"Calle prueba\",\"colonia\":\"Centro\",\"ciudad\":\"Oaxaca\","
                + "\"estado\":\"Oaxaca\",\"codigoPostal\":\"68000\",\"telefono\":\"9511234567\"}";
        JsonNode creada = contenido(request(comprador, "POST", "/api/direcciones", datos), 201);
        String id = creada.get("id").asText();
        assertNull(creada.get("usuario"), "No debe exponer los datos de la cuenta");
        assertEquals("9511234567", creada.get("telefono").asText());
        assertEquals(1, contenido(request(comprador, "GET", "/api/direcciones", null), 200).size());
        assertEquals(0, contenido(request(admin, "GET", "/api/direcciones", null), 200).size());
        assertEquals(403, request(admin, "GET", "/api/direcciones/usuario/" + cuenta.get("id").asText(), null).statusCode());
        assertEquals(404, request(admin, "PUT", "/api/direcciones/" + id, datos).statusCode());
        assertEquals(404, request(admin, "DELETE", "/api/direcciones/" + id, null).statusCode());
        assertEquals(400, request(admin, "POST", "/api/direcciones", datos.replace("{", "{\"id\":" + id + ",")).statusCode());
        JsonNode propiaAdmin = contenido(request(admin, "POST", "/api/direcciones",
                datos.replace("{", "{\"usuarioId\":" + cuenta.get("id").asText() + ",")), 201);
        assertEquals(1, contenido(request(admin, "GET", "/api/direcciones", null), 200).size());
        assertEquals(1, contenido(request(comprador, "GET", "/api/direcciones", null), 200).size());
        JsonNode editada = contenido(request(comprador, "PUT", "/api/direcciones/" + id,
                datos.replace("Calle prueba", "Calle editada")), 200);
        assertEquals("Calle editada", editada.get("calle").asText());
        contenido(request(comprador, "POST", "/api/auth/logout", "{}"), 200);
        assertEquals(403, request(comprador, "GET", "/api/direcciones", null).statusCode());
        contenido(request(comprador, "POST", "/api/auth/login",
                "{\"email\":\"admin@test.example\",\"contrasenia\":\"test12345\"}"), 200);
        JsonNode cambioCuenta = contenido(request(comprador, "GET", "/api/direcciones", null), 200);
        assertEquals(propiaAdmin.get("id").asText(), cambioCuenta.get(0).get("id").asText());
        assertEquals(204, request(admin, "DELETE", "/api/direcciones/" + propiaAdmin.get("id").asText(), null).statusCode());
        contenido(request(comprador, "POST", "/api/auth/login", credenciales), 200);
        assertEquals(204, request(comprador, "DELETE", "/api/direcciones/" + id, null).statusCode());
        assertEquals(0, contenido(request(comprador, "GET", "/api/direcciones", null), 200).size());
    }

    @Test
    void administradorCreaYEditaPerfumeYCompradorLoAgregaAlCarrito() throws Exception {
        request(cliente, "POST", "/api/usuarios/registro",
                "{\"nombre\":\"Admin\",\"email\":\"admin@test.example\",\"contrasenia\":\"test12345\"}");
        JsonNode sesion = contenido(request(cliente, "POST", "/api/auth/login",
                "{\"email\":\"admin@test.example\",\"contrasenia\":\"test12345\"}"), 200);
        assertTrue(sesion.get("administrador").asBoolean());
        String datos = "{\"nombre\":\"Prueba\",\"marca\":\"Marca prueba\",\"descripcion\":\"Aroma fresco\",\"ml\":100,\"concentracion\":\"EDP\",\"precio\":100,\"stock\":3}";
        JsonNode creado = contenido(request(cliente, "POST", "/api/perfumes", datos), 200);
        assertEquals("Marca prueba", creado.get("marca").asText());
        String id = creado.get("id").asText();
        JsonNode editado = contenido(request(cliente, "PUT", "/api/perfumes/" + id,
                datos.replace("Prueba", "Editado")), 200);
        assertEquals("Editado", editado.get("nombre").asText());
        JsonNode detalle = contenido(request(cliente, "GET", "/api/perfumes/" + id, null), 200);
        assertEquals("Aroma fresco", detalle.get("descripcion").asText());
        assertEquals(100, detalle.get("ml").asInt());
        assertEquals("EDP", detalle.get("concentracion").asText());
        HttpClient comprador = nuevoCliente();
        request(comprador, "POST", "/api/usuarios/registro",
                "{\"nombre\":\"Comprador\",\"email\":\"comprador@test.example\",\"contrasenia\":\"test12345\"}");
        JsonNode login = contenido(request(comprador, "POST", "/api/auth/login",
                "{\"email\":\"comprador@test.example\",\"contrasenia\":\"test12345\"}"), 200);
        assertFalse(login.get("administrador").asBoolean());
        assertEquals(403, request(comprador, "POST", "/api/perfumes", datos).statusCode());
        JsonNode carrito = contenido(request(comprador, "POST", "/api/carrito/items/" + id,
                "{\"cantidad\":1}"), 200);
        assertEquals("Marca prueba", carrito.get("items").get(0).get("marca").asText());
        assertEquals(100, carrito.get("subtotal").asDouble());
    }

    @Test
    void catalogoBuscaSinDistinguirMayusculasYRecuperaListaVacia() throws Exception {
        assertEquals(2, contenido(request(cliente, "GET", "/api/perfumes", null), 200).size());
        JsonNode resultado = contenido(request(cliente, "GET", "/api/perfumes?nombre=%20sAuV%20", null), 200);
        assertEquals(1, resultado.size());
        assertEquals("Sauvage", resultado.get(0).get("nombre").asText());
        assertEquals(0, contenido(request(cliente, "GET", "/api/perfumes?nombre=inexistente", null), 200).size());
        assertEquals(2, contenido(request(cliente, "GET", "/api/perfumes?nombre=%20%20", null), 200).size());
    }

    @Test
    void carritoPersisteCantidadesCalculaSubtotalYAislaSesiones() throws Exception {
        assertEquals(0, contenido(request(cliente, "GET", "/api/carrito", null), 200).get("items").size());
        JsonNode agregado = contenido(request(cliente, "POST", "/api/carrito/items/" + disponible, "{\"cantidad\":1}"), 200);
        assertEquals(1, agregado.get("items").get(0).get("cantidad").asInt());
        assertEquals(2450.50, agregado.get("subtotal").asDouble());
        JsonNode modificado = contenido(request(cliente, "PATCH", "/api/carrito/items/" + disponible, "{\"cantidad\":2}"), 200);
        assertEquals(4901.0, modificado.get("subtotal").asDouble());
        assertEquals(2, contenido(request(cliente, "GET", "/api/carrito", null), 200).get("items").get(0).get("cantidad").asInt());
        assertEquals(1, lineas.count());
        assertEquals(4901.0, carritos.findAll().get(0).getSubtotal());
        assertEquals(0, contenido(request(nuevoCliente(), "GET", "/api/carrito", null), 200).get("items").size());
        JsonNode eliminado = contenido(request(cliente, "DELETE", "/api/carrito/items/" + disponible, null), 200);
        assertEquals(0, eliminado.get("items").size());
        assertEquals(0, eliminado.get("subtotal").asDouble());
        assertEquals(0, lineas.count());
    }

    @Test
    void rechazaAgotadosCantidadesInvalidasYAcumulacionMayorAlStock() throws Exception {
        assertEquals(409, request(cliente, "POST", "/api/carrito/items/" + agotado, "{\"cantidad\":1}").statusCode());
        assertEquals(400, request(cliente, "POST", "/api/carrito/items/" + disponible, "{\"cantidad\":0}").statusCode());
        assertEquals(400, request(cliente, "POST", "/api/carrito/items/" + disponible, "{}").statusCode());
        contenido(request(cliente, "POST", "/api/carrito/items/" + disponible, "{\"cantidad\":3}"), 200);
        assertEquals(409, request(cliente, "POST", "/api/carrito/items/" + disponible, "{\"cantidad\":1}").statusCode());
        assertEquals(409, request(cliente, "PATCH", "/api/carrito/items/" + disponible, "{\"cantidad\":4}").statusCode());
        assertEquals(3, contenido(request(cliente, "GET", "/api/carrito", null), 200).get("items").get(0).get("cantidad").asInt());
        assertEquals(3, perfumes.findById(disponible).orElseThrow().getStock());
    }

    @Test
    void permitePreflightAngularConCookies() throws Exception {
        HttpRequest preflight = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/carrito/items/" + disponible))
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> respuesta = cliente.send(preflight, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respuesta.statusCode());
        assertEquals("http://localhost:4200", respuesta.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        assertEquals("true", respuesta.headers().firstValue("Access-Control-Allow-Credentials").orElse(""));
    }
}
