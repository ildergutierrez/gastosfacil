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
    private List<TransactionModel> listDisplayed = new ArrayList<>();
    private Map<String, TransactionModel> mapTransactions = new HashMap<>();
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private int displayedCount = 10;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historia);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        rvHistorial = findViewById(R.id.rvHistorial);
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(listDisplayed, this);
        rvHistorial.setAdapter(adapter);

        btnVerMas = findViewById(R.id.btnVerMas);
        btnVerMas.setOnClickListener(v -> {
            displayedCount += 20;
            actualizarUI();
        });

        ImageButton btnLogo = findViewById(R.id.btnLogoHistoria);
        LinearLayout navHome = findViewById(R.id.navHomeH);
        LinearLayout navExpenses = findViewById(R.id.navExpensesH);
        LinearLayout navCharts = findViewById(R.id.navChartsH);

        btnLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HistoriaActivity.this, ConfiguracionActivity.class);
                startActivity(intent);
            }
        });

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
                        mapTransactions.put(ing.getId(), new TransactionModel(ing.getId(), ing.getMonto(), ing.getCategoria(), ing.getFecha(), ing.getDescripcion(), "INGRESO", ing.getImagenBase64(), ing.getTimestamp()));
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
                        mapTransactions.put(egr.getId(), new TransactionModel(egr.getId(), egr.getMonto(), egr.getCategoria(), egr.getFecha(), egr.getDescripcion(), "EGRESO", egr.getImagenBase64(), egr.getTimestamp()));
                    }
                }
                actualizarUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void actualizarUI() {
        listFull.clear();
        listFull.addAll(mapTransactions.values());
        
        // Ordenar por timestamp (los más recientes primero)
        Collections.sort(listFull, (t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));
        
        listDisplayed.clear();
        int end = Math.min(displayedCount, listFull.size());
        for (int i = 0; i < end; i++) {
            listDisplayed.add(listFull.get(i));
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