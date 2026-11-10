package transporte.web;

import com.sun.net.httpserver.HttpExchange;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import transporte.boleto.Boleto;
import transporte.boleto.BoletoDAO;
import transporte.incidencia.Incidencia;
import transporte.incidencia.IncidenciaDAO;
import transporte.ruta.Ruta;
import transporte.ruta.RutaDAO;
import transporte.turno.Turno;
import transporte.turno.TurnoDAO;
import transporte.unidad.Unidad;
import transporte.unidad.UnidadDAO;
import transporte.usuario.Conductor;
import transporte.usuario.Usuario;

// Páginas del conductor
public class ConductorHandler extends BaseHandler {

    private static final String[] TIPOS_INCIDENCIA = {
        "Falla mecánica", "Accidente", "Retraso", "Problema con pasajero", "Otro"
    };

    private final BoletoDAO boletoDAO = new BoletoDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final IncidenciaDAO incidenciaDAO = new IncidenciaDAO();
    private final RutaDAO rutaDAO = new RutaDAO();
    private final UnidadDAO unidadDAO = new UnidadDAO();

    @Override
    protected void atender(HttpExchange ex) throws Exception {
        Usuario usuario = Sesiones.obtener(ex);
        if (!(usuario instanceof Conductor)) {
            redirigir(ex, "/");
            return;
        }
        Conductor conductor = (Conductor) usuario;
        Map<String, String> datos = Formulario.leer(ex);

        switch (ruta(ex)) {
            case "/conductor":
                inicio(ex, conductor, datos);
                break;
            case "/conductor/turnos":
                turnos(ex, conductor, datos);
                break;
            case "/conductor/validar":
                validar(ex, conductor, datos);
                break;
            case "/conductor/incidencias":
                incidencias(ex, conductor, datos);
                break;
            default:
                noEncontrado(ex);
        }
    }

    private void inicio(HttpExchange ex, Conductor c, Map<String, String> datos) throws Exception {
        // Busca si tiene turno hoy
        Turno turnoHoy = null;
        for (Turno t : c.consultarTurnos()) {
            if (t.getFecha().equals(LocalDate.now())) {
                turnoHoy = t;
            }
        }

        String html = Pagina.avisos(datos)
                + "<div class=\"tarjetas\">"
                + "<div class=\"caja\"><small>Licencia</small><p class=\"numero\">" + Pagina.esc(c.getNumeroLicencia()) + "</p>"
                + "<small>" + Pagina.esc(c.getTipoLicencia()) + "</small></div>"
                + "<div class=\"caja\"><small>Estado</small><p class=\"numero\">" + (c.isDisponible() ? "Disponible" : "No disponible") + "</p></div>"
                + "</div><div class=\"caja\"><h3>Turno de hoy</h3>";

        if (turnoHoy == null) {
            html += "<p>No tiene turno asignado para hoy.</p>";
        } else {
            Ruta ruta = rutaDAO.buscar(turnoHoy.getIdRuta());
            Unidad unidad = unidadDAO.buscar(turnoHoy.getIdUnidad());
            html += "<p>Ruta: <strong>" + Pagina.esc(ruta.getNombre()) + "</strong><br>"
                    + "Unidad: <strong>" + Pagina.esc(unidad.getPlaca()) + "</strong> (" + Pagina.esc(unidad.getModelo()) + ")<br>"
                    + "Inicio: " + turnoHoy.getHoraInicio()
                    + (turnoHoy.estaFinalizado() ? " · Finalizó: " + turnoHoy.getHoraFin() : " · En curso") + "</p>";
        }
        html += "<p><a class=\"boton\" href=\"/conductor/validar\">Validar boleto</a> "
                + "<a class=\"boton claro\" href=\"/conductor/incidencias\">Reportar incidencia</a></p></div>";

        enviarHtml(ex, Pagina.armar("Hola, " + c.getNombre(), c, html));
    }

    private void turnos(HttpExchange ex, Conductor c, Map<String, String> datos) throws Exception {
        // Finalizar turno
        if (esPost(ex)) {
            Turno turno = turnoDAO.buscar(Formulario.entero(datos, "id_turno"));
            if (turno == null || turno.getIdConductor() != c.getIdUsuario()) {
                throw new IllegalArgumentException("Turno no encontrado.");
            }
            turno.finalizar();
            turnoDAO.guardarFinalizacion(turno);
            volver(ex, String.format("Turno finalizado. Horas trabajadas: %.2f", turno.calcularHoras()), false);
            return;
        }

        Map<Integer, String> rutas = nombresDeRutas();
        Map<Integer, String> placas = placasDeUnidades();
        List<Turno> lista = c.consultarTurnos();

        StringBuilder html = new StringBuilder(Pagina.avisos(datos));
        if (lista.isEmpty()) {
            html.append("<p>No tiene turnos asignados.</p>");
        } else {
            html.append("<div class=\"tabla\"><table><tr><th>Fecha</th><th>Ruta</th><th>Unidad</th>")
                .append("<th>Inicio</th><th>Fin</th><th>Horas</th><th></th></tr>");
            for (Turno t : lista) {
                html.append("<tr><td>").append(Pagina.fecha(t.getFecha())).append("</td>")
                    .append("<td>").append(Pagina.esc(rutas.get(t.getIdRuta()))).append("</td>")
                    .append("<td>").append(Pagina.esc(placas.get(t.getIdUnidad()))).append("</td>")
                    .append("<td>").append(t.getHoraInicio()).append("</td>")
                    .append("<td>").append(t.estaFinalizado() ? t.getHoraFin().toString() : "-").append("</td>")
                    .append("<td>").append(String.format("%.2f", t.calcularHoras())).append("</td><td>");
                if (!t.estaFinalizado() && t.getFecha().equals(LocalDate.now())) {
                    html.append("<form method=\"post\"><input type=\"hidden\" name=\"id_turno\" value=\"")
                        .append(t.getIdTurno()).append("\"><button>Finalizar</button></form>");
                }
                html.append("</td></tr>");
            }
            html.append("</table></div>");
        }
        enviarHtml(ex, Pagina.armar("Mis turnos", c, html.toString()));
    }

    private void validar(HttpExchange ex, Conductor c, Map<String, String> datos) throws Exception {
        if (esPost(ex)) {
            String codigo = Formulario.texto(datos, "codigo").toUpperCase();
            Boleto boleto = boletoDAO.buscarPorCodigo(codigo);
            if (boleto == null) {
                throw new IllegalArgumentException("No existe ningún boleto con el código " + codigo);
            }
            if (c.validarBoleto(boleto)) {
                boletoDAO.actualizarEstado(boleto); // queda como USADO
                volver(ex, "Boleto " + codigo + " válido. El pasajero puede abordar.", false);
            } else {
                volver(ex, "Boleto " + codigo + " NO válido (estado: " + boleto.estadoParaMostrar() + ").", true);
            }
            return;
        }

        String html = Pagina.avisos(datos)
                + "<div class=\"caja angosta\">"
                + "<form method=\"post\">"
                + "<label>Código del boleto</label>"
                + "<input type=\"text\" name=\"codigo\" placeholder=\"BOL-XXXXXX\" required autofocus>"
                + "<button>Validar</button>"
                + "</form></div>";
        enviarHtml(ex, Pagina.armar("Validar boleto", c, html));
    }

    private void incidencias(HttpExchange ex, Conductor c, Map<String, String> datos) throws Exception {
        if (esPost(ex)) {
            Unidad unidad = unidadDAO.buscar(Formulario.entero(datos, "id_unidad"));
            if (unidad == null) {
                throw new IllegalArgumentException("Seleccione una unidad válida.");
            }
            Incidencia incidencia = c.registrarIncidencia(Formulario.texto(datos, "tipo"),
                    Formulario.texto(datos, "descripcion"));
            incidencia.setIdUnidad(unidad.getIdUnidad());
            incidenciaDAO.insertar(incidencia);
            volver(ex, "Incidencia registrada. El administrador la revisará.", false);
            return;
        }

        Map<Integer, String> placas = placasDeUnidades();

        // Formulario
        StringBuilder html = new StringBuilder(Pagina.avisos(datos));
        html.append("<div class=\"caja angosta\"><h3>Reportar incidencia</h3><form method=\"post\">")
            .append("<label>Tipo</label><select name=\"tipo\">");
        for (String tipo : TIPOS_INCIDENCIA) {
            html.append(Pagina.opcion(tipo, tipo, false));
        }
        html.append("</select><label>Unidad</label><select name=\"id_unidad\">");
        for (Unidad u : unidadDAO.listar()) {
            html.append(Pagina.opcion(u.getIdUnidad(), u.getPlaca() + " - " + u.getModelo(), false));
        }
        html.append("</select><label>Descripción</label>")
            .append("<textarea name=\"descripcion\" rows=\"3\" maxlength=\"300\" required></textarea>")
            .append("<button>Registrar</button></form></div>");

        // Las que ya reportó
        html.append("<h3>Mis incidencias</h3>");
        List<Incidencia> lista = incidenciaDAO.listarPorConductor(c.getIdUsuario());
        if (lista.isEmpty()) {
            html.append("<p>No ha reportado incidencias.</p>");
        } else {
            html.append("<div class=\"tabla\"><table><tr><th>Fecha</th><th>Tipo</th><th>Unidad</th><th>Descripción</th><th>Estado</th></tr>");
            for (Incidencia i : lista) {
                html.append("<tr><td>").append(Pagina.fechaHora(i.getFecha())).append("</td>")
                    .append("<td>").append(Pagina.esc(i.getTipo())).append("</td>")
                    .append("<td>").append(Pagina.esc(placas.get(i.getIdUnidad()))).append("</td>")
                    .append("<td>").append(Pagina.esc(i.getDescripcion())).append("</td>")
                    .append("<td>").append(i.getEstado()).append("</td></tr>");
            }
            html.append("</table></div>");
        }
        enviarHtml(ex, Pagina.armar("Incidencias", c, html.toString()));
    }

    private Map<Integer, String> nombresDeRutas() throws Exception {
        Map<Integer, String> mapa = new HashMap<>();
        for (Ruta r : rutaDAO.listarTodas()) {
            mapa.put(r.getIdRuta(), r.getNombre());
        }
        return mapa;
    }

    private Map<Integer, String> placasDeUnidades() throws Exception {
        Map<Integer, String> mapa = new HashMap<>();
        for (Unidad u : unidadDAO.listar()) {
            mapa.put(u.getIdUnidad(), u.getPlaca());
        }
        return mapa;
    }
}
