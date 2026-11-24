# Correos de compras de prueba

Después de verificar el pago en Stripe, el backend registra dos notificaciones
persistentes en la misma transacción que marca el pedido como PAGADO:

- COMPRADOR: al correo del propietario del pedido, obtenido en el backend.
- ADMIN: a `COMPRA_EMAIL_ADMIN`, configurado en el servidor.

Son mensajes separados, incluso si el administrador compra con su propia cuenta.
Incluyen nombre y correo del comprador, ID de pedido, referencia de pago, fecha,
productos y marcas, cantidades, precios unitarios, importes, subtotal, envío,
descuento, total MXN, destinatario, dirección y teléfono. Asunto y cuerpo indican
que es una compra de prueba y que no hubo un cargo real.

La confirmación rechaza pagos sin estado succeeded, importes o moneda incorrectos,
metadata de otro pedido y pagos de producción. El correo del comprador no se toma
del navegador. El pedido se bloquea durante la confirmación. La clave única
(pedido_id, tipo) impide registrar dos veces la misma notificación.

## Preparar la base existente

Respaldar `notificacion_compra` y ejecutar
[migrar-notificaciones-dos-correos.sql](migrar-notificaciones-dos-correos.sql)
con el backend detenido antes del primer arranque de esta versión. El script
conserva los registros anteriores como ADMIN y reemplaza la restricción antigua
que permitía un único mensaje por pedido. En bases nuevas Hibernate crea las tablas.
Los pedidos usan UUID de texto, compatibles con el esquema local existente.

## Configurar entrega real por SMTP

Los pagos siguen siendo de prueba; los correos sí se entregan a direcciones reales.
Completar `Backend/application-local.properties`, ignorado por Git, con los valores
del proveedor. El archivo `Backend/application-mail.example.properties` muestra
los nombres de las propiedades sin credenciales reales. No basta con la contraseña
de inicio de sesión de la aplicación: hace falta una cuenta remitente y sus
credenciales SMTP autorizadas.

| Variable | Uso |
| --- | --- |
| COMPRA_EMAIL_ENABLED | true para activar el envío, false por defecto. |
| COMPRA_EMAIL_ADMIN | Correo que recibe el resumen para administración. |
| COMPRA_EMAIL_FROM | Remitente autorizado por SMTP. |
| MAIL_HOST / MAIL_PORT | Servidor y puerto del proveedor. |
| MAIL_USERNAME / MAIL_PASSWORD | Usuario y contraseña o token SMTP. |
| MAIL_AUTH | Activa autenticación cuando la exige el proveedor. |
| MAIL_STARTTLS | Activa y exige STARTTLS. |
| MAIL_SSL | TLS implícito; no activar junto con STARTTLS. |

Si se usan propiedades directas en el archivo local, tienen prioridad sobre las
variables de entorno equivalentes. Reiniciar el backend tras configurarlas.
No activar el envío hasta completar y verificar remitente, servidor y credenciales.

## Reintentos y comprobación

El trabajador procesa hasta diez mensajes cada treinta segundos. Cada destinatario
tiene estado e intentos independientes: el fallo de un correo no revierte la compra
ni reenvía el mensaje que ya se entregó al otro destinatario. Tras cinco fallos,
queda FALLIDA. SMTP aceptado significa ENVIADA, sin garantizar lectura ni entrega
final. Si el proceso cae después de la aceptación SMTP y antes de guardar el estado,
un reintento puede duplicar ese correo.

Con el envío desactivado, las notificaciones permanecen pendientes en MySQL y se
procesarán al activarlo. Revisar la cola antes de activarlo si hay compras antiguas.

```sql
SELECT id, pedido_id, tipo, estado, intentos, creado_en, enviado_en, ultimo_error
FROM notificacion_compra ORDER BY id DESC;
```

Las pruebas `NotificacionesCompraTests` verifican dos destinatarios, contenido,
confirmación repetida, reintentos independientes y rechazo de pagos inválidos o de
otra cuenta. Usan H2, un proveedor de pagos simulado y un remitente capturador en
memoria: no cobran ni envían correos reales.

Para verificar entrega real, completar SMTP, pagar con Stripe en modo de prueba y
comprobar el mensaje recibido tanto en la cuenta compradora como en administración.
