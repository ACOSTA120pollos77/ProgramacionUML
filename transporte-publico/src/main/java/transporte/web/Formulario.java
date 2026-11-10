package transporte.web;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

// Lee los datos que manda un formulario HTML (y los parámetros de la URL)
public class Formulario {

    public static Map<String, String> leer(HttpExchange ex) throws IOException {
        Map<String, String> datos = new HashMap<>();
        separar(datos, ex.getRequestURI().getRawQuery());

        if (ex.getRequestMethod().equalsIgnoreCase("POST")) {
            String cuerpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            separar(datos, cuerpo);
        }
        return datos;
    }

    // El navegador manda los datos así: nombre=Ana&correo=ana%40mail.com
    private static void separar(Map<String, String> datos, String texto) {
        if (texto == null || texto.isEmpty()) {
            return;
        }
        for (String par : texto.split("&")) {
            String[] partes = par.split("=", 2);
            String clave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
            String valor = partes.length > 1 ? URLDecoder.decode(partes[1], StandardCharsets.UTF_8).trim() : "";
            // Los checkbox con el mismo nombre se juntan con coma (ej: LUN,MAR,MIE)
            datos.merge(clave, valor, (anterior, nuevo) -> anterior + "," + nuevo);
        }
    }

    public static String texto(Map<String, String> datos, String campo) {
        String valor = datos.get(campo);
        if (valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException("Falta llenar el campo: " + campo);
        }
        return valor;
    }

    public static int entero(Map<String, String> datos, String campo) {
        try {
            return Integer.parseInt(texto(datos, campo));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un número entero.");
        }
    }

    public static double decimal(Map<String, String> datos, String campo) {
        try {
            return Double.parseDouble(texto(datos, campo));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un número.");
        }
    }

    public static LocalTime hora(Map<String, String> datos, String campo) {
        try {
            return LocalTime.parse(texto(datos, campo));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La hora no tiene un formato válido.");
        }
    }

    public static LocalDate fecha(Map<String, String> datos, String campo) {
        try {
            return LocalDate.parse(texto(datos, campo));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha no tiene un formato válido.");
        }
    }
}
