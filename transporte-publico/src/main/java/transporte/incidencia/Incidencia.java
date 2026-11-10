package transporte.incidencia;

import java.time.LocalDateTime;

// Incidencia que reporta un conductor sobre una unidad
public class Incidencia {

    private int idIncidencia;
    private LocalDateTime fecha;
    private String tipo;
    private String descripcion;
    private String estado; // ABIERTA, EN PROCESO o CERRADA
    private int idUnidad;
    private int idConductor;

    // Incidencia nueva
    public Incidencia(String tipo, String descripcion, int idConductor) {
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.idConductor = idConductor;
        this.fecha = LocalDateTime.now();
        this.estado = "ABIERTA";
    }

    // Incidencia leída de la base de datos
    public Incidencia(int idIncidencia, LocalDateTime fecha, String tipo, String descripcion,
                      String estado, int idUnidad, int idConductor) {
        this.idIncidencia = idIncidencia;
        this.fecha = fecha;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.estado = estado;
        this.idUnidad = idUnidad;
        this.idConductor = idConductor;
    }

    public void cambiarEstado(String estado) {
        if (this.estado.equals("CERRADA")) {
            throw new IllegalStateException("La incidencia ya está cerrada.");
        }
        if (!estado.equals("ABIERTA") && !estado.equals("EN PROCESO")) {
            throw new IllegalArgumentException("Estado no válido.");
        }
        this.estado = estado;
    }

    public void cerrar() {
        if (estado.equals("CERRADA")) {
            throw new IllegalStateException("La incidencia ya está cerrada.");
        }
        estado = "CERRADA";
    }

    public int getIdIncidencia() { return idIncidencia; }
    public LocalDateTime getFecha() { return fecha; }
    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public String getEstado() { return estado; }
    public int getIdUnidad() { return idUnidad; }
    public void setIdUnidad(int idUnidad) { this.idUnidad = idUnidad; }
    public int getIdConductor() { return idConductor; }
}
