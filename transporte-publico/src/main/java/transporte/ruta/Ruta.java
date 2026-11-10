package transporte.ruta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Ruta: contiene sus paradas y sus horarios
public class Ruta {

    private int idRuta;
    private String nombre;
    private String origen;
    private String destino;
    private double tarifa;
    private boolean activa;

    private List<Parada> paradas = new ArrayList<>();
    private List<Horario> horarios = new ArrayList<>();

    public Ruta(int idRuta, String nombre, String origen, String destino, double tarifa, boolean activa) {
        this.idRuta = idRuta;
        this.nombre = nombre;
        this.origen = origen;
        this.destino = destino;
        this.tarifa = tarifa;
        this.activa = activa;
    }

    public void agregarParada(Parada parada) {
        paradas.add(parada);
    }

    public void agregarHorario(Horario horario) {
        horarios.add(horario);
    }

    // Devuelve las paradas en el orden del recorrido
    public List<Parada> obtenerParadas() {
        paradas.sort(Comparator.comparingInt(Parada::getOrden));
        return paradas;
    }

    public void cambiarEstado(boolean activa) {
        this.activa = activa;
    }

    // Según el diagrama una ruta debe tener mínimo 2 paradas
    public boolean estaCompleta() {
        return paradas.size() >= 2 && !horarios.isEmpty();
    }

    public int getIdRuta() { return idRuta; }
    public void setIdRuta(int idRuta) { this.idRuta = idRuta; }
    public String getNombre() { return nombre; }
    public String getOrigen() { return origen; }
    public String getDestino() { return destino; }
    public double getTarifa() { return tarifa; }
    public boolean isActiva() { return activa; }
    public List<Horario> getHorarios() { return horarios; }
}
