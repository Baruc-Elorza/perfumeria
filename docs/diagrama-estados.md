# Diagrama de estados del proyecto

El diagrama muestra el ciclo de vida del pedido que coordina el flujo de compra. Las transiciones reflejan el código actual; los estados `RECHAZADO`, `CANCELADO` y `ENTREGADO` existen en el modelo, pero no se encontró una operación que los active.

```mermaid
stateDiagram-v2
    direction LR

    [*] --> Carrito
    Carrito --> Carrito: agregar / cambiar cantidad / quitar
    Carrito --> Checkout: iniciar compra [sesión activa y carrito no vacío]
    Checkout --> Carrito: corregir carrito o dirección
    Checkout --> Pendiente: crear pedido
    Pendiente --> Pagado: Stripe confirma pago exitoso y se validan moneda, pedido e importe
    Pagado --> Enviado: administración registra número de guía
    Enviado --> [*]

    state "Pedido pendiente" as Pendiente
    state "Pago confirmado" as Pagado
    state "Pedido enviado" as Enviado

    Pendiente --> Pendiente: pago aún no confirmado / validación rechazada

    state "Notificación de compra" as Notificacion {
        [*] --> N_Pendiente: pedido pasa a PAGADO
        N_Pendiente --> N_Enviada: correo aceptado por SMTP
        N_Pendiente --> N_Pendiente: error SMTP / reintento programado
        N_Pendiente --> N_Fallida: 5 intentos fallidos
        N_Enviada --> [*]
        N_Fallida --> [*]
    }
```

## Lectura del flujo

- El carrito persiste por sesión; agregar productos no reserva ni descuenta existencias.
- El pedido se crea en estado `PENDIENTE`. Solo pasa a `PAGADO` tras verificar con Stripe que el pago fue exitoso y coincide con el pedido.
- Desde `PAGADO`, administración guarda una guía y el pedido cambia a `ENVIADO`.
- Al confirmar el pago se registra una notificación pendiente. El correo requiere que las notificaciones y SMTP estén habilitados; los errores se reintentan hasta cinco veces.
- Aunque el modelo declara `RECHAZADO`, `CANCELADO` y `ENTREGADO`, no están conectados a transiciones en el flujo actual.
