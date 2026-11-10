package transporte.web;

import com.sun.net.httpserver.HttpExchange;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import transporte.incidencia.Incidencia;
import transporte.incidencia.IncidenciaDAO;
import transporte.reporte.Reporte;
import transporte.reporte.ReporteDAO;
import transporte.ruta.Horario;
import transporte.ruta.Parada;
import transporte.ruta.Ruta;
import transporte.ruta.RutaDAO;
import transporte.turno.Turno;
import transporte.turno.TurnoDAO;
import transporte.unidad.Unidad;
import transporte.unidad.UnidadDAO;
import transporte.usuario.Administrador;
import transporte.usuario.Conductor;
import transporte.usuario.Usuario;
import transporte.usuario.UsuarioDAO;

// Páginas del administrador
public class AdminHandler extends BaseHandler {

    private static final String[] ESTADOS_UNIDAD = {"DISPONIBLE", "EN RUTA", "MANTENIMIENTO"};

    private final RutaDAO rutaDAO = new RutaDAO();
    private final UnidadDAO unidadDAO = new UnidadDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final IncidenciaDAO incidenciaDAO = new IncidenciaDAO();
    private final ReporteDAO reporteDAO = new ReporteDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void atender(HttpExchange ex) throws Exception {
        Usuario usuario = Sesiones.obtener(ex);
        if (!(usuario instanceof Administrador)) {
            redirigir(ex, "/");
            return;
        }
        Administrador admin = (Administrador) usuario;
        Map<String, String> datos = Formulario.leer(ex);

        switch (ruta(ex)) {
            case "/admin":
                inicio(ex, admin, datos);
                break;
            case "/admin/rutas":
                rutas(ex, admin, datos);
                break;
            case "/admin/ruta":
                detalleRuta(ex, admin, datos);
                break;
            case "/admin/unidades":
                unidades(ex, admin, datos);
                break;
            case "/admin/turnos":
                turnos(ex, admin, datos);
                break;
            case "/admin/incidencias":
                incidencias(ex, admin, datos);
                break;
            case "/admin/reportes":
                reportes(ex, admin, datos);
                break;
            case "/admin/reporte":
                verReporte(ex, admin, datos);
                break;
            case "/admin/exportar":
                exportarReporte(ex, datos);
                break;
            default:
                noEncontrado(ex);
        }
    }

    // ===================== Inicio =====================

    private void inicio(HttpExchange ex, Administrador admin, Map<String, String> datos) throws Exception {
        int rutasActivas = rutaDAO.listarActivas().size();

        int unidadesDisponibles = 0;
        List<Unidad> unidades = unidadDAO.listar();
        for (Unidad u : unidades) {
            if (u.estaDisponible()) {
                unidadesDisponibles++;
            }
        }

        int turnosHoy = 0;
        for (Turno t : turnoDAO.listar()) {
            if (t.getFecha().equals(LocalDate.now())) {
                turnosHoy++;
            }
        }

        int abiertas = 0;
        for (Incidencia i : incidenciaDAO.listar()) {
            if (!i.getEstado().equals("CERRADA")) {
                abiertas++;
            }
        }

        String html = Pagina.avisos(datos)
                + "<p>" + Pagina.esc(admin.getCargo()) + "</p>"
                + "<div class=\"tarjetas\">"
                + tarjeta("Rutas activas", String.valueOf(rutasActivas), "/admin/rutas")
                + tarjeta("Unidades disponibles", unidadesDisponibles + " de " + unidades.size(), "/admin/unidades")
                + tarjeta("Turnos de hoy", String.valueOf(turnosHoy), "/admin/turnos")
                + tarjeta("Incidencias pendientes", String.valueOf(abiertas), "/admin/incidencias")
                + "</div>";
        enviarHtml(ex, Pagina.armar("Panel de administración", admin, html));
    }

    private String tarjeta(String titulo, String valor, String enlace) {
        return "<div class=\"caja\"><small>" + titulo + "</small><p class=\"numero\">" + valor + "</p>"
                + "<a href=\"" + enlace + "\">Ver más</a></div>";
    }

    // ===================== Rutas =====================

    private void rutas(HttpExchange ex, Administrador admin, Map<String, String> datos) throws Exception {
        if (esPost(ex)) {
            String accion = Formulario.texto(datos, "accion");

            if (accion.equals("nueva")) {
                Ruta ruta = new Ruta(0, Formulario.texto(datos, "nombre"), Formulario.texto(datos, "origen"),
                        Formulario.texto(datos, "destino"), Formulario.decimal(datos, "tarifa"), false);
                admin.registrarRuta(ruta);
                rutaDAO.insertar(ruta);
                redirigir(ex, "/admin/ruta?id=" + ruta.getIdRuta() + "&ok=Ruta+creada.+Ahora+agregue+sus+paradas+y+horarios.");

            } else if (accion.equals("estado")) {
                Ruta ruta = buscarRuta(Formulario.entero(datos, "id_ruta"));
                ruta.cambiarEstado(!ruta.isActiva());
                rutaDAO.actualizarEstado(ruta);
                volver(ex, "La ruta " + ruta.getNombre() + " ahora está " + (ruta.isActiva() ? "activa." : "inactiva."), false);
            }
            return;
        }

        StringBuilder html = new StringBuilder(Pagina.avisos(datos));
        html.append("<div class=\"tabla\"><table><tr><th>Nombre</th><th>Recorrido</th><th>Tarifa</th>")
            .append("<th>Paradas</th><th>Horarios</th><th>Estado</th><th></th></tr>");

        for (Ruta r : rutaDAO.listarTodas()) {
            html.append("<tr><td><a href=\"/admin/ruta?id=").append(r.getIdRuta()).append("\">")
                .append(Pagina.esc(r.getNombre())).append("</a>");
            if (!r.estaCompleta()) {
                html.append(" <span class=\"etiqueta\">Incompleta</span>");
            }
            html.append("</td><td>").append(Pagina.esc(r.getOrigen())).append(" → ").append(Pagina.esc(r.getDestino())).append("</td>")
                .append("<td>").append(Pagina.dinero(r.getTarifa())).append("</td>")
                .append("<td>").append(r.obtenerParadas().size()).append("</td>")
                .append("<td>").append(r.getHorarios().size()).append("</td>")
                .append("<td>").append(r.isActiva() ? "Activa" : "Inactiva").append("</td>")
                .append("<td><form method=\"post\"><input type=\"hidden\" name=\"accion\" value=\"estado\">")
                .append("<input type=\"hidden\" name=\"id_ruta\" value=\"").append(r.getIdRuta()).append("\">")
                .append("<button class=\"claro\">").append(r.isActiva() ? "Desactivar" : "Activar").append("</button></form></td></tr>");
        }
        html.append("</table></div>");

        html.append("<div class=\"caja angosta\"><h3>Nueva ruta</h3><form method=\"post\">")
            .append("<input type=\"hidden\" name=\"accion\" value=\"nueva\">")
            .append("<label>Nombre</label><input name=\"nombre\" required>")
            .append("<label>Origen</label><input name=\"origen\" required>")
            .append("<label>Destino</label><input name=\"destino\" required>")
            .append("<label>Tarifa ($)</label><input type=\"number\" name=\"tarifa\" step=\"0.05\" min=\"0.05\" max=\"10\" required>")
            .append("<button>Registrar ruta</button></form></div>");

        enviarHtml(ex, Pagina.armar("Rutas", admin, html.toString()));
    }

    private void detalleRuta(HttpExchange ex, Administrador admin, Map<String, String> datos) throws Exception {
        Ruta ruta = buscarRuta(Formulario.entero(datos, "id"));

        if (esPost(ex)) {
            String accion = Formulario.texto(datos, "accion");

            if (accion.equals("parada")) {
                Parada parada = new Parada(0, Formulario.texto(datos, "nombre"), Formulario.texto(datos, "ubicacion"),
                        Formulario.entero(datos, "orden"), ruta.getIdRuta());
                for (Parada existente : ruta.obtenerParadas()) {
                    if (existente.getOrden() == parada.getOrden()) {
                        throw new IllegalArgumentException("Ya hay una parada con el orden " + parada.getOrden());
                    }
                }
                ruta.agregarParada(parada);
                rutaDAO.insertarParada(parada);
                volver(ex, "Parada agregada.", false);

            } else if (accion.equals("horario")) {
                String dias = datos.get("dias");
                if (dias == null || dias.isEmpty()) {
                    throw new IllegalArgumentException("Seleccione al menos un día de operación.");
                }
                Horario horario = new Horario(0, Formulario.hora(datos, "salida"), Formulario.hora(datos, "llegada"),
                        dias, ruta.getIdRuta());
                ruta.agregarHorario(horario);
                rutaDAO.insertarHorario(horario);
                volver(ex, "Horario agregado.", false);
            }
            return;
        }

        StringBuilder html = new StringBuilder(Pagina.avisos(datos));
        html.append("<p>").append(Pagina.esc(ruta.getOrigen())).append(" → ").append(Pagina.esc(ruta.getDestino()))
            .append(" · Tarifa ").append(Pagina.dinero(ruta.getTarifa()))
            .append(" · ").append(ruta.isActiva() ? "Activa" : "Inactiva").append("</p>");
        if (!ruta.estaCompleta()) {
            html.append("<p class=\"aviso error\">Una ruta necesita mínimo 2 paradas y 1 horario.</p>");
        }

        // Paradas
        html.append("<div class=\"columnas\"><div class=\"caja\"><h3>Paradas</h3><ol>");
        for (Parada p : ruta.obtenerParadas()) {
            html.append("<li>").append(Pagina.esc(p.getNombre())).append(" <small>(")
                .append(Pagina.esc(p.getUbicacion())).append(")</small></li>");
        }
        int siguiente = ruta.obtenerParadas().size() + 1;
        html.append("</ol><form method=\"post\" action=\"/admin/ruta?id=").append(ruta.getIdRuta()).append("\">")
            .append("<input type=\"hidden\" name=\"accion\" value=\"parada\">")
            .append("<label>Nombre</label><input name=\"nombre\" required>")
            .append("<label>Ubicación</label><input name=\"ubicacion\" required>")
            .append("<label>Orden</label><input type=\"number\" name=\"orden\" min=\"1\" value=\"").append(siguiente).append("\" required>")
            .append("<button>Agregar parada</button></form></div>");

        // Horarios
        html.append("<div class=\"caja\"><h3>Horarios</h3><table><tr><th>Salida</th><th>Llegada</th><th>Días</th></tr>");
        for (Horario h : ruta.getHorarios()) {
            html.append("<tr><td>").append(h.getHoraSalida()).append("</td><td>").append(h.getHoraLlegada())
                .append("</td><td>").append(Pagina.esc(h.getDiasOperacion())).append("</td></tr>");
        }
        html.append("</table><form method=\"post\" action=\"/admin/ruta?id=").append(ruta.getIdRuta()).append("\">")
            .append("<input type=\"hidden\" name=\"accion\" value=\"horario\">")
            .append("<label>Salida</label><input type=\"time\" name=\"salida\" required>")
            .append("<label>Llegada</label><input type=\"time\" name=\"llegada\" required>")
            .append("<label>Días</label><p>");
        for (String dia : Horario.DIAS) {
            html.append("<label class=\"check\"><input type=\"checkbox\" name=\"dias\" value=\"").append(dia).append("\"> ")
                .append(dia).append("</label> ");
        }
        html.append("</p><button>Agregar horario</button></form></div></div>");
        html.append("<p><a href=\"/admin/rutas\">← Volver a rutas</a></p>");

        enviarHtml(ex, Pagina.armar(ruta.getNombre(), admin, html.toString()));
    }

    // ===================== Unidades =====================

    private void unidades(HttpExchange ex, Administrador admin, Map<String, String> datos) throws Exception {
        if (esPost(ex)) {
            String accion = Formulario.texto(datos, "accion");

            if (accion.equals("nueva")) {
                String placa = Formulario.texto(datos, "placa").toUpperCase();
                if (unidadDAO.existePlaca(placa)) {
                    throw new IllegalArgumentException("Ya existe una unidad con la placa " + placa);
                }
                int capacidad = Formulario.entero(datos, "capacidad");
                if (capacidad < 10 || capacidad > 150) {
                    throw new IllegalArgumentException("La capacidad debe estar entre 10 y 150 pasajeros.");
                }
                unidadDAO.insertar(new Unidad(0, placa, Formulario.texto(datos, "modelo"), capacidad, "DISPONIBLE", 0));
                volver(ex, "Unidad " + placa + " registrada.", false);

            } else if (accion.equals("asignar")) {
                Unidad unidad = buscarUnidad(Formulario.entero(datos, "id_unidad"));
                Ruta ruta = buscarRuta(Formulario.entero(datos, "id_ruta"));
                admin.asignarUnidad(unidad, ruta);
                unidadDAO.actualizar(unidad);
                volver(ex, "Unidad " + unidad.getPlaca() + " asignada a " + ruta.getNombre() + ".", false);

            } else if (accion.equals("estado")) {
                Unidad unidad = buscarUnidad(Formulario.entero(datos, "id_unidad"));
                unidad.cambiarEstado(Formulario.texto(datos, "estado"));
                unidadDAO.actualizar(unidad);
                volver(ex, "Estado de " + unidad.getPlaca() + " actualizado.", false);
            }
            return;
        }

        List<Ruta> rutas = rutaDAO.listarActivas();
        Map<Integer, String> nombres = nombresDeRutas();

        StringBuilder html = new StringBuilder(Pagina.avisos(datos));
        html.append("<div class=\"tabla\"><table><tr><th>Placa</th><th>Modelo</th><th>Capacidad</th>")
            .append("<th>Ruta asignada</th><th>Asignar ruta</th><th>Estado</th></tr>");

        for (Unidad u : unidadDAO.listar()) {
            html.append("<tr><td><strong>").append(Pagina.esc(u.getPlaca())).append("</strong></td>")
                .append("<td>").append(Pagina.esc(u.getModelo())).append("</td>")
                .append("<td>").append(u.getCapacidad()).append("</td>")
                .append("<td>").append(u.getIdRuta() == 0 ? "Sin ruta" : Pagina.esc(nombres.get(u.getIdRuta()))).append("</td>");

            // Asignar ruta
            html.append("<td><form method=\"post\" class=\"en-linea\"><input type=\"hidden\" name=\"accion\" value=\"asignar\">")
                .append("<input type=\"hidden\" name=\"id_unidad\" value=\"").append(u.getIdUnidad()).append("\"><select name=\"id_ruta\">");
            for (Ruta r : rutas) {
                html.append(Pagina.opcion(r.getIdRuta(), r.getNombre(), r.getIdRuta() == u.getIdRuta()));
            }
            html.append("</select><button>Asignar</button></form></td>");

            // Cambiar estado
            html.append("<td><form method=\"post\" class=\"en-linea\"><input type=\"hidden\" name=\"accion\" value=\"estado\">")
                .append("<input type=\"hidden\" name=\"id_unidad\" value=\"").append(u.getIdUnidad()).append("\"><select name=\"estado\">");
            for (String estado : ESTADOS_UNIDAD) {
                html.append(Pagina.opcion(estado, estado, estado.equals(u.getEstado())));
            }
            html.append("</select><button class=\"claro\">Cambiar</button></form></td></tr>");
        }
        html.append("</table></div>");

        html.append("<div class=\"caja angosta\"><h3>Nueva unidad</h3><form method=\"post\">")
            .append("<input type=\"hidden\" name=\"accion\" value=\"nueva\">")
            .append("<label>Placa</label><input name=\"placa\" placeholder=\"BA-0000\" required>")
            .append("<label>Modelo</label><input name=\"modelo\" required>")
            .append("<label>Capacidad</label><input type=\"number\" name=\"capacidad\" min=\"10\" max=\"150\" required>")
            .append("<button>Registrar unidad</button></form></div>");

        enviarHtml(ex, Pagina.armar("Unidades", admin, html.toString()));
    }

    // ===================== Turnos =====================

    private void turnos(HttpExchange ex, Administrador admin, Map<String, String> datos) throws Exception {
        if (esPost(ex)) {
            Conductor conductor = usuarioDAO.buscarConductor(Formulario.entero(datos, "id_conductor"));
            if (conductor == null) {
                throw new IllegalArgumentException("Conductor no encontrado.");
            }
            Unidad unidad = buscarUnidad(Formulario.entero(datos, "id_unidad"));
            Ruta ruta = buscarRuta(Formulario.entero(datos, "id_ruta"));
            LocalDate fecha = Formulario.fecha(datos, "fecha");
            LocalTime inicio = Formulario.hora(datos, "hora_inicio");

            if (turnoDAO.conductorTieneTurno(conductor.getIdUsuario(), fecha)) {
                throw new IllegalStateException(conductor.getNombre() + " ya tiene un turno ese día.");
            }

            Turno turno = admin.crearTurno(conductor, unidad, ruta, fecha);
            turno.setHoraInicio(inicio);
            turnoDAO.insertar(turno);
            volver(ex, "Turno creado para " + conductor.getNombre() + ".", false);
            return;
        }

        Map<Integer, String> rutas = nombresDeRutas();
        Map<Integer, String> placas = placasDeUnidades();
        Map<Integer, String> conductores = new HashMap<>();
        List<Conductor> listaConductores = usuarioDAO.listarConductores();
        for (Conductor c : listaConductores) {
            conductores.put(c.getIdUsuario(), c.getNombre());
        }

        StringBuilder html = new StringBuilder(Pagina.avisos(datos));

        // Formulario para crear turno
        html.append("<div class=\"caja\"><h3>Crear turno</h3><form method=\"post\" class=\"fila\">")
            .append("<div><label>Conductor</label><select name=\"id_conductor\">");
        for (Conductor c : listaConductores) {
            html.append(Pagina.opcion(c.getIdUsuario(), c.getNombre(), false));
        }
        html.append("</select></div><div><label>Unidad</label><select name=\"id_unidad\">");
        for (Unidad u : unidadDAO.listar()) {
            if (u.estaDisponible()) {
                html.append(Pagina.opcion(u.getIdUnidad(), u.getPlaca(), false));
            }
        }
        html.append("</select></div><div><label>Ruta</label><select name=\"id_ruta\">");
        for (Ruta r : rutaDAO.listarActivas()) {
            html.append(Pagina.opcion(r.getIdRuta(), r.getNombre(), false));
        }
        html.append("</select></div>")
            .append("<div><label>Fecha</label><input type=\"date\" name=\"fecha\" value=\"").append(LocalDate.now()).append("\" required></div>")
            .append("<div><label>Hora de inicio</label><input type=\"time\" name=\"hora_inicio\" required></div>")
            .append("<div><button>Crear turno</button></div></form></div>");

        // Lista de turnos
        html.append("<div class=\"tabla\"><table><tr><th>Fecha</th><th>Conductor</th><th>Ruta</th><th>Unidad</th>")
            .append("<th>Inicio</th><th>Fin</th><th>Horas</th></tr>");
        for (Turno t : turnoDAO.listar()) {
            html.append("<tr><td>").append(Pagina.fecha(t.getFecha())).append("</td>")
                .append("<td>").append(Pagina.esc(conductores.get(t.getIdConductor()))).append("</td>")
                .append("<td>").append(Pagina.esc(rutas.get(t.getIdRuta()))).append("</td>")
                .append("<td>").append(Pagina.esc(placas.get(t.getIdUnidad()))).append("</td>")
                .append("<td>").append(t.getHoraInicio()).append("</td>")
                .append("<td>").append(t.estaFinalizado() ? t.getHoraFin().toString() : "En curso").append("</td>")
                .append("<td>").append(String.format("%.2f", t.calcularHoras())).append("</td></tr>");
        }
        html.append("</table></div>");

        enviarHtml(ex, Pagina.armar("Turnos", admin, html.toString()));
    }

    // ===================== Incidencias =====================

    private void incidencias(HttpExchange ex, Administrador admin, Map<String, String> datos) throws Exception {
        if (esPost(ex)) {
            Incidencia incidencia = incidenciaDAO.buscar(Formulario.entero(datos, "id_incidencia"));
            if (incidencia == null) {
                throw new IllegalArgumentException("Incidencia no encontrada.");
            }
            String estado = Formulario.texto(datos, "estado");
            if (estado.equals("CERRADA")) {
                incidencia.cerrar();
            } else {
                incidencia.cambiarEstado(estado);
            }
            incidenciaDAO.actualizarEstado(incidencia);
            volver(ex, "Incidencia #" + incidencia.getIdIncidencia() + " ahora está " + estado + ".", false);
            return;
        }

        Map<Integer, String> placas = placasDeUnidades();
        Map<Integer, String> conductores = new HashMap<>();
        for (Conductor c : usuarioDAO.listarConductores()) {
            conductores.put(c.getIdUsuario(), c.getNombre());
        }

        StringBuilder html = new StringBuilder(Pagina.avisos(datos));
        html.append("<div class=\"tabla\"><table><tr><th>#</th><th>Fecha</th><th>Tipo</th><th>Unidad</th>")
            .append("<th>Conductor</th><th>Descripción</th><th>Estado</th></tr>");

        for (Incidencia i : incidenciaDAO.listar()) {
            html.append("<tr><td>").append(i.getIdIncidencia()).append("</td>")
                .append("<td>").append(Pagina.fechaHora(i.getFecha())).append("</td>")
                .append("<td>").append(Pagina.esc(i.getTipo())).append("</td>")
                .append("<td>").append(Pagina.esc(placas.get(i.getIdUnidad()))).append("</td>")
                .append("<td>").append(Pagina.esc(conductores.get(i.getIdConductor()))).append("</td>")
                .append("<td>").append(Pagina.esc(i.getDescripcion())).append("</td><td>");

            if (i.getEstado().equals("CERRADA")) {
                html.append("CERRADA");
            } else {
                html.append("<form method=\"post\" class=\"en-linea\">")
                    .append("<input type=\"hidden\" name=\"id_incidencia\" value=\"").append(i.getIdIncidencia()).append("\">")
                    .append("<select name=\"estado\">")
                    .append(Pagina.opcion("ABIERTA", "ABIERTA", i.getEstado().equals("ABIERTA")))
                    .append(Pagina.opcion("EN PROCESO", "EN PROCESO", i.getEstado().equals("EN PROCESO")))
                    .append(Pagina.opcion("CERRADA", "CERRADA", false))
                    .append("</select><button>Guardar</button></form>");
            }
            html.append("</td></tr>");
        }
        html.append("</table></div>");

        enviarHtml(ex, Pagina.armar("Incidencias", admin, html.toString()));
    }

    // ===================== Reportes =====================

    private void reportes(HttpExchange ex, Administrador admin, Map<String, String> datos) throws Exception {
        if (esPost(ex)) {
            String tipo = Formulario.texto(datos, "tipo");
            Reporte reporte = admin.generarReporte(tipo);

            if (tipo.equals("RUTA")) {
                Ruta ruta = buscarRuta(Formulario.entero(datos, "id_ruta"));
                reporte.generarPorRuta(ruta);
                reporte.agregarDato("Turnos programados", String.valueOf(reporteDAO.turnosPorRuta(ruta.getIdRuta())));
                reporte.agregarDato("Boletos vendidos", String.valueOf(reporteDAO.boletosVendidosPorRuta(ruta.getIdRuta())));
                reporte.agregarDato("Ingresos", Pagina.dinero(reporteDAO.ingresosPorRuta(ruta.getIdRuta())));
            } else {
                Unidad unidad = buscarUnidad(Formulario.entero(datos, "id_unidad"));
                reporte.generarPorUnidad(unidad);
                reporte.agregarDato("Turnos realizados", String.valueOf(reporteDAO.turnosPorUnidad(unidad.getIdUnidad())));
                reporte.agregarDato("Incidencias totales", String.valueOf(reporteDAO.incidenciasPorUnidad(unidad.getIdUnidad())));
                reporte.agregarDato("Incidencias sin cerrar", String.valueOf(reporteDAO.incidenciasAbiertasPorUnidad(unidad.getIdUnidad())));
            }

            reporteDAO.insertar(reporte);
            redirigir(ex, "/admin/reporte?id=" + reporte.getIdReporte());
            return;
        }

        Map<Integer, String> rutas = nombresDeRutas();
        Map<Integer, String> placas = placasDeUnidades();

        StringBuilder html = new StringBuilder(Pagina.avisos(datos));
        html.append("<div class=\"columnas\">");

        html.append("<div class=\"caja\"><h3>Reporte por ruta</h3><form method=\"post\">")
            .append("<input type=\"hidden\" name=\"tipo\" value=\"RUTA\"><select name=\"id_ruta\">");
        for (Map.Entry<Integer, String> r : rutas.entrySet()) {
            html.append(Pagina.opcion(r.getKey(), r.getValue(), false));
        }
        html.append("</select><button>Generar</button></form></div>");

        html.append("<div class=\"caja\"><h3>Reporte por unidad</h3><form method=\"post\">")
            .append("<input type=\"hidden\" name=\"tipo\" value=\"UNIDAD\"><select name=\"id_unidad\">");
        for (Map.Entry<Integer, String> u : placas.entrySet()) {
            html.append(Pagina.opcion(u.getKey(), u.getValue(), false));
        }
        html.append("</select><button>Generar</button></form></div></div>");

        // Reportes ya generados
        html.append("<h3>Reportes generados</h3>");
        List<Reporte> lista = reporteDAO.listar();
        if (lista.isEmpty()) {
            html.append("<p>Todavía no se han generado reportes.</p>");
        } else {
            html.append("<div class=\"tabla\"><table><tr><th>#</th><th>Tipo</th><th>Sobre</th><th>Fecha</th><th></th></tr>");
            for (Reporte r : lista) {
                String sobre = r.getTipo().equals("RUTA") ? rutas.get(r.getIdRuta()) : placas.get(r.getIdUnidad());
                html.append("<tr><td>").append(r.getIdReporte()).append("</td>")
                    .append("<td>").append(r.getTipo()).append("</td>")
                    .append("<td>").append(Pagina.esc(sobre)).append("</td>")
                    .append("<td>").append(Pagina.fecha(r.getFechaGeneracion())).append("</td>")
                    .append("<td><a href=\"/admin/reporte?id=").append(r.getIdReporte()).append("\">Ver</a></td></tr>");
            }
            html.append("</table></div>");
        }
        enviarHtml(ex, Pagina.armar("Reportes", admin, html.toString()));
    }

    private void verReporte(HttpExchange ex, Administrador admin, Map<String, String> datos) throws Exception {
        Reporte reporte = buscarReporte(Formulario.entero(datos, "id"));
        String html = "<pre class=\"caja\">" + Pagina.esc(reporte.exportar()) + "</pre>"
                + "<p><a class=\"boton\" href=\"/admin/exportar?id=" + reporte.getIdReporte() + "\">Descargar .txt</a> "
                + "<a href=\"/admin/reportes\">← Volver a reportes</a></p>";
        enviarHtml(ex, Pagina.armar("Reporte #" + reporte.getIdReporte(), admin, html));
    }

    // Descarga el reporte como archivo de texto
    private void exportarReporte(HttpExchange ex, Map<String, String> datos) throws Exception {
        Reporte reporte = buscarReporte(Formulario.entero(datos, "id"));
        ex.getResponseHeaders().set("Content-Disposition",
                "attachment; filename=\"reporte-" + reporte.getIdReporte() + ".txt\"");
        enviar(ex, 200, reporte.exportar(), "text/plain; charset=UTF-8");
    }

    // ===================== Auxiliares =====================

    private Ruta buscarRuta(int id) throws Exception {
        Ruta ruta = rutaDAO.buscar(id);
        if (ruta == null) {
            throw new IllegalArgumentException("La ruta no existe.");
        }
        return ruta;
    }

    private Unidad buscarUnidad(int id) throws Exception {
        Unidad unidad = unidadDAO.buscar(id);
        if (unidad == null) {
            throw new IllegalArgumentException("La unidad no existe.");
        }
        return unidad;
    }

    private Reporte buscarReporte(int id) throws Exception {
        Reporte reporte = reporteDAO.buscar(id);
        if (reporte == null) {
            throw new IllegalArgumentException("El reporte no existe.");
        }
        return reporte;
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
