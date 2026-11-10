package transporte.unidad;

import transporte.ruta.Ruta;

// Unidad de transporte (bus)
public class Unidad {

    private int idUnidad;
    private String placa;
    private String modelo;
    private int capacidad;
    private String estado; // DISPONIBLE, EN RUTA o MANTENIMIENTO
    private int idRuta;    // 0 cuando no tiene ruta asignada

    public Unidad(int idUnidad, String placa, String modelo, int capacidad, String estado, int idRuta) {
        this.idUnidad = idUnidad;
        this.placa = placa;
        this.modelo = modelo;
        this.capacidad = capacidad;
        this.estado = estado;
        this.idRuta = idRuta;
    }

    public void asignarRuta(Ruta ruta) {
        this.idRuta = ruta.getIdRuta();
    }

    public void cambiarEstado(String estado) {
        if (!estado.equals("DISPONIBLE") && !estado.equals("EN RUTA") && !estado.equals("MANTENIMIENTO")) {
            throw new IllegalArgumentException("Estado de unidad no válido.");
        }
        this.estado = estado;
    }

    public boolean estaDisponible() {
        return estado.equals("DISPONIBLE");
    }

    public int getIdUnidad() { return idUnidad; }
    public void setIdUnidad(int idUnidad) { this.idUnidad = idUnidad; }
    public String getPlaca() { return placa; }
    public String getModelo() { return modelo; }
    public int getCapacidad() { return capacidad; }
    public String getEstado() { return estado; }
    public int getIdRuta() { return idRuta; }
}
