package transporte.web;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import transporte.usuario.Pasajero;
import transporte.usuario.Usuario;
import transporte.usuario.UsuarioDAO;

// Inicio de sesión, registro de pasajeros y cierre de sesión
public class InicioHandler extends BaseHandler {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void atender(HttpExchange ex) throws Exception {
        Map<String, String> datos = Formulario.leer(ex);

        switch (ruta(ex)) {
            case "/":
                inicio(ex, datos);
                break;
            case "/login":
                login(ex, datos);
                break;
            case "/registro":
                registro(ex, datos);
                break;
            case "/salir":
                salir(ex);
                break;
            case "/estilos.css":
                enviar(ex, 200, Pagina.cargar("estilos.css"), "text/css; charset=UTF-8");
                break;
            default:
                noEncontrado(ex);
        }
    }

    private void inicio(HttpExchange ex, Map<String, String> datos) throws Exception {
        Usuario usuario = Sesiones.obtener(ex);
        if (usuario != null) {
            redirigir(ex, Pagina.inicioDe(usuario));
            return;
        }
        enviarHtml(ex, Pagina.cargar("login.html").replace("{{mensaje}}", Pagina.avisos(datos)));
    }

    private void login(HttpExchange ex, Map<String, String> datos) throws Exception {
        if (!esPost(ex)) {
            redirigir(ex, "/");
            return;
        }
        String correo = Formulario.texto(datos, "correo").toLowerCase();
        String contrasena = Formulario.texto(datos, "contrasena");

        Usuario usuario = usuarioDAO.buscarPorCorreo(correo);

        // Mismo mensaje en los dos casos para no revelar qué correos existen
        if (usuario == null || !usuario.iniciarSesion(correo, contrasena)) {
            redirigir(ex, "/?error=Correo+o+contrase%C3%B1a+incorrectos");
            return;
        }
        Sesiones.crear(ex, usuario);
        redirigir(ex, Pagina.inicioDe(usuario));
    }

    // Solo los pasajeros se pueden registrar solos. Conductores y admins los crea la empresa.
    private void registro(HttpExchange ex, Map<String, String> datos) throws Exception {
        if (!esPost(ex)) {
            enviarHtml(ex, Pagina.cargar("registro.html").replace("{{mensaje}}", Pagina.avisos(datos)));
            return;
        }
        String nombre = Formulario.texto(datos, "nombre");
        String cedula = Formulario.texto(datos, "cedula");
        String telefono = datos.getOrDefault("telefono", "");
        String correo = Formulario.texto(datos, "correo").toLowerCase();
        String contrasena = Formulario.texto(datos, "contrasena");

        if (!correo.contains("@") || !correo.contains(".")) {
            throw new IllegalArgumentException("Escriba un correo válido.");
        }
        if (usuarioDAO.buscarPorCorreo(correo) != null) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
        }

        Pasajero nuevo = new Pasajero(0, nombre, correo, "", cedula, telefono, 0);
        nuevo.cambiarContrasena(contrasena);
        usuarioDAO.registrarPasajero(nuevo);

        redirigir(ex, "/?ok=Cuenta+creada.+Ya+puede+iniciar+sesi%C3%B3n.");
    }

    private void salir(HttpExchange ex) throws Exception {
        Usuario usuario = Sesiones.obtener(ex);
        if (usuario != null) {
            usuario.cerrarSesion();
        }
        Sesiones.cerrar(ex);
        redirigir(ex, "/");
    }
}
