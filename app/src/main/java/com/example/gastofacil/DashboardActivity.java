package com.example.gastofacil;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvBalance, tvIncomes, tvExpenses;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        currencyFormat.setMaximumFractionDigits(2);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // UI
        tvBalance = findViewById(R.id.tvBalance);
        tvIncomes = findViewById(R.id.tvIncomes);
        tvExpenses = findViewById(R.id.tvExpenses);
        TextView tvViewAll = findViewById(R.id.tvViewAll);
        ImageButton btnLogo = findViewById(R.id.btnLogoDashboard);
        MaterialCardView btnRegistrarMovimiento = findViewById(R.id.btnRegistrarMovimiento);
        LinearLayout navExpenses = findViewById(R.id.navExpenses);
        LinearLayout navHistory = findViewById(R.id.navHistory);
        LinearLayout navCharts = findViewById(R.id.navCharts);

        btnLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, ConfiguracionActivity.class);
                startActivity(intent);
            }
        });

        tvViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, HistoriaActivity.class);
            startActivity(intent);
        });

        // Click en el botón de registrar movimiento
        btnRegistrarMovimiento.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, MovimientosActivity.class);
                startActivity(intent);
            }
        });

        // Click en el botón Gastos de la barra inferior
        navExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, MovimientosActivity.class);
                startActivity(intent);
            }
        });

        // Click en el botón Historial de la barra inferior
        navHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, HistoriaActivity.class);
                startActivity(intent);
            }
        });

        // Click en el botón Gráficos de la barra inferior
        navCharts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, GraficosActivity.class);
                startActivity(intent);
            }
        });

        cargarResumen();
        setupTransactionItems();
    }

    private void cargarResumen() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId != null) {
            mDatabase.child("usuarios").child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        gasto_user user = snapshot.getValue(gasto_user.class);
                        if (user != null) {
                            tvBalance.setText(CurrencyUtils.formatBalance(user.getSaldo()));
                            
                            // Formato abreviado para ingresos y gastos
                            String inc = CurrencyUtils.formatShort(user.getTotalIngresos());
                            String exp = CurrencyUtils.formatShort(user.getTotalEgresos());
                            tvIncomes.setText("+" + inc);
                            tvExpenses.setText("-" + exp);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(DashboardActivity.this, getString(R.string.h_loading_error), Toast.LENGTH_SHORT).show();
                }
            });
            
            // Cargar categoría principal para el texto del dashboard
            cargarCategoriaPrincipal(userId);
        }
    }

    private void cargarCategoriaPrincipal(String userId) {
        mDatabase.child("egresos").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    findViewById(R.id.tvSummaryDesc).setVisibility(View.GONE);
                    return;
                }

                Map<String, Double> conteo = new HashMap<>();
                double total = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    G_Egresos egr = ds.getValue(G_Egresos.class);
                    if (egr != null) {
                        double m = Double.parseDouble(egr.getMonto());
                        total += m;
                        conteo.put(egr.getCategoria(), conteo.getOrDefault(egr.getCategoria(), 0.0) + m);
                    }
                }

                String maxCat = "";
                double maxVal = 0;
                for (Map.Entry<String, Double> entry : conteo.entrySet()) {
                    if (entry.getValue() > maxVal) {
                        maxVal = entry.getValue();
                        maxCat = entry.getKey();
                    }
                }

                if (!maxCat.isEmpty()) {
                    int porc = (int) ((maxVal / total) * 100);
                    TextView tvSummary = findViewById(R.id.tvSummaryDesc);
                    TextView tvChartPercent = findViewById(R.id.tvDashboardChartPercent);
                    PieChartView pieChart = findViewById(R.id.dashboardPieChart);
                    
                    tvSummary.setVisibility(View.VISIBLE);
                    tvSummary.setText(String.format(getString(R.string.d_main_cat_text), maxCat, porc));
                    
                    if (tvChartPercent != null) {
                        tvChartPercent.setText(porc + "%");
                    }
                    if (pieChart != null) {
                        pieChart.setData(conteo);
                    }
                } else {
                    TextView tvChartPercent = findViewById(R.id.tvDashboardChartPercent);
                    PieChartView pieChart = findViewById(R.id.dashboardPieChart);
                    if (tvChartPercent != null) tvChartPercent.setText("0%");
                    if (pieChart != null) pieChart.setData(new HashMap<>());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupTransactionItems() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        // Inicialmente ocultar todos
        findViewById(R.id.item1).setVisibility(View.GONE);
        findViewById(R.id.item2).setVisibility(View.GONE);
        findViewById(R.id.item3).setVisibility(View.GONE);

        List<TransactionModel> allTransactions = new ArrayList<>();

        // Escuchar Ingresos
        mDatabase.child("ingresos").child(userId).limitToLast(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Para simplificar, recargamos la lista combinada cada vez que uno de los dos cambie
                procesarMovimientos(snapshot, allTransactions, "INGRESO", userId);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Escuchar Egresos
        mDatabase.child("egresos").child(userId).limitToLast(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                procesarMovimientos(snapshot, allTransactions, "EGRESO", userId);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private synchronized void procesarMovimientos(DataSnapshot snapshot, List<TransactionModel> all, String tipo, String userId) {
        // Eliminar los del mismo tipo antes de re-añadir
        all.removeIf(t -> t.getTipo().equals(tipo));
        
        for (DataSnapshot ds : snapshot.getChildren()) {
            if (tipo.equals("INGRESO")) {
                G_Ingresos ing = ds.getValue(G_Ingresos.class);
                if (ing != null) all.add(new TransactionModel(ing.getId(), ing.getMonto(), ing.getCategoria(), ing.getFecha(), ing.getDescripcion(), tipo, ing.getImagenBase64(), ing.getTimestamp()));
            } else {
                G_Egresos egr = ds.getValue(G_Egresos.class);
                if (egr != null) all.add(new TransactionModel(egr.getId(), egr.getMonto(), egr.getCategoria(), egr.getFecha(), egr.getDescripcion(), tipo, egr.getImagenBase64(), egr.getTimestamp()));
            }
        }

        // Ordenar por timestamp (los más recientes primero)
        Collections.sort(all, (t1, t2) -> Long.compare(t2.getTimestamp(), t1.getTimestamp()));

        // Mostrar los 3 primeros y ocultar el resto
        for (int i = 1; i <= 3; i++) {
            int resId = getResources().getIdentifier("item" + i, "id", getPackageName());
            View itemView = findViewById(resId);
            
            if (i <= all.size()) {
                TransactionModel t = all.get(i - 1);
                boolean isIng = t.getTipo().equals("INGRESO");
                String prefix = isIng ? "+" : "-";
                
                itemView.setVisibility(View.VISIBLE);
                
                // Formatear fecha
                String fechaItem;
                if (t.getTimestamp() > 0) {
                    SimpleDateFormat sdfItem = new SimpleDateFormat("dd/MM/yy", new Locale("es", "ES"));
                    fechaItem = sdfItem.format(new Date(t.getTimestamp()));
                } else {
                    fechaItem = t.getFecha();
                    if (fechaItem != null && fechaItem.contains("Hoy, ")) {
                        fechaItem = fechaItem.replace("Hoy, ", "");
                    }
                }
                
                actualizarItem(i, t.getCategoria(), fechaItem, prefix + CurrencyUtils.formatShort(Double.parseDouble(t.getMonto())), isIng);
            } else {
                if (itemView != null) itemView.setVisibility(View.GONE);
            }
        }
    }

    private void actualizarItem(int index, String nombre, String desc, String valor, boolean isIngreso) {
        int resId = getResources().getIdentifier("item" + index, "id", getPackageName());
        View item = findViewById(resId);
        if (item != null) {
            ((TextView) item.findViewById(R.id.tvTransName)).setText(nombre);
            ((TextView) item.findViewById(R.id.tvTransDesc)).setText(desc);
            TextView tvVal = item.findViewById(R.id.tvTransValue);
            tvVal.setText(valor);
            if (isIngreso) {
                tvVal.setTextColor(getResources().getColor(R.color.income_green));
                ((ImageView) item.findViewById(R.id.ivTransIcon)).setImageResource(R.drawable.ic_trend_up);
            } else {
                tvVal.setTextColor(Color.RED);
                ((ImageView) item.findViewById(R.id.ivTransIcon)).setImageResource(R.drawable.ic_trend_down);
            }
        }
    }
}