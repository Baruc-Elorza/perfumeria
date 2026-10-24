package com.example.demo.controller;

import com.example.demo.dto.CarritoRespuesta;
import com.example.demo.model.Pedido;
import com.example.demo.service.CarritoService;
import com.example.demo.service.CompraService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/compras")
@CrossOrigin(
    origins = "http://localhost:4200",
    allowCredentials = "true"
)
public class CompraController {

    private final CompraService compraService;
    private final CarritoService carritoService;

    public CompraController(
            CompraService compraService,
            CarritoService carritoService) {
        this.compraService = compraService;
        this.carritoService = carritoService;
    }

    @PostMapping("/pendiente")
    public ResponseEntity<?> crearPedidoPendiente(
            @RequestBody CrearPedidoRequest request,
            Authentication authentication,
            HttpSession session) {

        try {
            if (authentication == null
                    || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(Map.of(
                            "error",
                            "Debes iniciar sesión."
                        ));
            }

            if (request == null
                    || esVacio(request.getCalle())
                    || esVacio(request.getColonia())
                    || esVacio(request.getCiudad())
                    || esVacio(request.getCodigoPostal())
                    || esVacio(request.getTelefono())) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                            "error",
                            "Completa todos los datos de envío."
                        ));
            }

            Long carritoId =
                    (Long) session.getAttribute("carritoId");

            CarritoRespuesta carrito =
                    carritoService.obtenerCarrito(carritoId);

            Pedido pedido = compraService.crearPedidoPendiente(
                    authentication.getName(),
                    request.getCalle(),
                    request.getColonia(),
                    request.getCiudad(),
                    request.getCodigoPostal(),
                    request.getTelefono(),
                    carrito
            );

            return ResponseEntity.ok(Map.of(
                    "pedidoId", pedido.getId(),
                    "estado", pedido.getEstado().name(),
                    "total", pedido.getTotal()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                        "error",
                        "No se pudo crear el pedido."
                    ));
        }
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }


    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmarCompra(
            @RequestBody ConfirmacionPagoRequest request,
            Authentication authentication) {

        try {
            if (authentication == null
                    || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401)
                        .body(Map.of(
                                "error",
                                "Debes iniciar sesión."
                        ));
            }

            if (request == null
                    || esVacio(request.getPedidoId())
                    || esVacio(request.getReferenciaPagoProveedor())) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Faltan los datos de confirmación."
                        ));
            }

            compraService.confirmarCompraYPago(
                    request.getPedidoId(),
                    request.getReferenciaPagoProveedor(),
                    authentication.getName()
            );

            return ResponseEntity.ok(Map.of(
                    "mensaje",
                    "Compra y pago confirmados correctamente"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error",
                            "Error al verificar y confirmar el pago."
                    ));
        }
    }


    public static class CrearPedidoRequest {
        private String calle;
        private String colonia;
        private String ciudad;
        private String codigoPostal;
        private String telefono;

        public String getCalle() {
            return calle;
        }

        public void setCalle(String calle) {
            this.calle = calle;
        }

        public String getColonia() {
            return colonia;
        }

        public void setColonia(String colonia) {
            this.colonia = colonia;
        }

        public String getCiudad() {
            return ciudad;
        }

        public void setCiudad(String ciudad) {
            this.ciudad = ciudad;
        }

        public String getCodigoPostal() {
            return codigoPostal;
        }

        public void setCodigoPostal(String codigoPostal) {
            this.codigoPostal = codigoPostal;
        }

        public String getTelefono() {
            return telefono;
        }

        public void setTelefono(String telefono) {
            this.telefono = telefono;
        }
    }

    public static class ConfirmacionPagoRequest {
        private String pedidoId;
        private String referenciaPagoProveedor;

        public String getPedidoId() {
            return pedidoId;
        }

        public void setPedidoId(String pedidoId) {
            this.pedidoId = pedidoId;
        }

        public String getReferenciaPagoProveedor() {
            return referenciaPagoProveedor;
        }

        public void setReferenciaPagoProveedor(
                String referenciaPagoProveedor) {
            this.referenciaPagoProveedor =
                    referenciaPagoProveedor;
        }
    }
}
