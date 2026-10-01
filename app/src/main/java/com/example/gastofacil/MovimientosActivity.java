package com.example.gastofacil;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.Priority;
import android.location.Address;
import android.location.Geocoder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MovimientosActivity extends AppCompatActivity {

    private TextView tvToggleIngreso, tvToggleEgreso;
    private TextView tvAmountLabel, tvDateDisplay, tvAvailableBalanceM;
    private EditText etAmount, etNote;
    private TextView tvCatComida, tvCatTransporte, tvCatEstudio, tvCatOtro;
    private RelativeLayout rlDatePicker;
    private LinearLayout llPhotoLocationContainer;
    private Button btnConfirm;
    private ImageView ivCapturedPhoto, ivMapIcon;

    private String selectedCategory = "";
    private Bitmap capturedBitmap = null;
    private Double selectedLat = null;
    private Double selectedLng = null;
    private FusedLocationProviderClient fusedLocationClient;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private double saldoActual = 0.0;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    getCurrentLocation();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    getCurrentLocation();
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> locationPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedLat = result.getData().getDoubleExtra("lat", 0);
                    selectedLng = result.getData().getDoubleExtra("lng", 0);
                    Toast.makeText(this, "Ubicación seleccionada manualmente", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    capturedBitmap = (Bitmap) extras.get("data");
                    ivCapturedPhoto.setImageBitmap(capturedBitmap);
                    ivCapturedPhoto.setVisibility(View.VISIBLE);
                    findViewById(R.id.tvPhotoHint).setVisibility(View.GONE);
                }
            }
    );

    private boolean isIngreso = true;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", new Locale("es", "ES"));

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movimientos);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        cargarSaldoActual();

        // UI References
        ImageButton btnLogo = findViewById(R.id.btnLogoM);
        tvToggleIngreso = findViewById(R.id.tvToggleIngreso);
        tvToggleEgreso = findViewById(R.id.tvToggleEgreso);
        tvAmountLabel = findViewById(R.id.tvAmountLabel);
        tvDateDisplay = findViewById(R.id.tvDateDisplay);
        tvAvailableBalanceM = findViewById(R.id.tvAvailableBalanceM);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        tvCatComida = findViewById(R.id.tvCatComida);
        tvCatTransporte = findViewById(R.id.tvCatTransporte);
        tvCatEstudio = findViewById(R.id.tvCatEstudio);
        tvCatOtro = findViewById(R.id.tvCatOtro);
        rlDatePicker = findViewById(R.id.rlDatePicker);
        llPhotoLocationContainer = findViewById(R.id.llPhotoLocationContainer);
        btnConfirm = findViewById(R.id.btnConfirm);
        ivCapturedPhoto = findViewById(R.id.ivCapturedPhoto);
        ivMapIcon = findViewById(R.id.ivMapIcon);
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermissions();

        // Navigation
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navExpenses = findViewById(R.id.navExpenses);
        LinearLayout navHistory = findViewById(R.id.navHistory);
        LinearLayout navCharts = findViewById(R.id.navCharts);

        // Get initial type
        String initialType = getIntent().getStringExtra("TYPE");
        if ("GASTO".equals(initialType)) {
            isIngreso = false;
        }

        // Initialize UI
        updateToggleUI();
        tvDateDisplay.setText(sdf.format(calendar.getTime()));
        selectCategory(tvCatComida); // Seleccionada por defecto

        // Toggle Listeners
        tvToggleIngreso.setOnClickListener(v -> {
            isIngreso = true;
            updateToggleUI();
        });

        tvToggleEgreso.setOnClickListener(v -> {
            isIngreso = false;
            updateToggleUI();
        });

        // Amount Logic: Only numbers, color managed by toggle
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // We handle only the number here, no prefix in the EditText itself
            }
        });

        // Category Listeners
        View.OnClickListener categoryListener = v -> selectCategory((TextView) v);
        tvCatComida.setOnClickListener(categoryListener);
        tvCatTransporte.setOnClickListener(categoryListener);
        tvCatEstudio.setOnClickListener(categoryListener);
        tvCatOtro.setOnClickListener(categoryListener);

        // Date Picker
        rlDatePicker.setOnClickListener(v -> showDatePicker());

        // Photo Picker specific click (Camera)
        findViewById(R.id.ivCameraIcon).setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                cameraLauncher.launch(takePictureIntent);
            } else {
                Toast.makeText(this, getString(R.string.m_camera_error), Toast.LENGTH_SHORT).show();
            }
        });

        // Location Picker click (Map)
        ivMapIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationPickerActivity.class);
            if (selectedLat != null && selectedLng != null) {
                intent.putExtra("lat", selectedLat);
                intent.putExtra("lng", selectedLng);
            }
            locationPickerLauncher.launch(intent);
        });

        // Confirm Button
        btnConfirm.setOnClickListener(v -> {
            if (isIngreso) {
                registrarMovimiento();
            } else {
                registrarEgreso();
            }
        });

        // Logo to future layout
        btnLogo.setOnClickListener(v -> {
            Intent intent = new Intent(MovimientosActivity.this, ConfiguracionActivity.class);
            startActivity(intent);
        });

        // Bottom Nav Logic
        navHome.setOnClickListener(v -> navigateTo(DashboardActivity.class));
        navExpenses.setOnClickListener(v -> {
            // Already here, but reset if needed
            Toast.makeText(this, getString(R.string.m_already_in), Toast.LENGTH_SHORT).show();
        });
        navHistory.setOnClickListener(v -> navigateTo(HistoriaActivity.class));
        navCharts.setOnClickListener(v -> navigateTo(GraficosActivity.class));
    }

    private void registrarEgreso() {
        String montoStr = etAmount.getText().toString().trim();
        String nota = etNote.getText().toString().trim();
        String fecha = tvDateDisplay.getText().toString();

        // Validaciones
        if (montoStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.m_enter_amount), Toast.LENGTH_SHORT).show();
            return;
        }

        double monto = Double.parseDouble(montoStr);
        if (monto > saldoActual) {
            Toast.makeText(this, String.format(getString(R.string.m_insufficient_funds), CurrencyUtils.formatShort(saldoActual)), Toast.LENGTH_LONG).show();
            return;
        }

        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, getString(R.string.m_select_category), Toast.LENGTH_SHORT).show();
            return;
        }

        if (nota.isEmpty()) {
            nota = getString(R.string.descripcion);
        }

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, getString(R.string.m_user_not_auth), Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressBar pbConfirm = findViewById(R.id.pbConfirm);
        btnConfirm.setEnabled(false);
        btnConfirm.setText("");
        pbConfirm.setVisibility(View.VISIBLE);

        // Procesar imagen si existe
        String b64Image = "";
        if (capturedBitmap != null) {
            b64Image = procesarImagenLigera(capturedBitmap);
        }

        // Crear objeto
        String id = mDatabase.child("egresos").child(userId).push().getKey();
        G_Egresos egreso = new G_Egresos(id, montoStr, selectedCategory, fecha, nota, b64Image, System.currentTimeMillis(), selectedLat, selectedLng);

        // Guardar en Firebase
        if (id != null) {
            mDatabase.child("egresos").child(userId).child(id).setValue(egreso)
                    .addOnCompleteListener(task -> {
                        pbConfirm.setVisibility(View.GONE);
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText(R.string.ng_confirm_button);

                        if (task.isSuccessful()) {
                            actualizarSaldo(-monto);
                            Toast.makeText(MovimientosActivity.this, getString(R.string.m_save_success), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(MovimientosActivity.this, getString(R.string.m_save_error), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void registrarMovimiento() {
        String montoStr = etAmount.getText().toString().trim();
        String nota = etNote.getText().toString().trim();
        String fecha = tvDateDisplay.getText().toString();
        
        // Validaciones
        if (montoStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.m_enter_amount), Toast.LENGTH_SHORT).show();
            return;
        }
        
        double monto = Double.parseDouble(montoStr);

        if (selectedCategory.isEmpty()) {
            Toast.makeText(this, getString(R.string.m_select_category), Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (nota.isEmpty()) {
            nota = "Sin Descripcion";
        }

        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, getString(R.string.m_user_not_auth), Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressBar pbConfirm = findViewById(R.id.pbConfirm);
        btnConfirm.setEnabled(false);
        btnConfirm.setText("");
        pbConfirm.setVisibility(View.VISIBLE);

        // Procesar imagen si existe
        String b64Image = "";
        if (capturedBitmap != null) {
            b64Image = procesarImagenLigera(capturedBitmap);
        }

        // Crear objeto
        String id = mDatabase.child("ingresos").child(userId).push().getKey();
        G_Ingresos ingreso = new G_Ingresos(id, montoStr, selectedCategory, fecha, nota, b64Image, System.currentTimeMillis(), selectedLat, selectedLng);

        // Guardar en Firebase
        if (id != null) {
            mDatabase.child("ingresos").child(userId).child(id).setValue(ingreso)
                    .addOnCompleteListener(task -> {
                        pbConfirm.setVisibility(View.GONE);
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText(R.string.ni_confirm_button);

                        if (task.isSuccessful()) {
                            actualizarSaldo(monto);
                            Toast.makeText(MovimientosActivity.this, getString(R.string.m_save_success), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(MovimientosActivity.this, getString(R.string.m_save_error), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private String procesarImagenLigera(Bitmap bitmap) {
        try {
            // 1. Redimensionar a un tamaño pequeño (max 500px)
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale = (float) 500 / width;
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 500, (int) (height * scale), true);

            // 2. Comprimir calidad al 30%
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 30, stream);
            byte[] byteArray = stream.toByteArray();

            // 3. Convertir a Texto
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            return "";
        }
    }

    private void requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(10000)
                .build();

        fusedLocationClient.getCurrentLocation(request, null).addOnSuccessListener(this, location -> {
            if (location != null) {
                selectedLat = location.getLatitude();
                selectedLng = location.getLongitude();
                String address = getAddressFromCoordinates(this, selectedLat, selectedLng);
                Toast.makeText(this, "Dirección real: " + address, Toast.LENGTH_LONG).show();
            } else {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, lastLoc -> {
                    if (lastLoc != null) {
                        selectedLat = lastLoc.getLatitude();
                        selectedLng = lastLoc.getLongitude();
                        String address = getAddressFromCoordinates(this, selectedLat, selectedLng);
                        Toast.makeText(this, "Dirección (última conocida): " + address, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "No se pudo obtener el GPS. Toca el mapa para seleccionar.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private String getAddressFromCoordinates(android.content.Context context, double lat, double lng) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) sb.append(", ");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", lat, lng);
    }

    private void updateToggleUI() {
        if (isIngreso) {
            tvToggleIngreso.setBackgroundResource(R.drawable.toggle_item_active);
            tvToggleIngreso.setTextColor(Color.WHITE);
            tvToggleEgreso.setBackgroundResource(android.R.color.transparent);
            tvToggleEgreso.setTextColor(Color.parseColor("#78909C"));
            
            tvAmountLabel.setText(R.string.ni_amount_label);
            etAmount.setTextColor(Color.parseColor("#263238"));
            btnConfirm.setText(R.string.ni_confirm_button);
            btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.transparent));
            btnConfirm.setBackgroundResource(R.drawable.confirm_button_bg);
            btnConfirm.setBackgroundTintList(null); // Use drawable color
        } else {
            tvToggleEgreso.setBackgroundResource(R.drawable.toggle_item_active);
            tvToggleEgreso.setTextColor(Color.WHITE);
            tvToggleIngreso.setBackgroundResource(android.R.color.transparent);
            tvToggleIngreso.setTextColor(Color.parseColor("#78909C"));

            tvAmountLabel.setText(R.string.ng_amount_label);
            etAmount.setTextColor(Color.RED);
            btnConfirm.setText(R.string.ng_confirm_button);
            // For egreso, we might want a red button, but the image shows green for "Confirmar Ingreso".
            // If the user is in "Egreso", it should probably be red or the same theme.
            // I'll use red for Egreso to be clear.
            btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
        }
    }

    private void selectCategory(TextView selected) {
        // Resetear todos los chips
        tvCatComida.setBackgroundResource(R.drawable.category_chip_bg);
        tvCatComida.setTextColor(Color.BLACK);
        tvCatTransporte.setBackgroundResource(R.drawable.category_chip_bg);
        tvCatTransporte.setTextColor(Color.BLACK);
        tvCatEstudio.setBackgroundResource(R.drawable.category_chip_bg);
        tvCatEstudio.setTextColor(Color.BLACK);
        tvCatOtro.setBackgroundResource(R.drawable.category_chip_bg);
        tvCatOtro.setTextColor(Color.BLACK);

        // Seleccionar el actual
        selected.setBackgroundResource(R.drawable.category_chip_selected_bg);
        selected.setTextColor(ContextCompat.getColor(this, R.color.nav_active));
        
        // Guardar la categoría seleccionada
        selectedCategory = selected.getText().toString();
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    tvDateDisplay.setText(sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void navigateTo(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void cargarSaldoActual() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId != null) {
            mDatabase.child("usuarios").child(userId).child("saldo").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().getValue() != null) {
                    saldoActual = Double.parseDouble(task.getResult().getValue().toString());
                    tvAvailableBalanceM.setText(String.format(getString(R.string.m_available_balance), CurrencyUtils.formatBalance(saldoActual)));
                }
            });
        }
    }

    private void actualizarSaldo(double variacion) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId != null) {
            double nuevoSaldo = saldoActual + variacion;
            mDatabase.child("usuarios").child(userId).child("saldo").setValue(nuevoSaldo);

            // También actualizar totales
            if (variacion > 0) {
                mDatabase.child("usuarios").child(userId).child("totalIngresos").get().addOnCompleteListener(task -> {
                    double currentTotal = task.isSuccessful() && task.getResult().getValue() != null ? 
                            Double.parseDouble(task.getResult().getValue().toString()) : 0.0;
                    mDatabase.child("usuarios").child(userId).child("totalIngresos").setValue(currentTotal + variacion);
                });
            } else {
                mDatabase.child("usuarios").child(userId).child("totalEgresos").get().addOnCompleteListener(task -> {
                    double currentTotal = task.isSuccessful() && task.getResult().getValue() != null ? 
                            Double.parseDouble(task.getResult().getValue().toString()) : 0.0;
                    mDatabase.child("usuarios").child(userId).child("totalEgresos").setValue(currentTotal + Math.abs(variacion));
                });
            }
        }
    }
}