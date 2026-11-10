# Sistema de Gestión de Transporte Público

Proyecto Semestral · Programación I · Universidad Tecnológica de Panamá

Aplicación web hecha en **Java** (backend) y **HTML** (frontend) con base de datos **MySQL**. Está basada en el diagrama de clases UML del proyecto: usuarios (pasajero, conductor y administrador), rutas, paradas, horarios, boletos, unidades, turnos, incidencias y reportes.

---

## 1. Qué hay que instalar

| Programa | Para qué sirve | Dónde se descarga |
|---|---|---|
| **JDK 17 o más nuevo** | Compilar y correr Java | adoptium.net (Temurin) |
| **XAMPP** | Trae MySQL y phpMyAdmin (panel para ver la base de datos) | apachefriends.org |
| **NetBeans** (recomendado), IntelliJ IDEA o VS Code | Abrir y correr el proyecto | netbeans.apache.org |

Para saber si ya tienen Java, abrir una terminal (cmd) y escribir `java -version`.

---

## 2. Crear la base de datos

1. Abrir el **XAMPP Control Panel** y darle **Start** a **MySQL** (también a **Apache**, que lo necesita phpMyAdmin).
2. Entrar en el navegador a `http://localhost/phpmyadmin`
3. Arriba, ir a la pestaña **Importar**.
4. En "Archivo a importar" escoger `database/transporte_db.sql` (está en la carpeta del proyecto).
5. Bajar y darle **Importar**.

Al terminar debe aparecer a la izquierda la base de datos **transporte_db** con 12 tablas y datos de prueba.

> Si se necesita empezar de cero (borrar todo lo que se probó), solo se vuelve a importar el mismo archivo.

---

## 3. Correr el proyecto

El proyecto usa **Maven**, que descarga solo el conector de MySQL la primera vez (se necesita internet esa vez).

**NetBeans**
1. File → Open Project → seleccionar la carpeta `transporte-publico`.
2. Clic derecho al proyecto → **Run** (o F6).

**IntelliJ IDEA**
1. File → Open → seleccionar la carpeta `transporte-publico` (la detecta como proyecto Maven).
2. Abrir `src/main/java/transporte/Main.java` y darle al botón verde ▶ al lado de `main`.

**VS Code**
1. Instalar la extensión "Extension Pack for Java".
2. Abrir la carpeta y darle **Run** encima del método `main` en `Main.java`.

Cuando en la consola salga:

```
Servidor iniciado. Abra en el navegador: http://localhost:8080
```

se abre **http://localhost:8080** en el navegador.

### Cuentas de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@transporte.com | admin123 |
| Conductor | conductor1@transporte.com | conductor123 |
| Conductor | conductor2@transporte.com | conductor123 |
| Pasajero | pasajero@transporte.com | pasajero123 |

También se pueden crear pasajeros nuevos desde "Regístrese aquí".

---

## 4. Problemas comunes

| Error en la consola | Causa | Solución |
|---|---|---|
| `Communications link failure` | MySQL no está encendido | Darle Start a MySQL en XAMPP |
| `Unknown database 'transporte_db'` | No se importó el script | Hacer el paso 2 |
| `Access denied for user 'root'` | El MySQL tiene contraseña | Ponerla en `CLAVE` dentro de `datos/Conexion.java` |
| `Address already in use` | Ya hay algo usando el puerto 8080 (o el programa ya está corriendo) | Cerrar la otra ejecución o cambiar `puerto` en `Main.java` |
| No descarga el conector de MySQL | Sin internet o Maven no ha cargado | Clic derecho al proyecto → Build / Reload Maven |

---

## 5. Estructura del proyecto

Cada paquete de Java corresponde a un paquete del diagrama UML.

```
transporte-publico/
├── database/
│   └── transporte_db.sql          Script de la base de datos (tablas + datos de prueba)
├── pom.xml                        Configuración de Maven (conector MySQL)
└── src/main/
    ├── java/transporte/
    │   ├── Main.java              Inicia el servidor web
    │   ├── datos/
    │   │   └── Conexion.java      Conexión a MySQL
    │   ├── usuario/               Usuario (abstracta), Pasajero, Conductor, Administrador, UsuarioDAO
    │   ├── ruta/                  Ruta, Parada, Horario, RutaDAO
    │   ├── boleto/                Boleto, BoletoDAO
    │   ├── unidad/                Unidad, UnidadDAO
    │   ├── turno/                 Turno, TurnoDAO
    │   ├── incidencia/            Incidencia, IncidenciaDAO
    │   ├── reporte/               Reporte, ReporteDAO
    │   └── web/                   Manejadores de cada página (login, pasajero, conductor, admin)
    └── resources/web/
        ├── plantilla.html         Estructura común de todas las páginas
        ├── login.html
        ├── registro.html
        └── estilos.css
```

- Las **clases del UML** (Pasajero, Ruta, Boleto...) tienen los atributos y la lógica (validaciones, cálculos).
- Las clases **DAO** (Data Access Object) son las únicas que hablan con MySQL.
- Las clases de **web** reciben lo que manda el navegador, usan las clases del UML y devuelven HTML.

---

## 6. Subirlo a internet (opcional)

Vercel y Netlify **no sirven** para este proyecto porque solo alojan páginas estáticas o JavaScript; no pueden correr Java ni MySQL. Si se quiere en línea hace falta un servicio que acepte Java y MySQL, por ejemplo **Railway**:

1. Subir el proyecto a GitHub.
2. En Railway crear un proyecto nuevo desde el repositorio y agregarle una base de datos MySQL.
3. Importar `database/transporte_db.sql` en esa base de datos.
4. En las variables del servicio de Java poner `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD` con los datos que da Railway. El programa ya las lee (ver `Conexion.java`), y el puerto lo toma de la variable `PORT`.

Para la presentación lo más seguro es correrlo en la computadora con XAMPP.
