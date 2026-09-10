package com.example.gastofacil;

public class G_Egresos {
    private String id;
    private String monto;
    private String categoria;
    private String fecha;
    private String descripcion;
    private String imagenBase64;
    private long timestamp;

    public G_Egresos() {}

    public G_Egresos(String id, String monto, String categoria, String fecha, String descripcion, String imagenBase64, long timestamp) {
        this.id = id;
        this.monto = monto;
        this.categoria = categoria;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.imagenBase64 = imagenBase64;
        this.timestamp = timestamp;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMonto() { return monto; }
    public void setMonto(String monto) { this.monto = monto; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescription(String descripcion) { this.descripcion = descripcion; }

    public String getImagenBase64() { return imagenBase64; }
    public void setImagenBase64(String imagenBase64) { this.imagenBase64 = imagenBase64; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
