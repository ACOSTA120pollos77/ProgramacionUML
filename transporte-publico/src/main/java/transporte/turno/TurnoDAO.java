package transporte.turno;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import transporte.datos.Conexion;

// Consultas de turnos
public class TurnoDAO {

    public List<Turno> listar() throws SQLException {
        return listar("SELECT * FROM turno ORDER BY fecha DESC, hora_inicio", 0);
    }

    public List<Turno> listarPorConductor(int idConductor) throws SQLException {
        return listar("SELECT * FROM turno WHERE id_conductor = ? ORDER BY fecha DESC, hora_inicio", idConductor);
    }

    public Turno buscar(int idTurno) throws SQLException {
        List<Turno> resultado = listar("SELECT * FROM turno WHERE id_turno = ?", idTurno);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public void insertar(Turno turno) throws SQLException {
        String sql = "INSERT INTO turno (fecha, hora_inicio, hora_fin, id_ruta, id_conductor, id_unidad) "
                + "VALUES (?, ?, NULL, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, turno.getFecha());
            ps.setObject(2, turno.getHoraInicio());
            ps.setInt(3, turno.getIdRuta());
            ps.setInt(4, turno.getIdConductor());
            ps.setInt(5, turno.getIdUnidad());
            ps.executeUpdate();
        }
    }

    public void guardarFinalizacion(Turno turno) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("UPDATE turno SET hora_fin = ? WHERE id_turno = ?")) {
            ps.setObject(1, turno.getHoraFin());
            ps.setInt(2, turno.getIdTurno());
            ps.executeUpdate();
        }
    }

    // Evita que un conductor tenga dos turnos el mismo día
    public boolean conductorTieneTurno(int idConductor, LocalDate fecha) throws SQLException {
        String sql = "SELECT 1 FROM turno WHERE id_conductor = ? AND fecha = ?";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idConductor);
            ps.setObject(2, fecha);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // El parámetro es opcional: si es 0 la consulta no lleva "?"
    private List<Turno> listar(String sql, int parametro) throws SQLException {
        List<Turno> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != 0) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Turno(rs.getInt("id_turno"),
                            rs.getObject("fecha", LocalDate.class),
                            rs.getObject("hora_inicio", LocalTime.class),
                            rs.getObject("hora_fin", LocalTime.class),
                            rs.getInt("id_ruta"), rs.getInt("id_conductor"), rs.getInt("id_unidad")));
                }
            }
        }
        return lista;
    }
}
