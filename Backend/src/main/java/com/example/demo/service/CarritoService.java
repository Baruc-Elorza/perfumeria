package com.example.demo.service;

import com.example.demo.dto.CarritoRespuesta;
import com.example.demo.model.Carrito;
import com.example.demo.model.CarritoPerfume;
import com.example.demo.model.Perfume;
import com.example.demo.repository.CarritoRepository;
import com.example.demo.repository.CarritoPerfumeRepository;
import com.example.demo.repository.PerfumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CarritoService {
    private final CarritoRepository carritos;
    private final CarritoPerfumeRepository lineas;
    private final PerfumeRepository perfumes;

    public CarritoService(CarritoRepository carritos, CarritoPerfumeRepository lineas,
                          PerfumeRepository perfumes) {
        this.carritos = carritos;
        this.lineas = lineas;
        this.perfumes = perfumes;
    }

    @Transactional
    public Long crear() {
        return carritos.save(new Carrito(0)).getId();
    }

    @Transactional(readOnly = true)
    public CarritoRespuesta obtenerCarrito(Long id) {
        return respuesta(id == null ? List.of() : lineas.findByCarritoIdOrderByIdAsc(id));
    }

    @Transactional
    public CarritoRespuesta agregar(Long id, Long perfumeId, Integer cantidad) {
        return actualizar(id, perfumeId, cantidad, true);
    }

    @Transactional
    public CarritoRespuesta cambiarCantidad(Long id, Long perfumeId, Integer cantidad) {
        return actualizar(id, perfumeId, cantidad, false);
    }

    private CarritoRespuesta actualizar(Long id, Long perfumeId, Integer cantidad, boolean sumar) {
        if (cantidad == null || cantidad < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor que cero.");
        }
        Carrito carrito = bloquear(id);
        Perfume perfume = perfumes.findById(perfumeId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El perfume ya no existe."));
        List<CarritoPerfume> productos = lineas.findByCarritoIdOrderByIdAsc(id);
        CarritoPerfume linea = productos.stream()
                .filter(p -> p.getPerfume().getId().equals(perfumeId)).findFirst().orElse(null);
        if (linea == null && !sumar) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El perfume no está en el carrito.");
        }
        long nuevaCantidad = (long) cantidad + (sumar && linea != null ? linea.getCantidad() : 0);
        if (perfume.getStock() == null || nuevaCantidad > perfume.getStock()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay existencias suficientes.");
        }
        if (perfume.getPrecio() == null || !Double.isFinite(perfume.getPrecio()) || perfume.getPrecio() < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El perfume no tiene un precio válido.");
        }
        if (linea == null) {
            linea = new CarritoPerfume();
            linea.setCarrito(carrito);
            linea.setPerfume(perfume);
            productos.add(linea);
        }
        linea.setCantidad((int) nuevaCantidad);
        lineas.save(linea);
        return actualizarSubtotal(carrito, productos);
    }

    @Transactional
    public CarritoRespuesta eliminar(Long id, Long perfumeId) {
        if (id == null) return respuesta(List.of());
        Carrito carrito = bloquear(id);
        List<CarritoPerfume> productos = lineas.findByCarritoIdOrderByIdAsc(id);
        productos.removeIf(linea -> {
            if (!linea.getPerfume().getId().equals(perfumeId)) return false;
            lineas.delete(linea);
            return true;
        });
        return actualizarSubtotal(carrito, productos);
    }

    private Carrito bloquear(Long id) {
        if (id == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El carrito está vacío.");
        return carritos.buscarParaActualizar(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El carrito ya no existe."));
    }

    private CarritoRespuesta actualizarSubtotal(Carrito carrito, List<CarritoPerfume> productos) {
        CarritoRespuesta resultado = respuesta(productos);
        carrito.setSubtotal(resultado.subtotal().doubleValue());
        return resultado;
    }

    private CarritoRespuesta respuesta(List<CarritoPerfume> productos) {
        List<CarritoRespuesta.Linea> items = productos.stream().map(linea -> {
            Perfume p = linea.getPerfume();
            BigDecimal precio = BigDecimal.valueOf(p.getPrecio() == null ? 0 : p.getPrecio())
                    .setScale(2, RoundingMode.HALF_UP);
            return new CarritoRespuesta.Linea(p.getId(), p.getNombre(), p.getMarca(), p.getImagenUrl(),
                    precio, p.getStock() == null ? 0 : p.getStock(), linea.getCantidad());
        }).toList();
        BigDecimal subtotal = items.stream().map(p -> p.precio().multiply(BigDecimal.valueOf(p.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        return new CarritoRespuesta(items, subtotal);
    }
}
