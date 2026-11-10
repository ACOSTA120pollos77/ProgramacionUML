package transporte.unidad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import transporte.datos.Conexion;

// Consultas de unidades
public class UnidadDAO {

    public List<Unidad> listar() throws SQLException {
        List<Unidad> lista = new ArrayList<>();
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM unidad ORDER BY placa");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(crearUnidad(rs));
            }
        }
        return lista;
    }

    public Unidad buscar(int idUnidad) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM unidad WHERE id_unidad = ?")) {
            ps.setInt(1, idUnidad);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return crearUnidad(rs);
                }
            }
        }
        return null;
    }

    public void insertar(Unidad unidad) throws SQLException {
        String sql = "INSERT INTO unidad (placa, modelo, capacidad, estado, id_ruta) VALUES (?, ?, ?, ?, NULL)";
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, unidad.getPlaca());
            ps.setString(2, unidad.getModelo());
            ps.setInt(3, unidad.getCapacidad());
            ps.setString(4, unidad.getEstado());
            ps.executeUpdate();
        }
    }

    // Guarda los cambios de estado y de ruta asignada
    public void actualizar(Unidad unidad) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("UPDATE unidad SET estado = ?, id_ruta = ? WHERE id_unidad = ?")) {
            ps.setString(1, unidad.getEstado());
            if (unidad.getIdRuta() == 0) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, unidad.getIdRuta());
            }
            ps.setInt(3, unidad.getIdUnidad());
            ps.executeUpdate();
        }
    }

    public boolean existePlaca(String placa) throws SQLException {
        try (Connection con = Conexion.obtener();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM unidad WHERE placa = ?")) {
            ps.setString(1, placa);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Unidad crearUnidad(ResultSet rs) throws SQLException {
        // getInt devuelve 0 cuando id_ruta es NULL
        return new Unidad(rs.getInt("id_unidad"), rs.getString("placa"), rs.getString("modelo"),
                rs.getInt("capacidad"), rs.getString("estado"), rs.getInt("id_ruta"));
    }
}
