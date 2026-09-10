package com.example.gastofacil;

public class gasto_user {
    private String nombre;
    private String correo;
    private String telefono;
    private double saldo;
    private double totalIngresos;
    private double totalEgresos;
    private String idioma;
    private String pais;
    private String fechaNacimiento;
    private String genero;

    // Constructor vacío requerido para Firebase
    public gasto_user() {}

    public gasto_user(String nombre, String correo, String telefono, double saldo) {
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.saldo = saldo;
        this.totalIngresos = 0.0;
        this.totalEgresos = 0.0;
        this.idioma = "es"; // Por defecto español
        this.pais = "";
        this.fechaNacimiento = "";
        this.genero = "";
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    public double getTotalIngresos() { return totalIngresos; }
    public void setTotalIngresos(double totalIngresos) { this.totalIngresos = totalIngresos; }

    public double getTotalEgresos() { return totalEgresos; }
    public void setTotalEgresos(double totalEgresos) { this.totalEgresos = totalEgresos; }

    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }
}
