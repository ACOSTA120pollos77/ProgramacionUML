package transporte.usuario;

import java.sql.SQLException;
import java.util.List;
import transporte.boleto.Boleto;
import transporte.incidencia.Incidencia;
import transporte.turno.Turno;
import transporte.turno.TurnoDAO;

// Conductor
public class Conductor extends Usuario {

    private String numeroLicencia;
    private String tipoLicencia;
    private boolean disponible;

    public Conductor(int idUsuario, String nombre, String correo, String contrasena,
                     String numeroLicencia, String tipoLicencia, boolean disponible) {
        super(idUsuario, nombre, correo, contrasena, "CONDUCTOR");
        this.numeroLicencia = numeroLicencia;
        this.tipoLicencia = tipoLicencia;
        this.disponible = disponible;
    }

    // El conductor revisa el boleto cuando el pasajero sube al bus
    public boolean validarBoleto(Boleto boleto) {
        return boleto.validar();
    }

    public Incidencia registrarIncidencia(String tipo, String descripcion) {
        if (descripcion.length() < 10) {
            throw new IllegalArgumentException("Describa la incidencia con un poco más de detalle.");
        }
        return new Incidencia(tipo, descripcion, idUsuario);
    }

    public List<Turno> consultarTurnos() throws SQLException {
        return new TurnoDAO().listarPorConductor(idUsuario);
    }

    @Override
    public String mostrarMenu() {
        return "<a href=\"/conductor\">Inicio</a>"
                + "<a href=\"/conductor/turnos\">Mis turnos</a>"
                + "<a href=\"/conductor/validar\">Validar boleto</a>"
                + "<a href=\"/conductor/incidencias\">Incidencias</a>";
    }

    public String getNumeroLicencia() { return numeroLicencia; }
    public String getTipoLicencia() { return tipoLicencia; }
    public boolean isDisponible() { return disponible; }
}
