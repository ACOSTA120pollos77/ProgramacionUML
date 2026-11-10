package transporte.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import transporte.usuario.Usuario;

// Herramientas para armar las páginas HTML
public class Pagina {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    // Lee un archivo de la carpeta resources/web
    public static String cargar(String archivo) throws IOException {
        try (InputStream entrada = Pagina.class.getResourceAsStream("/web/" + archivo)) {
            if (entrada == null) {
                throw new IOException("No se encontró el archivo web/" + archivo);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Mete el contenido dentro de plantilla.html
    public static String armar(String titulo, Usuario usuario, String contenido) throws IOException {
        return cargar("plantilla.html")
                .replace("{{titulo}}", esc(titulo))
                .replace("{{usuario}}", esc(usuario.getNombre()))
                .replace("{{rol}}", esc(usuario.getRol().toLowerCase()))
                .replace("{{menu}}", usuario.mostrarMenu())
                .replace("{{contenido}}", contenido);
    }

    // Página de inicio según el rol de quien entra
    public static String inicioDe(Usuario usuario) {
        if (usuario.getRol().equals("PASAJERO")) {
            return "/pasajero";
        } else if (usuario.getRol().equals("CONDUCTOR")) {
            return "/conductor";
        }
        return "/admin";
    }

    // Muestra los mensajes ?ok= o ?error= que llegan en la URL
    public static String avisos(Map<String, String> datos) {
        String html = "";
        if (datos.containsKey("ok")) {
            html += "<p class=\"aviso ok\">" + esc(datos.get("ok")) + "</p>";
        }
        if (datos.containsKey("error")) {
            html += "<p class=\"aviso error\">" + esc(datos.get("error")) + "</p>";
        }
        return html;
    }

    // Evita que alguien meta código HTML o JavaScript en los formularios (XSS)
    public static String esc(Object valor) {
        if (valor == null) {
            return "";
        }
        return valor.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static String opcion(Object valor, String texto, boolean seleccionada) {
        return "<option value=\"" + esc(valor) + "\"" + (seleccionada ? " selected" : "") + ">" + esc(texto) + "</option>";
    }

    public static String dinero(double monto) {
        return String.format("$%.2f", monto);
    }

    public static String fecha(LocalDate fecha) {
        return fecha.format(FECHA);
    }

    public static String fechaHora(LocalDateTime fechaHora) {
        return fechaHora.format(FECHA_HORA);
    }
}
