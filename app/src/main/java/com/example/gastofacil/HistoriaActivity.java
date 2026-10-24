package com.example.gastofacil;

import android.content.Intent;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoriaActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionClickListener {

    private RecyclerView rvHistorial;
    private TransactionAdapter adapter;
    private Button btnVerMas;
    private List<TransactionModel> listFull = new ArrayList<>();
    private List<HistoryItem> historyItems = new ArrayList<>();
    private Map<String, TransactionModel> mapTransactions = new HashMap<>();
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private int displayedCount = 15;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historia);
        ThemeHelper.applyBackground(this, findViewById(android.R.id.content), false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        rvHistorial = findViewById(R.id.rvHistorial);
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(historyItems, this);
        rvHistorial.setAdapter(adapter);

        btnVerMas = findViewById(R.id.btnVerMas);
        btnVerMas.setOnClickListener(v -> {
            displayedCount += 15;
            actualizarUI();
        });

        LinearLayout navHome = findViewById(R.id.navHomeH);
        LinearLayout navExpenses = findViewById(R.id.navExpensesH);
        LinearLayout navCharts = findViewById(R.id.navChartsH);

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HistoriaActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        navExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HistoriaActivity.this, MovimientosActivity.class);
                intent.putExtra("TYPE", "GASTO");
                startActivity(intent);
                finish();
            }
        });

        navCharts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HistoriaActivity.this, GraficosActivity.class);
                startActivity(intent);
                finish();
            }
        });

        cargarHistorial();
    }

    private void cargarHistorial() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        // Cargar Ingresos
        mDatabase.child("ingresos").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    G_Ingresos ing = ds.getValue(G_Ingresos.class);
                    if (ing != null) {
                        String key = ds.getKey();
                        mapTransactions.put("ingreso_" + key, new TransactionModel(
                            key,
                            ing.getMonto(),
                            ing.getCategoria(),
                            ing.getFecha(),
                            ing.getDescripcion(),
                            "INGRESO",
                            ing.getImagenBase64(),
                            ing.getTimestamp(),
                            ing.getLatitude(),
                            ing.getLongitude()
                        ));
                    }
                }
                actualizarUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Cargar Egresos
        mDatabase.child("egresos").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    G_Egresos egr = ds.getValue(G_Egresos.class);
                    if (egr != null) {
                        String key = ds.getKey();
                        mapTransactions.put("egreso_" + key, new TransactionModel(
                            key,
                            egr.getMonto(),
                            egr.getCategoria(),
                            egr.getFecha(),
                            egr.getDescripcion(),
                            "EGRESO",
                            egr.getImagenBase64(),
                            egr.getTimestamp(),
                            egr.getLatitude(),
                            egr.getLongitude()
                        ));
                    }
                }
                actualizarUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private long parseDateToMillis(String fechaStr) {
        if (fechaStr == null || fechaStr.isEmpty()) return 0L;
        String clean = fechaStr.replace("Hoy, ", "").trim();
        try {
            String[] parts = clean.split("/");
            if (parts.length == 3) {
                String day = parts[0].length() == 1 ? "0" + parts[0] : parts[0];
                String month = parts[1].length() == 1 ? "0" + parts[1] : parts[1];
                String year = parts[2].length() == 2 ? "20" + parts[2] : parts[2];
                clean = day + "/" + month + "/" + year;
            }
        } catch (Exception ignored) {}

        SimpleDateFormat[] formats = {
            new SimpleDateFormat("dd/MM/yyyy", new Locale("es", "ES")),
            new SimpleDateFormat("d/M/yyyy", new Locale("es", "ES")),
            new SimpleDateFormat("dd/MM/yy", new Locale("es", "ES")),
            new SimpleDateFormat("yyyy-MM-dd", new Locale("es", "ES"))
        };
        for (SimpleDateFormat sdf : formats) {
            try {
                sdf.setLenient(true);
                Date d = sdf.parse(clean);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    private void actualizarUI() {
        listFull.clear();
        listFull.addAll(mapTransactions.values());
        
        // Ordenar estrictamente por fecha (los más recientes primero)
        Collections.sort(listFull, (t1, t2) -> {
            long d2 = parseDateToMillis(t2.getFecha());
            long d1 = parseDateToMillis(t1.getFecha());
            return Long.compare(d2, d1);
        });
        
        historyItems.clear();
        SimpleDateFormat sdfMonthYear = new SimpleDateFormat("MMMM yyyy", new Locale("es", "ES"));
        String lastMonthYear = "";

        int end = Math.min(displayedCount, listFull.size());
        for (int i = 0; i < end; i++) {
            TransactionModel t = listFull.get(i);
            String monthYear = "";
            long millis = parseDateToMillis(t.getFecha());
            if (millis > 0) {
                monthYear = sdfMonthYear.format(new Date(millis));
                if (!monthYear.isEmpty()) {
                    monthYear = monthYear.substring(0, 1).toUpperCase() + monthYear.substring(1);
                }
            } else {
                monthYear = "Otros";
            }

            if (!monthYear.equals(lastMonthYear)) {
                historyItems.add(new HistoryItem(monthYear));
                lastMonthYear = monthYear;
            }
            historyItems.add(new HistoryItem(t));
        }

        if (listFull.size() > displayedCount) {
            btnVerMas.setVisibility(View.VISIBLE);
        } else {
            btnVerMas.setVisibility(View.GONE);
        }
        
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onTransactionClick(TransactionModel transaction) {
        showDetalleDialog(transaction);
    }

    private void showDetalleDialog(TransactionModel transaction) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_detalle_movimiento);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvCategoria = dialog.findViewById(R.id.tvDetalleCategoria);
        TextView tvMonto = dialog.findViewById(R.id.tvDetalleMonto);
        TextView tvFecha = dialog.findViewById(R.id.tvDetalleFecha);
        TextView tvNota = dialog.findViewById(R.id.tvDetalleNota);
        LinearLayout llUbicacion = dialog.findViewById(R.id.llDetalleUbicacion);
        TextView tvCoordenadas = dialog.findViewById(R.id.tvDetalleCoordenadas);
        android.widget.ImageButton btnVerMapa = dialog.findViewById(R.id.btnVerEnMapa);
        ImageView ivFoto = dialog.findViewById(R.id.ivDetalleFoto);
        TextView tvSinEvidencia = dialog.findViewById(R.id.tvSinEvidencia);
        Button btnCerrar = dialog.findViewById(R.id.btnCerrarDetalle);

        tvCategoria.setText(transaction.getCategoria());
        
        double monto = Double.parseDouble(transaction.getMonto());
        String prefix = transaction.getTipo().equals("INGRESO") ? "+ " : "- ";
        tvMonto.setText(prefix + CurrencyUtils.formatFull(monto));
        
        // Formatear fecha para el detalle
        String fechaDetalle;
        if (transaction.getTimestamp() > 0) {
            SimpleDateFormat sdfDetalle = new SimpleDateFormat("dd/MM/yy", new Locale("es", "ES"));
            fechaDetalle = sdfDetalle.format(new java.util.Date(transaction.getTimestamp()));
        } else {
            fechaDetalle = transaction.getFecha();
            if (fechaDetalle != null && fechaDetalle.contains("Hoy, ")) {
                fechaDetalle = fechaDetalle.replace("Hoy, ", "");
            }
        }
        tvFecha.setText(fechaDetalle);
        
        if (transaction.getDescripcion() != null && !transaction.getDescripcion().isEmpty() && !transaction.getDescripcion().equals("Sin Descripcion")) {
            tvNota.setText(transaction.getDescripcion());
        } else {
            tvNota.setText(getString(R.string.common_no_desc));
        }

        // Lógica de Ubicación en el Detalle
        if (transaction.getLatitude() != null && transaction.getLongitude() != null) {
            llUbicacion.setVisibility(View.VISIBLE);
            tvCoordenadas.setText(String.format(Locale.ENGLISH, "%.6f, %.6f", transaction.getLatitude(), transaction.getLongitude()));
            btnVerMapa.setOnClickListener(v -> {
                Intent intent = new Intent(this, LocationPickerActivity.class);
                intent.putExtra("route_mode", true);
                intent.putExtra("dest_lat", transaction.getLatitude());
                intent.putExtra("dest_lng", transaction.getLongitude());
                startActivity(intent);
            });
        } else {
            llUbicacion.setVisibility(View.GONE);
        }

        if (transaction.getImagenBase64() != null && !transaction.getImagenBase64().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(transaction.getImagenBase64(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivFoto.setImageBitmap(decodedByte);
                ivFoto.setVisibility(View.VISIBLE);
                tvSinEvidencia.setVisibility(View.GONE);
            } catch (Exception e) {
                ivFoto.setVisibility(View.GONE);
                tvSinEvidencia.setVisibility(View.VISIBLE);
                tvSinEvidencia.setText(getString(R.string.common_error) + " al cargar imagen");
            }
        } else {
            ivFoto.setVisibility(View.GONE);
            tvSinEvidencia.setVisibility(View.VISIBLE);
        }

        btnCerrar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.75);
            dialog.getWindow().setLayout(width, height);
        }
    }
}