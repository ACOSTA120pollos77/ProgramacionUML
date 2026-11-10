package transporte.boleto;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Boleto que compra el pasajero para un horario
public class Boleto {

    private int idBoleto;
    private String codigo;
    private LocalDateTime fechaCompra;
    private double precio;
    private String estado; // ACTIVO, USADO o ANULADO
    private int idHorario;
    private int idPasajero;

    // Boleto nuevo, recién comprado
    public Boleto(int idPasajero, int idHorario, double precio) {
        this.idPasajero = idPasajero;
        this.idHorario = idHorario;
        this.precio = precio;
        this.fechaCompra = LocalDateTime.now();
        this.estado = "ACTIVO";
        this.codigo = generarCodigo();
    }

    // Boleto que ya existe en la base de datos
    public Boleto(int idBoleto, String codigo, LocalDateTime fechaCompra, double precio,
                  String estado, int idHorario, int idPasajero) {
        this.idBoleto = idBoleto;
        this.codigo = codigo;
        this.fechaCompra = fechaCompra;
        this.precio = precio;
        this.estado = estado;
        this.idHorario = idHorario;
        this.idPasajero = idPasajero;
    }

    // Si está vigente se marca como usado para que no se pueda usar dos veces
    public boolean validar() {
        if (!estaVigente()) {
            return false;
        }
        estado = "USADO";
        return true;
    }

    public void anular() {
        if (!estaVigente()) {
            throw new IllegalStateException("Solo se pueden anular boletos vigentes.");
        }
        estado = "ANULADO";
    }

    // Un boleto solo sirve el mismo día que se compró
    public boolean estaVigente() {
        return estado.equals("ACTIVO") && fechaCompra.toLocalDate().equals(LocalDate.now());
    }

    // Código tipo BOL-X7K2M9 (sin letras confusas como O, 0, I, 1)
    public String generarCodigo() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder nuevo = new StringBuilder("BOL-");
        for (int i = 0; i < 6; i++) {
            nuevo.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return nuevo.toString();
    }

    // Lo que se muestra en pantalla (un boleto activo de otro día ya venció)
    public String estadoParaMostrar() {
        if (estado.equals("ACTIVO") && !estaVigente()) {
            return "VENCIDO";
        }
        return estado;
    }

    public int getIdBoleto() { return idBoleto; }
    public void setIdBoleto(int idBoleto) { this.idBoleto = idBoleto; }
    public String getCodigo() { return codigo; }
    public LocalDateTime getFechaCompra() { return fechaCompra; }
    public double getPrecio() { return precio; }
    public String getEstado() { return estado; }
    public int getIdHorario() { return idHorario; }
    public int getIdPasajero() { return idPasajero; }
}
