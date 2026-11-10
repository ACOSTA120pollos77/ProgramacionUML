-- ==========================================================
-- Sistema de Gestión de Transporte Público
-- Base de datos: transporte_db
-- ==========================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS transporte_db CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE transporte_db;

DROP TABLE IF EXISTS reporte;
DROP TABLE IF EXISTS incidencia;
DROP TABLE IF EXISTS turno;
DROP TABLE IF EXISTS boleto;
DROP TABLE IF EXISTS unidad;
DROP TABLE IF EXISTS horario;
DROP TABLE IF EXISTS parada;
DROP TABLE IF EXISTS ruta;
DROP TABLE IF EXISTS administrador;
DROP TABLE IF EXISTS conductor;
DROP TABLE IF EXISTS pasajero;
DROP TABLE IF EXISTS usuario;

-- ---------- Usuarios (clase padre y sus hijas) ----------

CREATE TABLE usuario (
    id_usuario  INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    correo      VARCHAR(100) NOT NULL UNIQUE,
    contrasena  VARCHAR(64)  NOT NULL,
    rol         VARCHAR(20)  NOT NULL
);

CREATE TABLE pasajero (
    id_usuario  INT PRIMARY KEY,
    cedula      VARCHAR(20) NOT NULL,
    telefono    VARCHAR(20),
    saldo       DECIMAL(10,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

CREATE TABLE conductor (
    id_usuario       INT PRIMARY KEY,
    numero_licencia  VARCHAR(30) NOT NULL,
    tipo_licencia    VARCHAR(20) NOT NULL,
    disponible       BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

CREATE TABLE administrador (
    id_usuario  INT PRIMARY KEY,
    cargo       VARCHAR(60) NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
);

-- ---------- Rutas, paradas y horarios ----------

CREATE TABLE ruta (
    id_ruta  INT AUTO_INCREMENT PRIMARY KEY,
    nombre   VARCHAR(100) NOT NULL,
    origen   VARCHAR(100) NOT NULL,
    destino  VARCHAR(100) NOT NULL,
    tarifa   DECIMAL(6,2) NOT NULL,
    activa   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE parada (
    id_parada  INT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    ubicacion  VARCHAR(150),
    orden      INT NOT NULL,
    id_ruta    INT NOT NULL,
    FOREIGN KEY (id_ruta) REFERENCES ruta(id_ruta) ON DELETE CASCADE
);

CREATE TABLE horario (
    id_horario      INT AUTO_INCREMENT PRIMARY KEY,
    hora_salida     TIME NOT NULL,
    hora_llegada    TIME NOT NULL,
    dias_operacion  VARCHAR(40) NOT NULL,
    id_ruta         INT NOT NULL,
    FOREIGN KEY (id_ruta) REFERENCES ruta(id_ruta)
);

-- ---------- Unidades (buses) ----------

CREATE TABLE unidad (
    id_unidad  INT AUTO_INCREMENT PRIMARY KEY,
    placa      VARCHAR(15) NOT NULL UNIQUE,
    modelo     VARCHAR(60) NOT NULL,
    capacidad  INT NOT NULL,
    estado     VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    id_ruta    INT NULL,
    FOREIGN KEY (id_ruta) REFERENCES ruta(id_ruta)
);

-- ---------- Boletos ----------

CREATE TABLE boleto (
    id_boleto     INT AUTO_INCREMENT PRIMARY KEY,
    codigo        VARCHAR(12) NOT NULL UNIQUE,
    fecha_compra  DATETIME NOT NULL,
    precio        DECIMAL(6,2) NOT NULL,
    estado        VARCHAR(15) NOT NULL,
    id_horario    INT NOT NULL,
    id_pasajero   INT NOT NULL,
    FOREIGN KEY (id_horario) REFERENCES horario(id_horario),
    FOREIGN KEY (id_pasajero) REFERENCES pasajero(id_usuario)
);

-- ---------- Turnos ----------

CREATE TABLE turno (
    id_turno      INT AUTO_INCREMENT PRIMARY KEY,
    fecha         DATE NOT NULL,
    hora_inicio   TIME NOT NULL,
    hora_fin      TIME NULL,
    id_ruta       INT NOT NULL,
    id_conductor  INT NOT NULL,
    id_unidad     INT NOT NULL,
    FOREIGN KEY (id_ruta) REFERENCES ruta(id_ruta),
    FOREIGN KEY (id_conductor) REFERENCES conductor(id_usuario),
    FOREIGN KEY (id_unidad) REFERENCES unidad(id_unidad)
);

-- ---------- Incidencias ----------

CREATE TABLE incidencia (
    id_incidencia  INT AUTO_INCREMENT PRIMARY KEY,
    fecha          DATETIME NOT NULL,
    tipo           VARCHAR(40) NOT NULL,
    descripcion    VARCHAR(300) NOT NULL,
    estado         VARCHAR(15) NOT NULL,
    id_unidad      INT NOT NULL,
    id_conductor   INT NOT NULL,
    FOREIGN KEY (id_unidad) REFERENCES unidad(id_unidad),
    FOREIGN KEY (id_conductor) REFERENCES conductor(id_usuario)
);

-- ---------- Reportes ----------

CREATE TABLE reporte (
    id_reporte        INT AUTO_INCREMENT PRIMARY KEY,
    tipo              VARCHAR(10) NOT NULL,
    fecha_generacion  DATE NOT NULL,
    contenido         TEXT NOT NULL,
    id_ruta           INT NULL,
    id_unidad         INT NULL,
    FOREIGN KEY (id_ruta) REFERENCES ruta(id_ruta),
    FOREIGN KEY (id_unidad) REFERENCES unidad(id_unidad)
);


-- ==========================================================
-- Datos de prueba
-- Las contraseñas se guardan encriptadas con SHA-256
-- ==========================================================

INSERT INTO usuario (nombre, correo, contrasena, rol) VALUES
('Marta Quintero',   'admin@transporte.com',      SHA2('admin123', 256),     'ADMINISTRADOR'),
('Luis Herrera',     'conductor1@transporte.com', SHA2('conductor123', 256), 'CONDUCTOR'),
('Roberto Castillo', 'conductor2@transporte.com', SHA2('conductor123', 256), 'CONDUCTOR'),
('Ana Rodríguez',    'pasajero@transporte.com',   SHA2('pasajero123', 256),  'PASAJERO');

INSERT INTO administrador (id_usuario, cargo) VALUES (1, 'Jefa de Operaciones');

INSERT INTO conductor (id_usuario, numero_licencia, tipo_licencia, disponible) VALUES
(2, 'LIC-458921', 'Tipo E2', TRUE),
(3, 'LIC-702314', 'Tipo E2', TRUE);

INSERT INTO pasajero (id_usuario, cedula, telefono, saldo) VALUES
(4, '8-945-1203', '6612-3344', 10.00);

INSERT INTO ruta (nombre, origen, destino, tarifa, activa) VALUES
('Albrook - San Miguelito',     'Terminal de Albrook', 'San Miguelito',  0.35, TRUE),
('Corredor Sur - Tocumen',      'Terminal de Albrook', 'Tocumen',        1.25, TRUE),
('Vía España - Costa del Este', 'Plaza 5 de Mayo',     'Costa del Este', 0.35, TRUE);

INSERT INTO parada (nombre, ubicacion, orden, id_ruta) VALUES
('Terminal de Albrook', 'Albrook Mall',          1, 1),
('El Ingenio',          'Vía Ricardo J. Alfaro', 2, 1),
('Los Andes',           'Centro Comercial Los Andes', 3, 1),
('San Miguelito',       'Estación de San Miguelito',  4, 1),

('Terminal de Albrook', 'Albrook Mall',          1, 2),
('Atlapa',              'Corredor Sur',          2, 2),
('Costa del Este',      'Salida Corredor Sur',   3, 2),
('Tocumen',             'Aeropuerto de Tocumen', 4, 2),

('Plaza 5 de Mayo',     'Avenida Central',       1, 3),
('Vía España',          'El Cangrejo',           2, 3),
('Paitilla',            'Punta Paitilla',        3, 3),
('Costa del Este',      'Town Center',           4, 3);

INSERT INTO horario (hora_salida, hora_llegada, dias_operacion, id_ruta) VALUES
('05:30', '06:15', 'LUN,MAR,MIE,JUE,VIE',         1),
('07:00', '07:50', 'LUN,MAR,MIE,JUE,VIE,SAB,DOM', 1),
('17:30', '18:25', 'LUN,MAR,MIE,JUE,VIE,SAB,DOM', 1),
('06:00', '06:40', 'LUN,MAR,MIE,JUE,VIE,SAB,DOM', 2),
('12:00', '12:40', 'LUN,MAR,MIE,JUE,VIE',         2),
('08:00', '08:35', 'LUN,MAR,MIE,JUE,VIE,SAB,DOM', 3),
('18:00', '18:45', 'SAB,DOM',                     3);

INSERT INTO unidad (placa, modelo, capacidad, estado, id_ruta) VALUES
('BA-1234', 'Volvo B8R',            80, 'DISPONIBLE',    1),
('BA-5678', 'Mercedes-Benz OC 500', 80, 'DISPONIBLE',    2),
('BA-9012', 'Yutong ZK6128',        70, 'MANTENIMIENTO', NULL),
('BA-3456', 'Volvo B8R',            80, 'DISPONIBLE',    3);

INSERT INTO turno (fecha, hora_inicio, hora_fin, id_ruta, id_conductor, id_unidad) VALUES
(CURDATE(), '05:00', NULL, 1, 2, 1),
(CURDATE(), '06:00', NULL, 2, 3, 2);

INSERT INTO boleto (codigo, fecha_compra, precio, estado, id_horario, id_pasajero) VALUES
('BOL-A7K2M9', NOW(), 0.35, 'ACTIVO', 2, 4);

INSERT INTO incidencia (fecha, tipo, descripcion, estado, id_unidad, id_conductor) VALUES
(NOW(), 'Falla mecánica', 'El aire acondicionado dejó de funcionar a mitad del recorrido.', 'ABIERTA', 3, 3);
