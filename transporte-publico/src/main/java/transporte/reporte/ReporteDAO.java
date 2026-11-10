package transporte.reporte;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import transporte.datos.Conexion;

// Consultas de reportes y las estadísticas que van dentro de ellos
public class ReporteDAO {

    public void insertar(Reporte r) throws SQLException {
        String sql = "INSERT INTO reporte (tipo, fecha_generacion, contenido, id_ruta, id_unidad) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getTipo());
            ps.setObject(2, r.getFechaGeneracion());
            ps.setString(3, r.getContenido());
            if (r.getIdRuta() == 0) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, r.getIdRuta());
            }
            if (r.getIdUnidad() == 0) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, r.getIdUnidad());
            }
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                r.setIdReporte(llaves.getInt(1));
            }
        }
    }

    public List<Reporte> listar() throws SQLException {
        List<Reporte> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM reporte ORDER BY id_reporte DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(crearReporte(rs));
            }
        }
        return lista;
    }

    public Reporte buscar(int idReporte) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM reporte WHERE id_reporte = ?")) {
            ps.setInt(1, idReporte);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return crearReporte(rs);
                }
            }
        }
        return null;
    }

    // Estadísticas por ruta

    public int boletosVendidosPorRuta(int idRuta) throws SQLException {
        String sql = "SELECT COUNT(*) FROM boleto b JOIN horario h ON h.id_horario = b.id_horario "
                + "WHERE h.id_ruta = ? AND b.estado <> 'ANULADO'";
        return (int) numero(sql, idRuta);
    }

    public double ingresosPorRuta(int idRuta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(b.precio), 0) FROM boleto b JOIN horario h ON h.id_horario = b.id_horario "
                + "WHERE h.id_ruta = ? AND b.estado <> 'ANULADO'";
        return numero(sql, idRuta);
    }

    public int turnosPorRuta(int idRuta) throws SQLException {
        return (int) numero("SELECT COUNT(*) FROM turno WHERE id_ruta = ?", idRuta);
    }

    // Estadísticas por unidad

    public int turnosPorUnidad(int idUnidad) throws SQLException {
        return (int) numero("SELECT COUNT(*) FROM turno WHERE id_unidad = ?", idUnidad);
    }

    public int incidenciasPorUnidad(int idUnidad) throws SQLException {
        return (int) numero("SELECT COUNT(*) FROM incidencia WHERE id_unidad = ?", idUnidad);
    }

    public int incidenciasAbiertasPorUnidad(int idUnidad) throws SQLException {
        return (int) numero("SELECT COUNT(*) FROM incidencia WHERE id_unidad = ? AND estado <> 'CERRADA'", idUnidad);
    }

    // Ejecuta una consulta que devuelve un solo número (COUNT, SUM)
    private double numero(String sql, int id) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }

    private Reporte crearReporte(ResultSet rs) throws SQLException {
        return new Reporte(rs.getInt("id_reporte"), rs.getString("tipo"),
                rs.getObject("fecha_generacion", LocalDate.class), rs.getString("contenido"),
                rs.getInt("id_ruta"), rs.getInt("id_unidad"));
    }
}
