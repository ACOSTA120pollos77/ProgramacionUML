package transporte.web;

import com.sun.net.httpserver.HttpExchange;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import transporte.boleto.Boleto;
import transporte.boleto.BoletoDAO;
import transporte.ruta.Horario;
import transporte.ruta.Parada;
import transporte.ruta.Ruta;
import transporte.ruta.RutaDAO;
import transporte.usuario.Pasajero;
import transporte.usuario.Usuario;
import transporte.usuario.UsuarioDAO;

// Páginas del pasajero
public class PasajeroHandler extends BaseHandler {

    private final RutaDAO rutaDAO = new RutaDAO();
    private final BoletoDAO boletoDAO = new BoletoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void atender(HttpExchange ex) throws Exception {
        // Solo entra si inició sesión como pasajero
        Usuario usuario = Sesiones.obtener(ex);
        if (!(usuario instanceof Pasajero)) {
            redirigir(ex, "/");
            return;
        }
        Pasajero pasajero = (Pasajero) usuario;
        Map<String, String> datos = Formulario.leer(ex);

        switch (ruta(ex)) {
            case "/pasajero":
                inicio(ex, pasajero, datos);
                break;
            case "/pasajero/rutas":
                rutas(ex, pasajero, datos);
                break;
            case "/pasajero/comprar":
                comprar(ex, pasajero, datos);
                break;
            case "/pasajero/boletos":
                boletos(ex, pasajero, datos);
                break;
            case "/pasajero/recargar":
                recargar(ex, pasajero, datos);
                break;
            default:
                noEncontrado(ex);
        }
    }

    private void inicio(HttpExchange ex, Pasajero p, Map<String, String> datos) throws Exception {
        int activos = 0;
        for (Boleto b : boletoDAO.listarPorPasajero(p.getIdUsuario())) {
            if (b.estaVigente()) {
                activos++;
            }
        }

        String html = Pagina.avisos(datos)
                + "<div class=\"tarjetas\">"
                + "<div class=\"caja\"><small>Saldo disponible</small><p class=\"numero\">" + Pagina.dinero(p.getSaldo()) + "</p></div>"
                + "<div class=\"caja\"><small>Boletos para hoy</small><p class=\"numero\">" + activos + "</p></div>"
                + "</div>"
                + "<div class=\"caja\">"
                + "<h3>Mis datos</h3>"
                + "<p>Cédula: " + Pagina.esc(p.getCedula()) + "<br>Teléfono: " + Pagina.esc(p.getTelefono())
                + "<br>Correo: " + Pagina.esc(p.getCorreo()) + "</p>"
                + "<p><a class=\"boton\" href=\"/pasajero/rutas\">Ver rutas y comprar boleto</a> "
                + "<a class=\"boton claro\" href=\"/pasajero/recargar\">Recargar saldo</a></p>"
                + "</div>";
        enviarHtml(ex, Pagina.armar("Hola, " + p.getNombre(), p, html));
    }

    private void rutas(HttpExchange ex, Pasajero p, Map<String, String> datos) throws Exception {
        String hoy = Horario.diaDeHoy();
        StringBuilder html = new StringBuilder(Pagina.avisos(datos));
        html.append("<p>Saldo: <strong>").append(Pagina.dinero(p.getSaldo())).append("</strong> · Hoy es <strong>")
            .append(hoy).append("</strong></p>");

        List<Ruta> rutas = p.consultarRutas();
        if (rutas.isEmpty()) {
            html.append("<p>No hay rutas activas en este momento.</p>");
        }

        for (Ruta ruta : rutas) {
            html.append("<div class=\"caja\">");
            html.append("<h3>").append(Pagina.esc(ruta.getNombre())).append("</h3>");
            html.append("<p>").append(Pagina.esc(ruta.getOrigen())).append(" → ").append(Pagina.esc(ruta.getDestino()))
                .append(" · Tarifa: <strong>").append(Pagina.dinero(ruta.getTarifa())).append("</strong></p>");

            // Paradas
            html.append("<p><strong>Paradas:</strong></p><ol>");
            for (Parada parada : ruta.obtenerParadas()) {
                html.append("<li>").append(Pagina.esc(parada.getNombre()))
                    .append(" <small>(").append(Pagina.esc(parada.getUbicacion())).append(")</small>");
                if (parada.esTerminal()) {
                    html.append(" <span class=\"etiqueta\">Terminal</span>");
                }
                html.append("</li>");
            }
            html.append("</ol>");

            // Horarios
            html.append("<div class=\"tabla\"><table><tr><th>Salida</th><th>Llegada</th><th>Duración</th><th>Días</th><th></th></tr>");
            for (Horario h : ruta.getHorarios()) {
                html.append("<tr><td>").append(h.getHoraSalida()).append("</td>")
                    .append("<td>").append(h.getHoraLlegada()).append("</td>")
                    .append("<td>").append(h.calcularDuracion()).append(" min</td>")
                    .append("<td>").append(Pagina.esc(h.getDiasOperacion())).append("</td><td>");
                if (h.estaDisponible(hoy)) {
                    html.append("<form method=\"post\" action=\"/pasajero/comprar\">")
                        .append("<input type=\"hidden\" name=\"id_horario\" value=\"").append(h.getIdHorario()).append("\">")
                        .append("<button>Comprar</button></form>");
                } else {
                    html.append("<small>No opera hoy</small>");
                }
                html.append("</td></tr>");
            }
            html.append("</table></div></div>");
        }
        enviarHtml(ex, Pagina.armar("Rutas y horarios", p, html.toString()));
    }

    private void comprar(HttpExchange ex, Pasajero p, Map<String, String> datos) throws Exception {
        if (!esPost(ex)) {
            redirigir(ex, "/pasajero/rutas");
            return;
        }
        Horario horario = rutaDAO.buscarHorario(Formulario.entero(datos, "id_horario"));
        if (horario == null) {
            throw new IllegalArgumentException("El horario no existe.");
        }
        Ruta ruta = rutaDAO.buscar(horario.getIdRuta());
        if (!ruta.isActiva()) {
            throw new IllegalStateException("La ruta no está activa.");
        }

        Boleto boleto;
        try {
            boleto = p.comprarBoleto(horario, ruta.getTarifa());
        } catch (IllegalStateException e) {
            // Si no se pudo comprar, vuelve a la lista de rutas con el motivo
            redirigir(ex, "/pasajero/rutas?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8));
            return;
        }

        try {
            boletoDAO.guardarCompra(boleto, p);
        } catch (SQLException e) {
            p.reembolsar(boleto.getPrecio()); // la base de datos no guardó nada, se devuelve el saldo
            throw e;
        }
        redirigir(ex, "/pasajero/boletos?ok=Boleto+comprado.+C%C3%B3digo%3A+" + boleto.getCodigo());
    }

    private void boletos(HttpExchange ex, Pasajero p, Map<String, String> datos) throws Exception {
        // Anular un boleto
        if (esPost(ex)) {
            Boleto boleto = boletoDAO.buscarPorCodigo(Formulario.texto(datos, "codigo"));
            if (boleto == null || boleto.getIdPasajero() != p.getIdUsuario()) {
                throw new IllegalArgumentException("Boleto no encontrado.");
            }
            boleto.anular();
            p.reembolsar(boleto.getPrecio());
            boletoDAO.guardarAnulacion(boleto, p);
            volver(ex, "Boleto " + boleto.getCodigo() + " anulado. Se devolvieron " + Pagina.dinero(boleto.getPrecio()), false);
            return;
        }

        Map<Integer, String> descripcion = describirHorarios();
        List<Boleto> lista = boletoDAO.listarPorPasajero(p.getIdUsuario());

        StringBuilder html = new StringBuilder(Pagina.avisos(datos));
        if (lista.isEmpty()) {
            html.append("<p>Todavía no ha comprado boletos.</p>");
        } else {
            html.append("<p>Muestre el código al conductor al subir al bus. Los boletos valen solo el día de la compra.</p>");
            html.append("<div class=\"tabla\"><table><tr><th>Código</th><th>Ruta y salida</th><th>Comprado</th>")
                .append("<th>Precio</th><th>Estado</th><th></th></tr>");
            for (Boleto b : lista) {
                html.append("<tr><td><strong>").append(Pagina.esc(b.getCodigo())).append("</strong></td>")
                    .append("<td>").append(Pagina.esc(descripcion.get(b.getIdHorario()))).append("</td>")
                    .append("<td>").append(Pagina.fechaHora(b.getFechaCompra())).append("</td>")
                    .append("<td>").append(Pagina.dinero(b.getPrecio())).append("</td>")
                    .append("<td>").append(b.estadoParaMostrar()).append("</td><td>");
                if (b.estaVigente()) {
                    html.append("<form method=\"post\" action=\"/pasajero/boletos\">")
                        .append("<input type=\"hidden\" name=\"codigo\" value=\"").append(Pagina.esc(b.getCodigo())).append("\">")
                        .append("<button class=\"claro\">Anular</button></form>");
                }
                html.append("</td></tr>");
            }
            html.append("</table></div>");
        }
        enviarHtml(ex, Pagina.armar("Mis boletos", p, html.toString()));
    }

    private void recargar(HttpExchange ex, Pasajero p, Map<String, String> datos) throws Exception {
        if (esPost(ex)) {
            double monto = Formulario.decimal(datos, "monto");
            p.recargarSaldo(monto);
            usuarioDAO.actualizarSaldo(p);
            volver(ex, "Recarga de " + Pagina.dinero(monto) + " realizada. Nuevo saldo: " + Pagina.dinero(p.getSaldo()), false);
            return;
        }

        String html = Pagina.avisos(datos)
                + "<div class=\"caja angosta\">"
                + "<p>Saldo actual: <strong>" + Pagina.dinero(p.getSaldo()) + "</strong></p>"
                + "<form method=\"post\">"
                + "<label>Monto a recargar ($)</label>"
                + "<input type=\"number\" name=\"monto\" min=\"0.50\" max=\"50\" step=\"0.05\" required>"
                + "<button>Recargar</button>"
                + "</form></div>";
        enviarHtml(ex, Pagina.armar("Recargar saldo", p, html));
    }

    // Texto corto para cada horario, ej: "Albrook - San Miguelito (07:00)"
    private Map<Integer, String> describirHorarios() throws Exception {
        Map<Integer, String> mapa = new HashMap<>();
        for (Ruta ruta : rutaDAO.listarTodas()) {
            for (Horario h : ruta.getHorarios()) {
                mapa.put(h.getIdHorario(), ruta.getNombre() + " (" + h.getHoraSalida() + ")");
            }
        }
        return mapa;
    }
}
