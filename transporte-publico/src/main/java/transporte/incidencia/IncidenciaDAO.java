package transporte.incidencia;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import transporte.datos.Conexion;

// Consultas de incidencias
public class IncidenciaDAO {

    public List<Incidencia> listar() throws SQLException {
        return listar("SELECT * FROM incidencia ORDER BY fecha DESC", 0);
    }

    public List<Incidencia> listarPorConductor(int idConductor) throws SQLException {
        return listar("SELECT * FROM incidencia WHERE id_conductor = ? ORDER BY fecha DESC", idConductor);
    }

    public Incidencia buscar(int idIncidencia) throws SQLException {
        List<Incidencia> resultado = listar("SELECT * FROM incidencia WHERE id_incidencia = ?", idIncidencia);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    public void insertar(Incidencia inc) throws SQLException {
        String sql = "INSERT INTO incidencia (fecha, tipo, descripcion, estado, id_unidad, id_conductor) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, inc.getFecha());
            ps.setString(2, inc.getTipo());
            ps.setString(3, inc.getDescripcion());
            ps.setString(4, inc.getEstado());
            ps.setInt(5, inc.getIdUnidad());
            ps.setInt(6, inc.getIdConductor());
            ps.executeUpdate();
        }
    }

    public void actualizarEstado(Incidencia inc) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("UPDATE incidencia SET estado = ? WHERE id_incidencia = ?")) {
            ps.setString(1, inc.getEstado());
            ps.setInt(2, inc.getIdIncidencia());
            ps.executeUpdate();
        }
    }

    private List<Incidencia> listar(String sql, int parametro) throws SQLException {
        List<Incidencia> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != 0) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Incidencia(rs.getInt("id_incidencia"),
                            rs.getObject("fecha", LocalDateTime.class),
                            rs.getString("tipo"), rs.getString("descripcion"), rs.getString("estado"),
                            rs.getInt("id_unidad"), rs.getInt("id_conductor")));
                }
            }
        }
        return lista;
    }
}
