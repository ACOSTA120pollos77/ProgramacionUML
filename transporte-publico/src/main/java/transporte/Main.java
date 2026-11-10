package transporte;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import transporte.web.AdminHandler;
import transporte.web.ConductorHandler;
import transporte.web.InicioHandler;
import transporte.web.PasajeroHandler;

// Punto de entrada: levanta el servidor web
public class Main {

    public static void main(String[] args) throws Exception {
        int puerto = 8080;
        String puertoServidor = System.getenv("PORT"); // lo usan los hostings como Railway
        if (puertoServidor != null && !puertoServidor.isEmpty()) {
            puerto = Integer.parseInt(puertoServidor);
        }

        HttpServer servidor = HttpServer.create(new InetSocketAddress(puerto), 0);

        // Cada parte de la página tiene su propio manejador
        servidor.createContext("/", new InicioHandler());
        servidor.createContext("/pasajero", new PasajeroHandler());
        servidor.createContext("/conductor", new ConductorHandler());
        servidor.createContext("/admin", new AdminHandler());

        servidor.setExecutor(Executors.newFixedThreadPool(10));
        servidor.start();

        System.out.println("Servidor iniciado. Abra en el navegador: http://localhost:" + puerto);
    }
}
