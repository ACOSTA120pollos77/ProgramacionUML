package transporte.datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Conexión con la base de datos MySQL
public class Conexion {

    // Valores por defecto de XAMPP: usuario root y sin contraseña
    private static final String HOST = leer("DB_HOST", "localhost");
    private static final String PUERTO = leer("DB_PORT", "3306");
    private static final String BASE = leer("DB_NAME", "transporte_db");
    private static final String USUARIO = leer("DB_USER", "root");
    private static final String CLAVE = leer("DB_PASSWORD", "");

    public static Connection obtener() throws SQLException {
        String url = "jdbc:mysql://" + HOST + ":" + PUERTO + "/" + BASE
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Panama&characterEncoding=UTF-8";
        return DriverManager.getConnection(url, USUARIO, CLAVE);
    }

    // Si el programa se sube a un servidor, los datos se pueden cambiar con variables de entorno
    private static String leer(String variable, String porDefecto) {
        String valor = System.getenv(variable);
        if (valor == null || valor.isEmpty()) {
            return porDefecto;
        }
        return valor;
    }
}
