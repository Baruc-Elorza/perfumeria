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

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {
    @Value("${local.server.port}") int port;
    @Autowired PerfumeRepository perfumes;
    @Autowired CarritoRepository carritos;
    @Autowired CarritoPerfumeRepository lineas;
    private final JsonMapper json = JsonMapper.builder().build();
    private HttpClient cliente;
    private Long disponible;
    private Long agotado;

  @BeforeEach
    void preparar() {
        lineas.deleteAll();
        carritos.deleteAll();
        perfumes.deleteAll();

        Perfume perfume1 = new Perfume();
        perfume1.setNombre("Sauvage");
        perfume1.setMarca("Dior");
        perfume1.setDescripcion("Descripcion Sauvage");
        perfume1.setPrecio(2450.50);
        perfume1.setStock(3);
        disponible = perfumes.save(perfume1).getId();

        Perfume perfume2 = new Perfume();
        perfume2.setNombre("Eros");
        perfume2.setMarca("Versace");
        perfume2.setDescripcion("Descripcion Eros");
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