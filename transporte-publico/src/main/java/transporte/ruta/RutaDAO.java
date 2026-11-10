package transporte.ruta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import transporte.datos.Conexion;

// Consultas de rutas, paradas y horarios
public class RutaDAO {

    public List<Ruta> listarTodas() throws SQLException {
        return listar("SELECT * FROM ruta ORDER BY id_ruta");
    }

    public List<Ruta> listarActivas() throws SQLException {
        return listar("SELECT * FROM ruta WHERE activa = TRUE ORDER BY nombre");
    }

    public Ruta buscar(int idRuta) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM ruta WHERE id_ruta = ?")) {
            ps.setInt(1, idRuta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Ruta ruta = crearRuta(rs);
                    cargarParadasYHorarios(con, ruta);
                    return ruta;
                }
            }
        }
        return null;
    }

    public void insertar(Ruta ruta) throws SQLException {
        String sql = "INSERT INTO ruta (nombre, origen, destino, tarifa, activa) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ruta.getNombre());
            ps.setString(2, ruta.getOrigen());
            ps.setString(3, ruta.getDestino());
            ps.setDouble(4, ruta.getTarifa());
            ps.setBoolean(5, ruta.isActiva());
            ps.executeUpdate();

            try (ResultSet llaves = ps.getGeneratedKeys()) {
                llaves.next();
                ruta.setIdRuta(llaves.getInt(1));
            }
        }
    }

    public void actualizarEstado(Ruta ruta) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("UPDATE ruta SET activa = ? WHERE id_ruta = ?")) {
            ps.setBoolean(1, ruta.isActiva());
            ps.setInt(2, ruta.getIdRuta());
            ps.executeUpdate();
        }
    }

    // Paradas
    public void insertarParada(Parada parada) throws SQLException {
        String sql = "INSERT INTO parada (nombre, ubicacion, orden, id_ruta) VALUES (?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, parada.getNombre());
            ps.setString(2, parada.getUbicacion());
            ps.setInt(3, parada.getOrden());
            ps.setInt(4, parada.getIdRuta());
            ps.executeUpdate();
        }
    }

    // Horarios
    public void insertarHorario(Horario horario) throws SQLException {
        String sql = "INSERT INTO horario (hora_salida, hora_llegada, dias_operacion, id_ruta) VALUES (?, ?, ?, ?)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, horario.getHoraSalida());
            ps.setObject(2, horario.getHoraLlegada());
            ps.setString(3, horario.getDiasOperacion());
            ps.setInt(4, horario.getIdRuta());
            ps.executeUpdate();
        }
    }

    public Horario buscarHorario(int idHorario) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM horario WHERE id_horario = ?")) {
            ps.setInt(1, idHorario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return crearHorario(rs);
                }
            }
        }
        return null;
    }

    private List<Ruta> listar(String sql) throws SQLException {
        List<Ruta> rutas = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Ruta ruta = crearRuta(rs);
                cargarParadasYHorarios(con, ruta);
                rutas.add(ruta);
            }
        }
        return rutas;
    }

    // Llena la ruta con sus paradas y horarios
    private void cargarParadasYHorarios(Connection con, Ruta ruta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM parada WHERE id_ruta = ? ORDER BY orden")) {
            ps.setInt(1, ruta.getIdRuta());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ruta.agregarParada(new Parada(rs.getInt("id_parada"), rs.getString("nombre"),
                            rs.getString("ubicacion"), rs.getInt("orden"), rs.getInt("id_ruta")));
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM horario WHERE id_ruta = ? ORDER BY hora_salida")) {
            ps.setInt(1, ruta.getIdRuta());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ruta.agregarHorario(crearHorario(rs));
                }
            }
        }
    }

    private Ruta crearRuta(ResultSet rs) throws SQLException {
        return new Ruta(rs.getInt("id_ruta"), rs.getString("nombre"), rs.getString("origen"),
                rs.getString("destino"), rs.getDouble("tarifa"), rs.getBoolean("activa"));
    }

    private Horario crearHorario(ResultSet rs) throws SQLException {
        return new Horario(rs.getInt("id_horario"),
                rs.getObject("hora_salida", LocalTime.class),
                rs.getObject("hora_llegada", LocalTime.class),
                rs.getString("dias_operacion"), rs.getInt("id_ruta"));
    }
}
