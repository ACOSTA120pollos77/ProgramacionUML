package transporte.usuario;

import java.time.LocalDate;
import transporte.reporte.Reporte;
import transporte.ruta.Ruta;
import transporte.turno.Turno;
import transporte.unidad.Unidad;

// Administrador
public class Administrador extends Usuario {

    private String cargo;

    public Administrador(int idUsuario, String nombre, String correo, String contrasena, String cargo) {
        super(idUsuario, nombre, correo, contrasena, "ADMINISTRADOR");
        this.cargo = cargo;
    }

    // Revisa que los datos de la ruta tengan sentido antes de guardarla
    public void registrarRuta(Ruta ruta) {
        if (ruta.getOrigen().equalsIgnoreCase(ruta.getDestino())) {
            throw new IllegalArgumentException("El origen y el destino no pueden ser iguales.");
        }
        if (ruta.getTarifa() <= 0 || ruta.getTarifa() > 10) {
            throw new IllegalArgumentException("La tarifa debe estar entre $0.01 y $10.00");
        }
        ruta.cambiarEstado(true);
    }

    public void asignarUnidad(Unidad unidad, Ruta ruta) {
        if (unidad.getEstado().equals("MANTENIMIENTO")) {
            throw new IllegalStateException("La unidad " + unidad.getPlaca() + " está en mantenimiento.");
        }
        if (!ruta.isActiva()) {
            throw new IllegalStateException("No se puede asignar una unidad a una ruta inactiva.");
        }
        unidad.asignarRuta(ruta);
    }

    public Turno crearTurno(Conductor c, Unidad u, Ruta r, LocalDate fecha) {
        if (!c.isDisponible()) {
            throw new IllegalStateException(c.getNombre() + " no está disponible.");
        }
        if (!u.estaDisponible()) {
            throw new IllegalStateException("La unidad " + u.getPlaca() + " no está disponible.");
        }
        if (!r.isActiva()) {
            throw new IllegalStateException("La ruta " + r.getNombre() + " está inactiva.");
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se pueden crear turnos en fechas pasadas.");
        }
        return new Turno(fecha, r.getIdRuta(), c.getIdUsuario(), u.getIdUnidad());
    }

    public Reporte generarReporte(String tipo) {
        if (!tipo.equals("RUTA") && !tipo.equals("UNIDAD")) {
            throw new IllegalArgumentException("Tipo de reporte no válido.");
        }
        return new Reporte(tipo);
    }

    @Override
    public String mostrarMenu() {
        return "<a href=\"/admin\">Inicio</a>"
                + "<a href=\"/admin/rutas\">Rutas</a>"
                + "<a href=\"/admin/unidades\">Unidades</a>"
                + "<a href=\"/admin/turnos\">Turnos</a>"
                + "<a href=\"/admin/incidencias\">Incidencias</a>"
                + "<a href=\"/admin/reportes\">Reportes</a>";
    }

    public String getCargo() { return cargo; }
}
