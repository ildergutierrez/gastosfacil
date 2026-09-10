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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GraficosActivity extends AppCompatActivity {

    private TextView tvTotalSpent, tvCurrentMonth, tvMainPercent;
    private ImageView btnNextMonth, btnPrevMonth;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private Calendar calendarVisual = Calendar.getInstance();
    private SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM yyyy", new Locale("es", "CO"));

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graficos);

        currencyFormat.setMaximumFractionDigits(2);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        tvTotalSpent = findViewById(R.id.tvTotalSpentValue);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        tvMainPercent = findViewById(R.id.tvMainCategoryPercent);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        actualizarMesDisplay();

        btnPrevMonth.setOnClickListener(v -> {
            calendarVisual.add(Calendar.MONTH, -1);
            actualizarMesDisplay();
            cargarDatos();
        });

        btnNextMonth.setOnClickListener(v -> {
            calendarVisual.add(Calendar.MONTH, 1);
            actualizarMesDisplay();
            cargarDatos();
        });

        ImageButton btnLogo = findViewById(R.id.btnLogoGraficos);
        LinearLayout navHome = findViewById(R.id.navHomeG);
        LinearLayout navExpenses = findViewById(R.id.navExpensesG);
        LinearLayout navHistory = findViewById(R.id.navHistoryG);

        btnLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GraficosActivity.this, ConfiguracionActivity.class);
                startActivity(intent);
            }
        });

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GraficosActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        navExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GraficosActivity.this, MovimientosActivity.class);
                intent.putExtra("TYPE", "GASTO");
                startActivity(intent);
                finish();
            }
        });

        navHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GraficosActivity.this, HistoriaActivity.class);
                startActivity(intent);
                finish();
            }
        });

        cargarDatos();
    }

    private void cargarDatos() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        // Obtener el rango del mes actual en timestamps
        Calendar cal = (Calendar) calendarVisual.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfMonth = cal.getTimeInMillis();

        cal.add(Calendar.MONTH, 1);
        long endOfMonth = cal.getTimeInMillis();

        mDatabase.child("egresos").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double total = 0;
                Map<String, Double> porCategoria = new HashMap<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    G_Egresos egr = ds.getValue(G_Egresos.class);
                    if (egr != null) {
                        // Filtrar por timestamp (más fiable que el string de fecha)
                        if (egr.getTimestamp() >= startOfMonth && egr.getTimestamp() < endOfMonth) {
                            double monto = Double.parseDouble(egr.getMonto());
                            total += monto;
                            porCategoria.put(egr.getCategoria(), porCategoria.getOrDefault(egr.getCategoria(), 0.0) + monto);
                        }
                    }
                }
                actualizarResumen(total, porCategoria);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void actualizarMesDisplay() {
        tvCurrentMonth.setText(sdfMes.format(calendarVisual.getTime()));

        Calendar hoy = Calendar.getInstance();
        boolean esMismoMesYAno = (calendarVisual.get(Calendar.MONTH) == hoy.get(Calendar.MONTH) &&
                                 calendarVisual.get(Calendar.YEAR) == hoy.get(Calendar.YEAR));

        if (esMismoMesYAno) {
            btnNextMonth.setVisibility(View.INVISIBLE);
            btnNextMonth.setEnabled(false);
        } else {
            btnNextMonth.setVisibility(View.VISIBLE);
            btnNextMonth.setEnabled(true);
        }
    }

    private void actualizarResumen(double total, Map<String, Double> porCategoria) {
        tvTotalSpent.setText(CurrencyUtils.formatShort(total));
        PieChartView pieChart = findViewById(R.id.analysisPieChart);
        
        // Determinar categoría principal
        String mainCat = "";
        double maxMonto = 0;
        
        // Reset de items primero
        resetItems();

        for (Map.Entry<String, Double> entry : porCategoria.entrySet()) {
            double monto = entry.getValue();
            int porcentaje = (int) ((monto / total) * 100);
            
            if (monto > maxMonto) {
                maxMonto = monto;
                mainCat = entry.getKey();
            }

            mostrarItem(entry.getKey(), monto, porcentaje);
        }

        if (!mainCat.isEmpty()) {
            int mainPorcentaje = (int) ((maxMonto / total) * 100);
            tvMainPercent.setText(mainPorcentaje + "%");
        } else {
            tvMainPercent.setText("0%");
        }

        if (pieChart != null) {
            pieChart.setData(porCategoria);
        }
    }

    private void resetItems() {
        // Podríamos iterar sobre un array de IDs si quisiéramos ser más limpios
        View[] items = {findViewById(R.id.gItemFood), findViewById(R.id.gItemTransp), findViewById(R.id.gItemStudy), findViewById(R.id.gItemLeisure)};
        for (View v : items) {
            v.setVisibility(View.GONE);
        }
    }

    private void mostrarItem(String categoria, double monto, int porcentaje) {
        View item = null;
        int color = Color.GRAY;

        if (categoria.equalsIgnoreCase("Comida")) {
            item = findViewById(R.id.gItemFood);
            color = Color.parseColor("#FF9800"); // Naranja
        } else if (categoria.equalsIgnoreCase("Transporte")) {
            item = findViewById(R.id.gItemTransp);
            color = Color.parseColor("#2196F3"); // Azul
        } else if (categoria.equalsIgnoreCase("Estudio")) {
            item = findViewById(R.id.gItemStudy);
            color = Color.parseColor("#9C27B0"); // Morado
        } else if (categoria.equalsIgnoreCase("Ocio") || categoria.equalsIgnoreCase("Otro")) {
            item = findViewById(R.id.gItemLeisure);
            color = Color.parseColor("#E91E63"); // Rosa
        }

        if (item != null) {
            item.setVisibility(View.VISIBLE);
            ((TextView) item.findViewById(R.id.tvCategoryName)).setText(categoria);
            ((TextView) item.findViewById(R.id.tvCategoryValue)).setText(CurrencyUtils.formatShort(monto));
            TextView tvPct = item.findViewById(R.id.tvCategoryPercent);
            tvPct.setText(porcentaje + "%");
            tvPct.setTextColor(color);
            item.findViewById(R.id.vCategoryDot).setBackgroundColor(color);
        }
    }

    private void setupCategoryItems() {
        // Ya no es necesario con el nuevo sistema dinámico, pero lo dejamos vacío para no romper el código anterior
    }
}