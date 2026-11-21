-- MySQL 8. Ejecutar una vez antes de activar la nueva versión del backend.
-- Conserva las notificaciones anteriores como tipo ADMIN.
-- Crear previamente un respaldo de notificacion_compra.
SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
         AND table_name = 'notificacion_compra' AND column_name = 'tipo'),
  'SELECT 1',
  'ALTER TABLE notificacion_compra ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT ''ADMIN''');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

-- La antigua restricción permitía un solo correo por pedido.
SET @indice = (
  SELECT index_name FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'notificacion_compra'
    AND non_unique = 0 AND index_name <> 'PRIMARY'
  GROUP BY index_name HAVING COUNT(*) = 1 AND MAX(column_name) = 'pedido_id'
  LIMIT 1
);
SET @sql = IF(@indice IS NULL, 'SELECT 1',
  CONCAT('ALTER TABLE notificacion_compra DROP INDEX `', REPLACE(@indice, '`', '``'), '`'));
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;

ALTER TABLE notificacion_compra
  MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT,
  MODIFY COLUMN pedido_id VARCHAR(100) NOT NULL;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE()
         AND table_name = 'notificacion_compra' AND index_name = 'uk_compra_tipo'),
  'SELECT 1',
  'ALTER TABLE notificacion_compra ADD CONSTRAINT uk_compra_tipo UNIQUE (pedido_id, tipo)');
PREPARE paso FROM @sql; EXECUTE paso; DEALLOCATE PREPARE paso;
