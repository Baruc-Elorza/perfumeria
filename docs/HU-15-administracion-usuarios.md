# HU-15 — Administración de usuarios

## Alcance implementado

- Pantalla /admin/usuarios en pages/admin-usuarios, protegida por adminGuard.
- Enlaces desde catálogo (solo si la sesión indica administrador), perfumes y envíos.
- Lista con ID en detalle, nombre, correo, estado activo/bloqueado y tipo de cuenta.
- Búsqueda local por nombre o correo, sin distinguir mayúsculas; filtros todos/activos/bloqueados.
- Estados de carga, lista vacía, búsqueda sin resultados, error y reintento con Actualizar lista.
- Detalle actualizado desde backend.
- Bloqueo y desbloqueo mediante confirmación identificando nombre y correo.
- Conserva usuario, contraseña, direcciones y pedidos. No elimina ni cambia sus datos.
- Las cuentas de administrador son visibles, pero solo se cambia el estado de clientes.
- Prohibición de cambiar el estado de la propia cuenta en backend y UI.
- DTO exclusivo de administración sin contraseña ni hash.
- Estilos únicamente en Frontend/src/styles.css bajo .page-admin-usuarios.

## Autorización y acceso

Se aprovecha la configuración existente app.admin.emails / APP_ADMIN_EMAILS.
No se asignan roles desde la pantalla. El correo debe corresponder a una cuenta registrada.
Para probar con una cuenta propia, configurar en la terminal de Backend:

```bash
export APP_ADMIN_EMAILS="correo-del-administrador@ejemplo.com"
sh ./mvnw spring-boot:run
```

Mantener las demás variables de MySQL/Stripe requeridas por la aplicación.
Iniciar sesión y abrir http://localhost:4200/admin/usuarios.
No se crean usuarios de ejemplo automáticamente.

Backend protege /api/admin/usuarios/** y las consultas antiguas
/api/usuarios y /api/usuarios/{id} con ROLE_ADMIN.
Registro e inspección pública de disponibilidad de correo conservan su política previa.
Un cliente común no puede consultar la lista aunque invoque la API directamente.

## Endpoints

| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | /api/admin/usuarios | Lista de UsuarioAdminRespuesta |
| GET | /api/admin/usuarios/{id} | Datos de un usuario; 404 si no existe |
| PATCH | /api/admin/usuarios/{id}/estado | JSON {"activo":false} bloquea; {"activo":true} desbloquea |

Respuesta: id, nombre, email, activo, administrador, puedeCambiarEstado.
Un estado nulo/ausente no es válido.
Cambiar el estado propio o de otro administrador produce 409.
La fila se bloquea dentro de la transacción para serializar cambios simultáneos.
Pedir el estado que ya tiene no revoca sesiones otra vez.

## Sesiones y bloqueo

Usuario ya tenía activo; se agrega versionSesion (columna version_sesion, bigint default 0).
Con ddl-auto=update se incorporará al reiniciar el backend; en un despliegue
con migraciones debe añadirse como BIGINT NOT NULL DEFAULT 0 antes del arranque.

Al iniciar sesión se guarda la versión en HttpSession.
UsuarioActivoFilter, instalado solo en la cadena de Spring Security,
comprueba usuario activo y versión de sesión antes de autorizar cada petición.
Al bloquear o desbloquear se incrementa la versión:
- no se permiten nuevos inicios de sesión de cuentas bloqueadas;
- las sesiones existentes reciben 401 en la siguiente petición y se invalidan;
- desbloquear no reactiva las sesiones antiguas; exige nuevo login;
- varias sesiones del mismo cliente quedan revocadas por el mismo cambio.

Las sesiones previas a esta entrega no tienen versión y deben volver a iniciar sesión.
Una petición ya iniciada antes del bloqueo puede terminar: el control se aplica a
peticiones posteriores, no cancela operaciones que ya estaban ejecutándose.
El HTML ya visible no desaparece por sí solo: no se añadió notificación por WebSocket.
El acceso al catálogo público continúa disponible como invitado después de salir.
Cada petición autenticada agrega una consulta a usuario para validar revocación.

Login muestra un mensaje específico si la cuenta está bloqueada.
La pantalla administrativa redirige a login si su sesión recibe 401 y muestra
falta de permisos si recibe 403.

## Criterios de aceptación y verificación manual pendiente

Usar un administrador y un cliente, en navegadores/perfiles distintos:

1. Administrador: ver listado y detalle; no debe aparecer contraseña/hash en respuestas.
2. Buscar por nombre/correo, cambiar mayúsculas, filtrar estados y limpiar filtros.
3. Probar una búsqueda sin coincidencias.
4. Seleccionar Bloquear y Cancelar: el estado no debe cambiar.
5. Confirmar bloqueo: listado y detalle cambian y los pedidos permanecen.
6. Cliente con sesión abierta: siguiente consulta protegida debe rechazarla.
7. Cliente bloqueado: nuevo login debe fallar.
8. Desbloquear antes de que otra sesión antigua haga una petición:
   esa sesión también debe ser rechazada; un login nuevo sí debe funcionar.
9. Cliente común: /admin/usuarios no permite acceso; la API devuelve denegación.
10. Cuenta propia y demás administradores: sin botón de bloqueo; PATCH directo rechazado.
11. Actualizar lista después de cambios hechos por otro administrador.
12. Simular indisponibilidad de backend y comprobar error/reintento.

No se añadieron ni ejecutaron tests automatizados en esta entrega.
La compilación del backend y del frontend se comprobó; eso no sustituye
los criterios manuales ni pruebas automatizadas de autorización y sesiones.
No se ejecutaron bloqueos reales ni se reinició el backend contra MySQL.

## Archivos principales

Backend:
- controller/AdminUsuariosController.java
- service/AdministracionUsuariosService.java
- dto/UsuarioAdminRespuesta.java
- config/UsuarioActivoFilter.java
- Ajustes en SecurityConfig, AuthController, Usuario, UsuarioRepository,
  UsuarioDetailsService y ApiExceptionHandler.

Frontend:
- pages/admin-usuarios/admin-usuarios.component.ts y .html
- services/usuario-admin.service.ts y models/usuario-admin.model.ts
- Ruta, enlaces administrativos, mensaje de login y estilos globales.

## Límites del alcance

El detalle muestra los datos existentes en Usuario (nombre/correo/estado/rol),
no un expediente nuevo con campos que el registro aún no recopila.
El listado carga todos los usuarios y filtra en navegador; si crece mucho,
convendrá paginación y búsqueda en servidor.
No incluye edición de datos, asignación de roles ni borrado físico.
El bloqueo preserva pedidos, pagos y notificaciones ya registrados.
