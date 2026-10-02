# Perfumería

Aplicación Angular + Spring Boot + MySQL. El catálogo consulta y busca perfumes en el servidor; el carrito permite agregar, cambiar cantidades y eliminar productos con validación de existencias.

## Requisitos

- Java 21 o superior compatible con Spring Boot 4.1.1.
- MySQL activo por TCP en `localhost:3306`.
- Node 24 LTS (la versión mínima de esa rama para este Angular es 24.15.0).
- pnpm 11.3.0. Hay comandos alternativos con npm abajo si no tienes pnpm o nvm.

## Iniciar el backend

Crea la base una vez:

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS perfumeria;"
```

Desde la carpeta del proyecto:

```bash
cd Backend
export DB_USERNAME=root
read -s "DB_PASSWORD?Contraseña de MySQL: "
export DB_PASSWORD
sh ./mvnw spring-boot:run
```

El comando `read` anterior es para zsh, la terminal predeterminada de macOS. Las credenciales se conservan solo en esa terminal. Si la conexión es distinta puedes definir `DB_URL`.

La API estará en http://localhost:8080/api/perfumes. Una respuesta `[]` significa que el catálogo está vacío.

## Iniciar el frontend

En otra terminal, desde `Frontend`, si tienes nvm y pnpm:

```bash
nvm install
nvm use
pnpm install
pnpm start
```

Alternativa sin instalar Node ni pnpm globalmente:

```bash
npm exec --yes --package=node@24 --package=pnpm@11.3.0 -- pnpm install
npm exec --yes --package=node@24 -- node node_modules/@angular/cli/bin/ng.js serve
```

Abre http://localhost:4200. El catálogo está en `/` y el carrito en `/carrito`. Mantén ambas terminales abiertas; Control+C detiene cada servidor.

## Datos y contrato de la API

La tabla actual del catálogo es `perfume`. La tabla antigua `perfumes` se conserva. Si tiene registros, revisa y ejecuta manualmente [docs/migrar-perfumes.sql](docs/migrar-perfumes.sql) después del primer arranque. El script solo copia cuando la tabla nueva está vacía; no borra tablas ni sobrescribe productos.

`Perfume` admite nombre, marca, precio, stock, imagenUrl, descripcion y las notas notasTop, notasMiddle y notasBase.

Para crear un perfume de ejemplo, con el backend activo:

```bash
curl -X POST http://localhost:8080/api/perfumes \
  -H 'Content-Type: application/json' \
  -d '{"nombre":"Sauvage","marca":"Dior","precio":2450.50,"stock":3,"notasTop":"Bergamota"}'
```

| Método y ruta | Uso |
| --- | --- |
| GET /api/perfumes | Catálogo completo |
| GET /api/perfumes?nombre=sau | Búsqueda parcial, sin distinguir mayúsculas; vacío devuelve todo |
| GET /api/perfumes/{id} | Detalle |
| POST /api/perfumes | Registrar perfume |
| PUT /api/perfumes/{id} | Actualizar perfume |
| DELETE /api/perfumes/{id} | Eliminar perfume |
| GET /api/carrito | Productos y subtotal de la sesión |
| POST /api/carrito/items/{perfumeId} | Agregar unidades: `{"cantidad":1}` |
| PATCH /api/carrito/items/{perfumeId} | Establecer cantidad: `{"cantidad":2}` |
| DELETE /api/carrito/items/{perfumeId} | Quitar producto |

El carrito responde `{items: [...], subtotal: 0}`. Cada elemento contiene perfumeId, nombre, marca, imagenUrl, precio, stock y cantidad. Angular envía la cookie de sesión con `withCredentials`; usa siempre `localhost` al abrir la web.

Los registros del carrito se guardan en MySQL. La asociación al navegador usa una sesión anónima: al expirar la sesión o reiniciar el servidor puede comenzar un carrito nuevo. No hay todavía recuperación por cuenta de usuario.

Agregar al carrito no reserva ni descuenta stock. La validación de stock también se ejecuta en backend. Pago, creación del pedido y envío quedan para sus respectivas historias; el botón de pago aparece deshabilitado.

La administración está disponible en `http://localhost:4200/admin/perfumes`, también desde «Administrar perfumes» en el catálogo. Permite agregar, editar, consultar el detalle y eliminar con confirmación. El formulario incluye precio, existencias, descripción, URL de imagen y notas aromáticas. Si un perfume está en un carrito, su eliminación se rechaza con un aviso; se puede establecer su stock en cero para impedir nuevas compras. La seguridad sigue siendo la configuración local de desarrollo (API pública); la autenticación y los permisos administrativos están pendientes.

## Verificación

Las pruebas del backend usan H2 en memoria, separada de la base MySQL local:

```bash
cd Backend
sh ./mvnw test
```

Para comprobar Angular desde `Frontend`:

```bash
npm exec --yes --package=node@24 -- node node_modules/@angular/cli/bin/ng.js test --watch=false
npm exec --yes --package=node@24 -- node node_modules/@angular/cli/bin/ng.js build
```

Se cubren búsqueda, cancelación de respuestas antiguas, recuperación ante errores, rutas, subtotal, persistencia del carrito, sesiones independientes, cantidades inválidas, agotados y CORS.

La versión de Node está indicada en `Frontend/.nvmrc`. Spring Boot administra las versiones de sus starters y del driver MySQL para mantenerlas alineadas ([documentación oficial](https://docs.spring.io/spring-boot/reference/using/build-systems.html)). La interfaz usa signals para actualizarse al recibir las respuestas HTTP ([Angular](https://angular.dev/guide/signals)).
