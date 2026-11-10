package transporte.turno;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

// Turno: un conductor maneja una unidad en una ruta en cierta fecha
public class Turno {

    private int idTurno;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin; // queda vacía hasta que el conductor finaliza el turno
    private int idRuta;
    private int idConductor;
    private int idUnidad;

    // Turno nuevo (lo crea el administrador)
    public Turno(LocalDate fecha, int idRuta, int idConductor, int idUnidad) {
        this.fecha = fecha;
        this.idRuta = idRuta;
        this.idConductor = idConductor;
        this.idUnidad = idUnidad;
    }

    // Turno leído de la base de datos
    public Turno(int idTurno, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
                 int idRuta, int idConductor, int idUnidad) {
        this(fecha, idRuta, idConductor, idUnidad);
        this.idTurno = idTurno;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public double calcularHoras() {
        if (horaFin == null) {
            return 0;
        }
        long minutos = Duration.between(horaInicio, horaFin).toMinutes();
        return minutos / 60.0;
    }

    public void finalizar() {
        if (horaFin != null) {
            throw new IllegalStateException("Este turno ya fue finalizado.");
        }
        if (!fecha.equals(LocalDate.now())) {
            throw new IllegalStateException("Solo se puede finalizar un turno del día de hoy.");
        }
        LocalTime ahora = LocalTime.now().withSecond(0).withNano(0);
        if (!ahora.isAfter(horaInicio)) {
            throw new IllegalStateException("El turno todavía no ha empezado.");
        }
        horaFin = ahora;
    }

    public boolean estaFinalizado() {
        return horaFin != null;
    }

    public int getIdTurno() { return idTurno; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public int getIdRuta() { return idRuta; }
    public int getIdConductor() { return idConductor; }
    public int getIdUnidad() { return idUnidad; }
}
