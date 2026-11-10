package transporte.boleto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import transporte.datos.Conexion;
import transporte.usuario.Pasajero;

// Consultas de boletos
public class BoletoDAO {

    // La compra guarda el boleto y descuenta el saldo al mismo tiempo.
    // Si algo falla no se guarda nada (transacción).
    public void guardarCompra(Boleto boleto, Pasajero pasajero) throws SQLException {
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                String sql = "INSERT INTO boleto (codigo, fecha_compra, precio, estado, id_horario, id_pasajero) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, boleto.getCodigo());
                    ps.setObject(2, boleto.getFechaCompra());
                    ps.setDouble(3, boleto.getPrecio());
                    ps.setString(4, boleto.getEstado());
                    ps.setInt(5, boleto.getIdHorario());
                    ps.setInt(6, boleto.getIdPasajero());
                    ps.executeUpdate();

                    try (ResultSet llaves = ps.getGeneratedKeys()) {
                        llaves.next();
                        boleto.setIdBoleto(llaves.getInt(1));
                    }
                }
                actualizarSaldo(con, pasajero);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // Anular: cambia el estado y devuelve el dinero
    public void guardarAnulacion(Boleto boleto, Pasajero pasajero) throws SQLException {
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                actualizarEstado(con, boleto);
                actualizarSaldo(con, pasajero);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void actualizarEstado(Boleto boleto) throws SQLException {
        try (Connection con = Conexion.obtener()) {
            actualizarEstado(con, boleto);
        }
    }

    public Boleto buscarPorCodigo(String codigo) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM boleto WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return crearBoleto(rs);
                }
            }
        }
        return null;
    }

    public List<Boleto> listarPorPasajero(int idPasajero) throws SQLException {
        List<Boleto> lista = new ArrayList<>();
        String sql = "SELECT * FROM boleto WHERE id_pasajero = ? ORDER BY fecha_compra DESC";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idPasajero);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(crearBoleto(rs));
                }
            }
        }
        return lista;
    }

    private void actualizarEstado(Connection con, Boleto boleto) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE boleto SET estado = ? WHERE id_boleto = ?")) {
            ps.setString(1, boleto.getEstado());
            ps.setInt(2, boleto.getIdBoleto());
            ps.executeUpdate();
        }
    }

    private void actualizarSaldo(Connection con, Pasajero pasajero) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE pasajero SET saldo = ? WHERE id_usuario = ?")) {
            ps.setDouble(1, pasajero.getSaldo());
            ps.setInt(2, pasajero.getIdUsuario());
            ps.executeUpdate();
        }
    }

    private Boleto crearBoleto(ResultSet rs) throws SQLException {
        return new Boleto(rs.getInt("id_boleto"), rs.getString("codigo"),
                rs.getObject("fecha_compra", LocalDateTime.class), rs.getDouble("precio"),
                rs.getString("estado"), rs.getInt("id_horario"), rs.getInt("id_pasajero"));
    }
}
