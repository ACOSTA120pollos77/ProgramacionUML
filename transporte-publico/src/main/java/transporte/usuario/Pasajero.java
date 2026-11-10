package transporte.usuario;

import java.sql.SQLException;
import java.util.List;
import transporte.boleto.Boleto;
import transporte.ruta.Horario;
import transporte.ruta.Ruta;
import transporte.ruta.RutaDAO;

// Pasajero
public class Pasajero extends Usuario {

    private String cedula;
    private String telefono;
    private double saldo;

    public Pasajero(int idUsuario, String nombre, String correo, String contrasena,
                    String cedula, String telefono, double saldo) {
        super(idUsuario, nombre, correo, contrasena, "PASAJERO");
        this.cedula = cedula;
        this.telefono = telefono;
        this.saldo = saldo;
    }

    // El precio sale de la tarifa de la ruta a la que pertenece el horario
    public Boleto comprarBoleto(Horario horario, double precio) {
        if (!horario.estaDisponible(Horario.diaDeHoy())) {
            throw new IllegalStateException("Ese horario no opera el día de hoy.");
        }
        if (saldo < precio) {
            throw new IllegalStateException("Saldo insuficiente. Recargue su cuenta para comprar.");
        }
        saldo -= precio;
        return new Boleto(idUsuario, horario.getIdHorario(), precio);
    }

    public void recargarSaldo(double monto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a $0.00");
        }
        if (monto > 50) {
            throw new IllegalArgumentException("El máximo por recarga es $50.00");
        }
        saldo += monto;
    }

    // Cuando se anula un boleto se devuelve el dinero
    public void reembolsar(double monto) {
        saldo += monto;
    }

    public List<Ruta> consultarRutas() throws SQLException {
        return new RutaDAO().listarActivas();
    }

    @Override
    public String mostrarMenu() {
        return "<a href=\"/pasajero\">Inicio</a>"
                + "<a href=\"/pasajero/rutas\">Rutas y horarios</a>"
                + "<a href=\"/pasajero/boletos\">Mis boletos</a>"
                + "<a href=\"/pasajero/recargar\">Recargar saldo</a>";
    }

    public String getCedula() { return cedula; }
    public String getTelefono() { return telefono; }
    public double getSaldo() { return saldo; }
}
