package transporte.reporte;

import java.time.LocalDate;
import transporte.ruta.Ruta;
import transporte.unidad.Unidad;

// Reporte por ruta o por unidad que genera el administrador
public class Reporte {

    private int idReporte;
    private String tipo; // RUTA o UNIDAD
    private LocalDate fechaGeneracion;
    private String contenido = "";
    private int idRuta;
    private int idUnidad;

    // Reporte nuevo
    public Reporte(String tipo) {
        this.tipo = tipo;
        this.fechaGeneracion = LocalDate.now();
    }

    // Reporte leído de la base de datos
    public Reporte(int idReporte, String tipo, LocalDate fechaGeneracion, String contenido, int idRuta, int idUnidad) {
        this.idReporte = idReporte;
        this.tipo = tipo;
        this.fechaGeneracion = fechaGeneracion;
        this.contenido = contenido;
        this.idRuta = idRuta;
        this.idUnidad = idUnidad;
    }

    public void generarPorRuta(Ruta ruta) {
        idRuta = ruta.getIdRuta();
        contenido = "";
        agregarDato("Ruta", ruta.getNombre());
        agregarDato("Recorrido", ruta.getOrigen() + " -> " + ruta.getDestino());
        agregarDato("Tarifa", String.format("$%.2f", ruta.getTarifa()));
        agregarDato("Estado", ruta.isActiva() ? "Activa" : "Inactiva");
        agregarDato("Paradas", String.valueOf(ruta.obtenerParadas().size()));
        agregarDato("Horarios", String.valueOf(ruta.getHorarios().size()));
    }

    public void generarPorUnidad(Unidad unidad) {
        idUnidad = unidad.getIdUnidad();
        contenido = "";
        agregarDato("Placa", unidad.getPlaca());
        agregarDato("Modelo", unidad.getModelo());
        agregarDato("Capacidad", unidad.getCapacidad() + " pasajeros");
        agregarDato("Estado", unidad.getEstado());
    }

    // Agrega una línea al contenido (ej: "Boletos vendidos: 25")
    public void agregarDato(String etiqueta, String valor) {
        contenido += etiqueta + ": " + valor + "\n";
    }

    // Texto listo para descargar como archivo .txt
    public String exportar() {
        String separador = "----------------------------------------\n";
        return "REPORTE POR " + tipo + "\n"
                + "Sistema de Gestión de Transporte Público\n"
                + "Fecha de generación: " + fechaGeneracion + "\n"
                + separador
                + contenido
                + separador;
    }

    public int getIdReporte() { return idReporte; }
    public void setIdReporte(int idReporte) { this.idReporte = idReporte; }
    public String getTipo() { return tipo; }
    public LocalDate getFechaGeneracion() { return fechaGeneracion; }
    public String getContenido() { return contenido; }
    public int getIdRuta() { return idRuta; }
    public int getIdUnidad() { return idUnidad; }
}
