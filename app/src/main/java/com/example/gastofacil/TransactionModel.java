package com.example.gastofacil;

public class TransactionModel {
    private String id;
    private String monto;
    private String categoria;
    private String fecha;
    private String descripcion;
    private String tipo; // "INGRESO" o "EGRESO"
    private String imagenBase64;
    private long timestamp;

    public TransactionModel() {}

    public TransactionModel(String id, String monto, String categoria, String fecha, String descripcion, String tipo, String imagenBase64, long timestamp) {
        this.id = id;
        this.monto = monto;
        this.categoria = categoria;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.imagenBase64 = imagenBase64;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getMonto() { return monto; }
    public String getCategoria() { return categoria; }
    public String getFecha() { return fecha; }
    public String getDescripcion() { return descripcion; }
    public String getTipo() { return tipo; }
    public String getImagenBase64() { return imagenBase64; }
    public long getTimestamp() { return timestamp; }
}
