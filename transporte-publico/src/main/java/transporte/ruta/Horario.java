package transporte.ruta;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

// Horario de salida y llegada de una ruta
public class Horario {

    // Los días se guardan así en la base de datos: "LUN,MAR,MIE"
    public static final String[] DIAS = {"LUN", "MAR", "MIE", "JUE", "VIE", "SAB", "DOM"};

    private int idHorario;
    private LocalTime horaSalida;
    private LocalTime horaLlegada;
    private String diasOperacion;
    private int idRuta;

    public Horario(int idHorario, LocalTime horaSalida, LocalTime horaLlegada, String diasOperacion, int idRuta) {
        if (!horaLlegada.isAfter(horaSalida)) {
            throw new IllegalArgumentException("La hora de llegada debe ser después de la hora de salida.");
        }
        this.idHorario = idHorario;
        this.horaSalida = horaSalida;
        this.horaLlegada = horaLlegada;
        this.diasOperacion = diasOperacion;
        this.idRuta = idRuta;
    }

    public boolean estaDisponible(String dia) {
        return diasOperacion.toUpperCase().contains(dia.toUpperCase());
    }

    // Duración del viaje en minutos
    public int calcularDuracion() {
        return (int) Duration.between(horaSalida, horaLlegada).toMinutes();
    }

    public static String diaDeHoy() {
        int numero = LocalDate.now().getDayOfWeek().getValue(); // lunes = 1 ... domingo = 7
        return DIAS[numero - 1];
    }

    public int getIdHorario() { return idHorario; }
    public void setIdHorario(int idHorario) { this.idHorario = idHorario; }
    public LocalTime getHoraSalida() { return horaSalida; }
    public LocalTime getHoraLlegada() { return horaLlegada; }
    public String getDiasOperacion() { return diasOperacion; }
    public int getIdRuta() { return idRuta; }
}
