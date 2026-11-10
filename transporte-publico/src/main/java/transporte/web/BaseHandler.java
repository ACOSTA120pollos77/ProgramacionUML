package transporte.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// Lo que comparten todos los manejadores: responder HTML, redirigir y manejar errores
public abstract class BaseHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            atender(ex);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Errores de validación: se vuelve al formulario con el mensaje
            if (esPost(ex)) {
                volver(ex, e.getMessage(), true);
            } else {
                enviar(ex, 400, paginaError(e.getMessage()), "text/html; charset=UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
            enviar(ex, 500, paginaError("Ocurrió un error en el servidor: " + e.getMessage()), "text/html; charset=UTF-8");
        } finally {
            ex.close();
        }
    }

    protected abstract void atender(HttpExchange ex) throws Exception;

    protected void enviarHtml(HttpExchange ex, String html) throws IOException {
        enviar(ex, 200, html, "text/html; charset=UTF-8");
    }

    protected void enviar(HttpExchange ex, int codigo, String cuerpo, String tipo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream salida = ex.getResponseBody()) {
            salida.write(bytes);
        }
    }

    protected void redirigir(HttpExchange ex, String destino) throws IOException {
        ex.getResponseHeaders().set("Location", destino);
        ex.sendResponseHeaders(303, -1);
    }

    // Regresa a la misma página mostrando un mensaje de éxito o de error
    protected void volver(HttpExchange ex, String mensaje, boolean esError) throws IOException {
        String destino = ex.getRequestURI().getPath();
        String consulta = ex.getRequestURI().getRawQuery();
        destino += (consulta == null) ? "?" : "?" + consulta + "&";
        destino += (esError ? "error=" : "ok=") + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
        redirigir(ex, destino);
    }

    protected void noEncontrado(HttpExchange ex) throws IOException {
        enviar(ex, 404, paginaError("Página no encontrada."), "text/html; charset=UTF-8");
    }

    protected String ruta(HttpExchange ex) {
        String ruta = ex.getRequestURI().getPath();
        if (ruta.length() > 1 && ruta.endsWith("/")) {
            ruta = ruta.substring(0, ruta.length() - 1);
        }
        return ruta;
    }

    protected boolean esPost(HttpExchange ex) {
        return ex.getRequestMethod().equalsIgnoreCase("POST");
    }

    private String paginaError(String mensaje) {
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"><title>Error</title>"
                + "<link rel=\"stylesheet\" href=\"/estilos.css\"></head><body><main>"
                + "<h1>Algo salió mal</h1><p class=\"aviso error\">" + Pagina.esc(mensaje) + "</p>"
                + "<p><a href=\"/\">Volver al inicio</a></p></main></body></html>";
    }
}
