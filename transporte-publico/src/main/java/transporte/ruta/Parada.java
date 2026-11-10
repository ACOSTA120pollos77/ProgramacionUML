package transporte.ruta;

// Parada de una ruta
public class Parada {

    private int idParada;
    private String nombre;
    private String ubicacion;
    private int orden;
    private int idRuta;

    public Parada(int idParada, String nombre, String ubicacion, int orden, int idRuta) {
        this.idParada = idParada;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
        this.orden = orden;
        this.idRuta = idRuta;
    }

    public String mostrarInfo() {
        return orden + ". " + nombre + " (" + ubicacion + ")";
    }

    // La primera parada es la terminal de salida
    public boolean esTerminal() {
        return orden == 1 || nombre.toLowerCase().contains("terminal");
    }

    public int getIdParada() { return idParada; }
    public void setIdParada(int idParada) { this.idParada = idParada; }
    public String getNombre() { return nombre; }
    public String getUbicacion() { return ubicacion; }
    public int getOrden() { return orden; }
    public int getIdRuta() { return idRuta; }
}
