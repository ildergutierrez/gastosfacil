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
    private Double latitude;
    private Double longitude;

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

    public TransactionModel(String id, String monto, String categoria, String fecha, String descripcion, String tipo, String imagenBase64, long timestamp, Double latitude, Double longitude) {
        this.id = id;
        this.monto = monto;
        this.categoria = categoria;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.imagenBase64 = imagenBase64;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getMonto() { return monto; }
    public String getCategoria() { return categoria; }
    public String getFecha() { return fecha; }
    public String getDescripcion() { return descripcion; }
    public String getTipo() { return tipo; }
    public String getImagenBase64() { return imagenBase64; }
    public long getTimestamp() {
        if (fecha != null && !fecha.isEmpty()) {
            try {
                String clean = fecha.replace("Hoy, ", "").trim();
                String[] parts = clean.split("/");
                if (parts.length == 3) {
                    String day = parts[0];
                    String month = parts[1];
                    String year = parts[2];
                    if (year.length() == 2) {
                        year = "20" + year;
                    }
                    clean = day + "/" + month + "/" + year;
                }
                java.text.SimpleDateFormat[] formats = {
                    new java.text.SimpleDateFormat("dd/MM/yyyy", new java.util.Locale("es", "ES")),
                    new java.text.SimpleDateFormat("d/M/yyyy", new java.util.Locale("es", "ES")),
                    new java.text.SimpleDateFormat("dd/MM/yy", new java.util.Locale("es", "ES")),
                    new java.text.SimpleDateFormat("yyyy-MM-dd", new java.util.Locale("es", "ES"))
                };
                for (java.text.SimpleDateFormat sdf : formats) {
                    try {
                        sdf.setLenient(true);
                        java.util.Date date = sdf.parse(clean);
                        if (date != null) return date.getTime();
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        return timestamp;
    }

    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
