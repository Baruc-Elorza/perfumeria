-- Ejecutar manualmente solo si conservas registros de la versión anterior.
-- Arrancar primero el backend para crear la tabla perfume.
-- Copia el catálogo anterior únicamente cuando la tabla nueva está vacía.
-- No elimina ni modifica perfumes. Si ambas tablas tienen datos, revisar los IDs antes de fusionar.
USE perfumeria;
START TRANSACTION;
SET @catalogo_nuevo_vacio = (SELECT COUNT(*) = 0 FROM perfume);
INSERT INTO perfume (id, nombre, marca, descripcion, precio, stock, imagen_url)
SELECT p.id, p.nombre, p.marca, p.descripcion, p.precio, p.stock, p.imagen_url
FROM perfumes p
WHERE @catalogo_nuevo_vacio;
COMMIT;
