package transporte.web;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import transporte.usuario.Usuario;

// Guarda quién inició sesión. El navegador solo recibe un código (cookie) que identifica la sesión.
public class Sesiones {

    private static final String COOKIE = "SESION";
    private static final Map<String, Usuario> activas = new ConcurrentHashMap<>();

    public static void crear(HttpExchange ex, Usuario usuario) {
        String codigo = UUID.randomUUID().toString();
        activas.put(codigo, usuario);
        ex.getResponseHeaders().add("Set-Cookie", COOKIE + "=" + codigo + "; Path=/; HttpOnly; SameSite=Lax");
    }

    public static Usuario obtener(HttpExchange ex) {
        String codigo = leerCookie(ex);
        return codigo == null ? null : activas.get(codigo);
    }

    public static void cerrar(HttpExchange ex) {
        String codigo = leerCookie(ex);
        if (codigo != null) {
            activas.remove(codigo);
        }
        ex.getResponseHeaders().add("Set-Cookie", COOKIE + "=; Path=/; Max-Age=0");
    }

    private static String leerCookie(HttpExchange ex) {
        String cookies = ex.getRequestHeaders().getFirst("Cookie");
        if (cookies == null) {
            return null;
        }
        for (String cookie : cookies.split(";")) {
            String[] partes = cookie.trim().split("=", 2);
            if (partes.length == 2 && partes[0].equals(COOKIE)) {
                return partes[1];
            }
        }
        return null;
    }
}
