package transporte.usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Clase padre: Pasajero, Conductor y Administrador heredan de aquí
public abstract class Usuario {

    protected int idUsuario;
    protected String nombre;
    protected String correo;
    private String contrasena; // guardada en SHA-256, nunca en texto plano
    protected String rol;

    public Usuario(int idUsuario, String nombre, String correo, String contrasena, String rol) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
        this.rol = rol;
    }

    public boolean iniciarSesion(String correo, String contrasena) {
        return this.correo.equalsIgnoreCase(correo)
                && this.contrasena.equals(encriptarContrasena(contrasena));
    }

    public void cerrarSesion() {
        System.out.println("Sesión cerrada: " + correo);
    }

    // Cada tipo de usuario tiene su propio menú
    public abstract String mostrarMenu();

    protected String encriptarContrasena(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(texto.getBytes(StandardCharsets.UTF_8));

            StringBuilder resultado = new StringBuilder();
            for (byte b : hash) {
                resultado.append(String.format("%02x", b));
            }
            return resultado.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    // Se usa cuando se registra un usuario nuevo
    public void cambiarContrasena(String nueva) {
        if (nueva.length() < 6) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
        }
        this.contrasena = encriptarContrasena(nueva);
    }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getContrasena() { return contrasena; }
    public String getRol() { return rol; }
}
