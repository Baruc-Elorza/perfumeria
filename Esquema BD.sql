use perfumeria;
CREATE table producto(
id_producto int auto_increment primary key
);

CREATE table usuario(
id_usuario BIGINT AUTO_INCREMENT PRIMARY KEY
); 

CREATE TABLE carrito(
id_carrito BIGINT AUTO_INCREMENT PRIMARY KEY,
id_usuario BIGINT NOT NULL,
total decimal(5,2)
);

create table producto_carrito(
id_producto_carrito bigint auto_increment primary key,
fk_usuario bigint,
fk_carrito bigint,
foreign key (fk_usuario) references usuario(id_usuario),
foreign key (fk_carrito) references carrito(id_carrito)
);