package transporte.usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import transporte.datos.Conexion;

// Consultas a la base de datos de los usuarios
public class UsuarioDAO {

    // Une la tabla usuario con la de su rol para traer todos los datos de una vez
    private static final String CONSULTA =
            "SELECT u.*, p.cedula, p.telefono, p.saldo, "
            + "c.numero_licencia, c.tipo_licencia, c.disponible, a.cargo "
            + "FROM usuario u "
            + "LEFT JOIN pasajero p ON p.id_usuario = u.id_usuario "
            + "LEFT JOIN conductor c ON c.id_usuario = u.id_usuario "
            + "LEFT JOIN administrador a ON a.id_usuario = u.id_usuario ";

    public Usuario buscarPorCorreo(String correo) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(CONSULTA + "WHERE u.correo = ?")) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return crearUsuario(rs);
                }
            }
        }
        return null;
    }

    public Conductor buscarConductor(int idConductor) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(CONSULTA + "WHERE u.id_usuario = ? AND u.rol = 'CONDUCTOR'")) {
            ps.setInt(1, idConductor);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return (Conductor) crearUsuario(rs);
                }
            }
        }
        return null;
    }

    public List<Conductor> listarConductores() throws SQLException {
        List<Conductor> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(CONSULTA + "WHERE u.rol = 'CONDUCTOR' ORDER BY u.nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add((Conductor) crearUsuario(rs));
            }
        }
        return lista;
    }

    // Registro de pasajero: se guarda en dos tablas, por eso se usa una transacción
    public void registrarPasajero(Pasajero p) throws SQLException {
        try (Connection con = Conexion.obtener()) {
            con.setAutoCommit(false);
            try {
                String sqlUsuario = "INSERT INTO usuario (nombre, correo, contrasena, rol) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sqlUsuario, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, p.getNombre());
                    ps.setString(2, p.getCorreo());
                    ps.setString(3, p.getContrasena());
                    ps.setString(4, p.getRol());
                    ps.executeUpdate();

                    try (ResultSet llaves = ps.getGeneratedKeys()) {
                        llaves.next();
                        p.setIdUsuario(llaves.getInt(1));
                    }
                }

                String sqlPasajero = "INSERT INTO pasajero (id_usuario, cedula, telefono, saldo) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sqlPasajero)) {
                    ps.setInt(1, p.getIdUsuario());
                    ps.setString(2, p.getCedula());
                    ps.setString(3, p.getTelefono());
                    ps.setDouble(4, p.getSaldo());
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void actualizarSaldo(Pasajero p) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("UPDATE pasajero SET saldo = ? WHERE id_usuario = ?")) {
            ps.setDouble(1, p.getSaldo());
            ps.setInt(2, p.getIdUsuario());
            ps.executeUpdate();
        }
    }

    // Según el rol se crea el objeto de la clase hija que corresponde
    private Usuario crearUsuario(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_usuario");
        String nombre = rs.getString("nombre");
        String correo = rs.getString("correo");
        String contrasena = rs.getString("contrasena");
        String rol = rs.getString("rol");

        if (rol.equals("PASAJERO")) {
            return new Pasajero(id, nombre, correo, contrasena,
                    rs.getString("cedula"), rs.getString("telefono"), rs.getDouble("saldo"));
        } else if (rol.equals("CONDUCTOR")) {
            return new Conductor(id, nombre, correo, contrasena,
                    rs.getString("numero_licencia"), rs.getString("tipo_licencia"), rs.getBoolean("disponible"));
        } else {
            return new Administrador(id, nombre, correo, contrasena, rs.getString("cargo"));
        }
    }
}
