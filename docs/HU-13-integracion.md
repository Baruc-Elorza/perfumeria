# HU-13 — Correo de compra al administrador

## Estado de la entrega

Implementado el módulo de backend en `com.example.demo.notificaciones`.
No hay pantalla nueva ni endpoint público para enviar correos.
Todavía no existen los modelos/servicios de pedido y pago: falta la conexión con HU-05 y HU-16.
El envío está desactivado por defecto. No se enviaron correos reales.
Esta entrega no añade ni ejecuta tests automatizados; más abajo se especifican los casos pendientes.

## Mensaje para compartir con el equipo

> La HU-13 ya tiene un servicio interno para registrar el correo de un pedido pagado:
> NotificacionCompraService.registrar(CompraParaNotificar).
> HU-16 debe conservar los datos históricos del pedido, sus productos, cantidades,
> precios, dirección e importes. HU-05 debe verificar el pago con el proveedor
> desde el backend y confirmar referencia, importe y moneda.
> Dentro de la misma transacción que marca el pedido como pagado, deben llamar al
> servicio HU-13. El correo se enviará después del commit; no lo llamen desde Angular
> ni desde la redirección de la pasarela. Coordinen una única llamada desde el flujo
> de confirmación de pago. El servicio requiere transacción activa.
> Falta que acordemos el proveedor SMTP, el remitente autorizado y el correo del
> administrador. Para demostrar la historia completa debemos conectar y probar
> juntos un pedido pagado y otro rechazado.

## Responsabilidades por historia

| Historia | Qué debe entregar |
| --- | --- |
| HU-16 Realizar compras | Pedido persistido con ID estable; destinatario y dirección válidos; líneas con nombre, marca, cantidad y precio de compra; subtotal, envío, descuento y total calculados en backend. |
| HU-05 Pago con tarjeta | Verificación auténtica del proveedor (firma/webhook o consulta según proveedor); comprobar ID del pedido, referencia, importe y moneda; manejo de pagos pendientes, rechazados, cancelados y callbacks repetidos. |
| HU-13 Notificación | Validar el contrato recibido, registrar el correo junto con la confirmación del pedido, construir el texto, enviar al administrador y conservar estado/intentos. |
| Equipo/configuración | Cuenta SMTP y remitente autorizados, destinatario administrador, credenciales por variables de entorno y prueba final de recepción. |

La HU-13 no comprueba pagos contra una pasarela: confía exclusivamente en el
servicio interno de pagos. No exponer su método mediante un controlador que
acepte un estado EXITOSO enviado por el navegador.

## Contrato Java exacto

Clase: `com.example.demo.notificaciones.CompraParaNotificar`.

| Campo | Tipo / reglas |
| --- | --- |
| pedidoId | String, obligatorio, máximo 100, letras ASCII/números/guion/guion bajo. Se usa como clave única de notificación. |
| referenciaPago | String, obligatorio, máximo 150. Referencia del proveedor; nunca número de tarjeta, CVV o token secreto. |
| estadoPago | Enum EstadoPago. Solo EXITOSO permite registrar. PENDIENTE, RECHAZADO y CANCELADO se rechazan. |
| pagadoEn | Instant de confirmación del pago; el correo indica UTC. |
| cliente | Nombre del comprador, obligatorio, máximo 200. |
| destinatarioEnvio | Nombre de quien recibe el paquete, obligatorio, máximo 200. |
| direccionEnvio | Dirección completa ya validada por compras: calle, número, colonia, localidad, estado, CP y país; máximo 1000. |
| telefono | Opcional, máximo 50. |
| productos | Entre 1 y 200 líneas Producto. |
| subtotal | BigDecimal: suma de cantidad × precioUnitario. |
| envio | BigDecimal no negativo, usar 0.00 si no aplica. |
| descuento | BigDecimal no negativo, usar 0.00 si no aplica. |
| total | BigDecimal >= 0.01; debe ser subtotal + envio - descuento. |
| moneda | Por ahora únicamente MXN. |

Cada `CompraParaNotificar.Producto` contiene:

- `productoId`: String obligatorio, máximo 100.
- `nombre`, `marca`: String obligatorio, máximo 255.
- `cantidad`: int >= 1.
- `precioUnitario`: BigDecimal >= 0.01.

Importes: máximo 12 cifras enteras y 2 decimales. Crear BigDecimal desde texto
o valores decimales exactos; evitar `new BigDecimal(double)`.
Estos son los datos del momento de la compra, no valores actuales del catálogo.
Si se añaden variantes/tamaño, impuestos separados u otros datos de envío,
ampliar el contrato y la plantilla de forma coordinada antes de integrar.

## Punto de integración obligatorio

Secuencia propuesta para el servicio de confirmación de pago:

1. Validar el evento con el proveedor y consultar/verificar su resultado según su API.
2. Abrir una transacción de base de datos y bloquear el pedido para actualización.
3. Si ya estaba pagado y la notificación existe, terminar sin repetir efectos.
4. Verificar referencia, importe, moneda y estado permitido del pedido.
5. Guardar el pago exitoso y marcar el pedido como pagado.
6. Construir CompraParaNotificar a partir del pedido persistido.
7. Invocar el bean inyectado `notificacionCompraService.registrar(datos)`.
8. Confirmar la transacción. Solo entonces el trabajador podrá ver el pendiente.

HU-05/HU-16 deben decidir un único responsable del paso 7. El siguiente ejemplo
es un esquema para su futuro servicio, no un endpoint ya implementado:

```java
@Transactional
public void confirmarPago(/* evento previamente verificado */) {
    // Bloquear/cargar pedido.
    // Validar referencia, importe, moneda y duplicados.
    // Guardar estado PAGADO y pago.
    CompraParaNotificar datos = /* construir desde pedido y pago persistidos */;
    notificacionCompraService.registrar(datos);
}
```

`registrar` usa Propagation.MANDATORY: llamarlo sin transacción falla.
Inyectar el servicio de Spring; no crearlo con `new`.
No usar AFTER_COMMIT para registrar el pendiente: se perdería la atomicidad.
Si registrar falla por datos inválidos, se revierte la transacción local.
Eso no revierte un cobro que ya hizo la pasarela: HU-05 debe conservar/reintentar
el evento confirmado y permitir conciliación del pago. No ignorar esa excepción
ni responder al proveedor como si se hubiera procesado correctamente.

El bloqueo del pedido y la deduplicación de eventos son responsabilidad de compras/pagos.
La tabla HU-13 también tiene clave única por pedido. Dos registros concurrentes
sin bloqueo pueden provocar conflicto de unicidad y rollback: reintentar el evento
completo después, no atrapar el conflicto dentro de la misma transacción.

## Ejemplo del objeto que deben construir

```java
var datos = new CompraParaNotificar(
    "PED-1001", "referencia-proveedor-1001",
    CompraParaNotificar.EstadoPago.EXITOSO,
    Instant.parse("2026-10-03T18:00:00Z"),
    "Cliente de ejemplo", "Persona que recibe",
    "Calle Ejemplo 10, Centro, Puebla, Puebla, 72000, México",
    "2220000000",
    List.of(new CompraParaNotificar.Producto(
        "1", "Perfume de ejemplo", "Marca", 2, new BigDecimal("500.00"))),
    new BigDecimal("1000.00"), new BigDecimal("100.00"),
    new BigDecimal("0.00"), new BigDecimal("1100.00"), "MXN"
);
// Solo dentro de una transacción que confirma un pedido real/verificado:
// notificacionCompraService.registrar(datos);
```

Imports: java.math.BigDecimal, java.time.Instant, java.util.List y
com.example.demo.notificaciones.CompraParaNotificar.
Los datos de ejemplo no registran ni pagan una compra por sí solos.

## Qué recibe el administrador

Correo de texto con asunto `Compra confirmada - Pedido <id>`.
Incluye ID, referencia, fecha UTC, estado exitoso, cliente, destinatario, dirección,
teléfono si existe, productos con ID/nombre/marca/cantidad/precio/importe,
subtotal, envío, descuento y total MXN.
La dirección del administrador proviene de configuración del servidor, no del cliente.

## Configuración SMTP

Variables leídas por application.properties:

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| COMPRA_EMAIL_ENABLED | false | Activa trabajador de envío. |
| COMPRA_EMAIL_ADMIN | vacío | Correo del administrador, obligatorio al activar. |
| COMPRA_EMAIL_FROM | vacío | Remitente autorizado por el servidor SMTP. |
| MAIL_HOST | localhost | Host SMTP. |
| MAIL_PORT | 1025 | Puerto SMTP. |
| MAIL_USERNAME | vacío | Usuario SMTP si requiere autenticación. |
| MAIL_PASSWORD | vacío | Contraseña/token SMTP. |
| MAIL_AUTH | false | Autenticación SMTP. |
| MAIL_STARTTLS | false | Activa y exige STARTTLS. |
| MAIL_SSL | false | TLS implícito, si lo exige el proveedor. |

Para SMTP real usar las opciones exactas del proveedor; por ejemplo, STARTTLS
suele usar 587 y TLS implícito 465. No activar ambas modalidades sin que el
proveedor lo indique. No guardar credenciales en Git.
Configurar estas variables en la terminal/entorno del backend y reiniciarlo.

Para una comprobación local con un capturador SMTP ya instalado y escuchando
en localhost:1025, se pueden usar:

```bash
export MAIL_HOST=localhost
export MAIL_PORT=1025
export MAIL_AUTH=false
export MAIL_STARTTLS=false
export MAIL_SSL=false
export COMPRA_EMAIL_ADMIN=administrador@example.test
export COMPRA_EMAIL_FROM=perfumeria@example.test
export COMPRA_EMAIL_ENABLED=true
```

Estas direcciones son solo de ejemplo para captura local; no entregan correo real.
El módulo no instala ni inicia un capturador SMTP. No se activa ni envía nada
automáticamente al copiar este documento.

## Base de datos y comportamiento ante fallos

Hibernate creará `notificacion_compra` al arrancar, usando la configuración
existente ddl-auto=update. En despliegues con migraciones deberá crearse la tabla
mediante una migración equivalente. No modifica las tablas de carrito ni producto.

- PENDIENTE: esperando envío; permanece así aunque se reinicie el backend.
- ENVIADA: SMTP aceptó el mensaje; no garantiza lectura, entrega final ni ausencia de rebote.
- FALLIDA: agotó cinco intentos SMTP; requiere revisión.
- Revisión cada 30 segundos, lotes de hasta 10.
- Espera tras fallos: 1, 2, 3 y 4 minutos antes de los siguientes intentos.
- Si el envío está desactivado, los registros se acumulan y se procesarán al activarlo.
- Destinatario fijado en el primer intento; cambios posteriores de configuración
  aplican a notificaciones aún no intentadas.
- El trabajador bloquea cada fila durante el envío, con timeouts SMTP de 5 segundos.
- Llamadas repetidas para el mismo pedido y referencia conservan una sola notificación.
- Un pedido ya notificado con otra referencia se rechaza para revisión.

SMTP no garantiza entrega exactamente una vez: si el proveedor acepta el correo
y el proceso cae antes de guardar ENVIADA, el reintento puede duplicarlo.
El administrador debe identificar pedidos por su ID y no preparar dos veces
un mismo pedido. No se promete eliminar ese caso sin soporte adicional del proveedor.

Consulta operativa de solo lectura:

```sql
SELECT id, pedido_id, estado, intentos, creado_en, enviado_en, ultimo_error
FROM notificacion_compra
ORDER BY id DESC;
```

Tras corregir SMTP, se puede reactivar una notificación FALLIDA concreta,
revisando primero si el proveedor ya la aceptó. No ejecutar sobre toda la tabla:

```sql
-- Sustituir 123 por el ID revisado.
UPDATE notificacion_compra
SET estado = 'PENDIENTE', intentos = 0,
    proximo_intento = CURRENT_TIMESTAMP, ultimo_error = NULL
WHERE id = 123 AND estado = 'FALLIDA';
```

El cuerpo contiene datos de envío personales. Mantenerlo solo en el backend,
no exponer la tabla mediante un endpoint público y acordar su retención.

## Comprobaciones pendientes para aceptar la HU completa

1. Pedido pagado: se registra un pendiente y llega el correo con todos sus datos.
2. Pago pendiente, rechazado o cancelado: no se registra ni envía.
3. Rollback del pedido: no queda notificación.
4. Callback repetido y callbacks concurrentes: un registro por pedido,
   sin repetir descuento de stock ni cobro.
5. SMTP caído: la compra sigue confirmada, el pendiente reintenta y llega al recuperarse.
6. Cinco fallos: queda FALLIDA, visible para revisión.
7. Importes que no coinciden, pedido incompleto o estado inventado: rechazados.
8. Envío desactivado: no hay conexiones SMTP; cola conservada.
9. Revisión del mensaje recibido y validación de dirección/datos de empaquetado.
10. Prueba integrada HU-05 + HU-16 + HU-13 con el entorno de pruebas de la pasarela.

## Archivos y referencias

Todos los componentes están en Backend/src/main/java/com/example/demo/notificaciones:
CompraParaNotificar (contrato), NotificacionCompra (entidad),
NotificacionCompraRepository (persistencia), NotificacionCompraService (registro y plantilla),
CorreoCompraWorker (envío) y CorreoCompraScheduler (procesamiento periódico).

Referencia de configuración:
[Correo en Spring Boot](https://docs.spring.io/spring-boot/reference/io/email.html).
