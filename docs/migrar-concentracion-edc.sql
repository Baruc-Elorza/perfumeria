-- Sustituye el valor anterior EDF por EDC sin eliminar perfumes.
ALTER TABLE perfume MODIFY COLUMN concentracion
  ENUM('EDC','EDF','EDT','EDP','PARFUM') NULL;
UPDATE perfume SET concentracion = 'EDC' WHERE concentracion = 'EDF';
ALTER TABLE perfume MODIFY COLUMN concentracion
  ENUM('EDC','EDT','EDP','PARFUM') NULL;
