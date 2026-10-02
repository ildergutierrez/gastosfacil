package com.example.gastofacil;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Locale;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class ConfiguracionActivity extends AppCompatActivity {

    private EditText etNombre, etCorreo, etTelefono, etFechaNacimiento;
    private Spinner spinnerLenguaje, spinnerGenero, spinnerPais;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String idiomaActual = "es";
    private String correoActual = "";

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracion);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etNombre = findViewById(R.id.etNombreConfig);
        etCorreo = findViewById(R.id.etCorreoConfig);
        etTelefono = findViewById(R.id.etTelefonoConfig);
        spinnerPais = findViewById(R.id.spinnerPais);
        etFechaNacimiento = findViewById(R.id.etFechaNacimientoConfig);
        spinnerLenguaje = findViewById(R.id.spinnerLenguaje);
        spinnerGenero = findViewById(R.id.spinnerGenero);

        // Configurar Spinner de Idioma
        String[] idiomas = {"Español", "English"};
        ArrayAdapter<String> adapterIdiomas = new ArrayAdapter<>(this, R.layout.spinner_item, idiomas);
        adapterIdiomas.setDropDownViewResource(R.layout.spinner_item);
        spinnerLenguaje.setAdapter(adapterIdiomas);

        // Configurar Spinner de Género
        String[] generos = {"Otro", "Masculino", "Femenino"};
        if (LocaleHelper.getLanguage(this).equals("en")) {
            generos = new String[]{"Other", "Male", "Female"};
        }
        ArrayAdapter<String> adapterGeneros = new ArrayAdapter<>(this, R.layout.spinner_item, generos);
        adapterGeneros.setDropDownViewResource(R.layout.spinner_item);
        spinnerGenero.setAdapter(adapterGeneros);

        // Configurar Spinner de País
        ArrayAdapter<CharSequence> adapterPaises = ArrayAdapter.createFromResource(this,
                R.array.paises_america, R.layout.spinner_item);
        adapterPaises.setDropDownViewResource(R.layout.spinner_item);
        spinnerPais.setAdapter(adapterPaises);

        // Configurar DatePicker para Fecha de Nacimiento
        etFechaNacimiento.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(ConfiguracionActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        String fecha = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                        etFechaNacimiento.setText(fecha);
                    }, year, month, day);
            datePickerDialog.show();
        });

        cargarDatosUsuario();

        ImageButton btnLogo = findViewById(R.id.btnLogoConfig);
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        Button btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        Button btnCambiarContrasena = findViewById(R.id.btnCambiarContrasena);
        TextView tvTerminos = findViewById(R.id.tvTerminos);
        TextView tvPrivacidad = findViewById(R.id.tvPrivacidad);

        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navExpenses = findViewById(R.id.navExpenses);
        LinearLayout navHistory = findViewById(R.id.navHistory);
        LinearLayout navCharts = findViewById(R.id.navCharts);

        // Volver al dashboard al presionar el logo
        btnLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Cerrar sesión
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(ConfiguracionActivity.this, inicioSesion.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                Toast.makeText(ConfiguracionActivity.this, getString(R.string.c_logout_confirm), Toast.LENGTH_SHORT).show();
            }
        });

        androidx.appcompat.widget.SwitchCompat switchBiometria = findViewById(R.id.switchBiometria);
        android.content.SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        boolean biometricEnabled = loginPrefs.getBoolean("biometric_enabled", false);
        switchBiometria.setChecked(biometricEnabled);

        switchBiometria.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                promptAndEnableBiometrics(switchBiometria);
            } else {
                loginPrefs.edit().putBoolean("biometric_enabled", false).apply();
                Toast.makeText(ConfiguracionActivity.this, "Biometría desactivada", Toast.LENGTH_SHORT).show();
            }
        });

        // Guardar cambios generales
        ProgressBar pbConfig = findViewById(R.id.pbConfig);
        btnGuardarCambios.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseUser userAuth = mAuth.getCurrentUser();
                String userId = userAuth != null ? userAuth.getUid() : null;
                if (userId != null) {
                    btnGuardarCambios.setEnabled(false);
                    btnGuardarCambios.setText("");
                    pbConfig.setVisibility(View.VISIBLE);

                    String nuevoNombre = etNombre.getText().toString().trim();
                    String nuevoTelefono = etTelefono.getText().toString().trim();
                    String nuevoCorreo = etCorreo.getText().toString().trim();
                    String nuevoPais = spinnerPais.getSelectedItem().toString();
                    String nuevaFecha = etFechaNacimiento.getText().toString().trim();
                    
                    // Obtener género de forma independiente al idioma (guardamos valor en inglés para consistencia)
                    String nuevoGenero;
                    int generoPos = spinnerGenero.getSelectedItemPosition();
                    if (generoPos == 1) nuevoGenero = "Male";
                    else if (generoPos == 2) nuevoGenero = "Female";
                    else nuevoGenero = "Other";

                    String nuevoIdioma = (spinnerLenguaje.getSelectedItemPosition() == 1) ? "en" : "es";

                    if (!nuevoCorreo.equals(correoActual)) {
                        showReauthenticateDialog(nuevoCorreo);
                        // El guardado del resto se hace dentro del proceso de cambio de correo si es exitoso, 
                        // o podemos guardar lo demas aqui y solo el correo aparte.
                        // Para simplicidad, guardaremos los datos basicos y pediremos reauth para el correo.
                    }

                    mDatabase.child("usuarios").child(userId).child("nombre").setValue(nuevoNombre);
                    mDatabase.child("usuarios").child(userId).child("telefono").setValue(nuevoTelefono);
                    mDatabase.child("usuarios").child(userId).child("pais").setValue(nuevoPais);
                    mDatabase.child("usuarios").child(userId).child("fechaNacimiento").setValue(nuevaFecha);
                    mDatabase.child("usuarios").child(userId).child("genero").setValue(nuevoGenero);
                    mDatabase.child("usuarios").child(userId).child("idioma").setValue(nuevoIdioma)
                            .addOnCompleteListener(task -> {
                                pbConfig.setVisibility(View.GONE);
                                btnGuardarCambios.setEnabled(true);
                                btnGuardarCambios.setText(R.string.c_save_changes);

                                if (task.isSuccessful()) {
                                    if (!nuevoIdioma.equals(idiomaActual)) {
                                        LocaleHelper.applyLocale(ConfiguracionActivity.this, nuevoIdioma);
                                        idiomaActual = nuevoIdioma;
                                        recreate();
                                    }
                                    if (nuevoCorreo.equals(correoActual)) {
                                        Toast.makeText(ConfiguracionActivity.this, getString(R.string.c_profile_updated), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        // Abrir modal de cambiar contraseña
        btnCambiarContrasena.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangePasswordDialog();
            }
        });

        // Navegación a Términos
        tvTerminos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfiguracionActivity.this, TerminosActivity.class);
                startActivity(intent);
            }
        });

        // Navegación a Privacidad
        tvPrivacidad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfiguracionActivity.this, PrivacidadActivity.class);
                startActivity(intent);
            }
        });

        // Navegación inferior
        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfiguracionActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        navExpenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfiguracionActivity.this, MovimientosActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        navHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfiguracionActivity.this, HistoriaActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        navCharts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfiguracionActivity.this, GraficosActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
    }

    private void cargarDatosUsuario() {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId != null) {
            mDatabase.child("usuarios").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        gasto_user user = snapshot.getValue(gasto_user.class);
                        if (user != null) {
                            etNombre.setText(user.getNombre());
                            etCorreo.setText(user.getCorreo());
                            correoActual = user.getCorreo();
                            etTelefono.setText(user.getTelefono());
                            
                            // Seleccionar País en Spinner
                            if (user.getPais() != null) {
                                for (int i = 0; i < spinnerPais.getCount(); i++) {
                                    if (spinnerPais.getItemAtPosition(i).toString().equalsIgnoreCase(user.getPais())) {
                                        spinnerPais.setSelection(i);
                                        break;
                                    }
                                }
                            }

                            etFechaNacimiento.setText(user.getFechaNacimiento());
                            
                            // Seleccionar Género en Spinner (Comparar con valores internos en inglés o español antiguos)
                            if (user.getGenero() != null) {
                                String g = user.getGenero();
                                if (g.equalsIgnoreCase("Male") || g.equalsIgnoreCase("Masculino")) {
                                    spinnerGenero.setSelection(1);
                                } else if (g.equalsIgnoreCase("Female") || g.equalsIgnoreCase("Femenino")) {
                                    spinnerGenero.setSelection(2);
                                } else {
                                    spinnerGenero.setSelection(0);
                                }
                            }

                            idiomaActual = user.getIdioma() != null ? user.getIdioma() : "es";
                            if (idiomaActual.equals("en")) {
                                spinnerLenguaje.setSelection(1);
                            } else {
                                spinnerLenguaje.setSelection(0);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void showChangePasswordDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_cambio_contrasena);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etCurrentPass = dialog.findViewById(R.id.etCurrentPassword);
        EditText etNewPass = dialog.findViewById(R.id.etNewPassword);
        Button btnSave = dialog.findViewById(R.id.btnSavePassword);
        ProgressBar pbDialogPass = dialog.findViewById(R.id.pbDialogPass);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String current = etCurrentPass.getText().toString();
                String newPass = etNewPass.getText().toString();

                if (current.isEmpty() || newPass.isEmpty()) {
                    Toast.makeText(ConfiguracionActivity.this, getString(R.string.m_fill_fields), Toast.LENGTH_SHORT).show();
                } else if (!isValidPassword(newPass)) {
                    Toast.makeText(ConfiguracionActivity.this, getString(R.string.c_pass_requirement), Toast.LENGTH_LONG).show();
                } else {
                    btnSave.setEnabled(false);
                    btnSave.setText("");
                    pbDialogPass.setVisibility(View.VISIBLE);

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), current);
                        user.reauthenticate(credential).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                user.updatePassword(newPass).addOnCompleteListener(task2 -> {
                                    pbDialogPass.setVisibility(View.GONE);
                                    btnSave.setEnabled(true);
                                    btnSave.setText(R.string.c_save);

                                    if (task2.isSuccessful()) {
                                        Toast.makeText(ConfiguracionActivity.this, getString(R.string.c_password_updated), Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    } else {
                                        Toast.makeText(ConfiguracionActivity.this, getString(R.string.c_password_error), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                pbDialogPass.setVisibility(View.GONE);
                                btnSave.setEnabled(true);
                                btnSave.setText(R.string.c_save);
                                Toast.makeText(ConfiguracionActivity.this, getString(R.string.c_wrong_password), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });

        dialog.show();

        // Configurar el ancho al 95% del ancho de la pantalla
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
            dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showReauthenticateDialog(String nuevoCorreo) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_reautenticar_correo);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText etPass = dialog.findViewById(R.id.etReauthPassword);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirmEmailChange);
        ProgressBar pbDialogReauth = dialog.findViewById(R.id.pbDialogReauth);

        btnConfirm.setOnClickListener(v -> {
            String password = etPass.getText().toString();
            if (password.isEmpty()) {
                Toast.makeText(this, getString(R.string.c_enter_pass), Toast.LENGTH_SHORT).show();
            } else {
                btnConfirm.setEnabled(false);
                btnConfirm.setText("");
                pbDialogReauth.setVisibility(View.VISIBLE);
                reautenticarYCambiarCorreo(password, nuevoCorreo, dialog, pbDialogReauth, btnConfirm);
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95);
            dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void reautenticarYCambiarCorreo(String password, String nuevoCorreo, Dialog dialog, ProgressBar pb, Button btn) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updateEmail(nuevoCorreo).addOnCompleteListener(taskEmail -> {
                        pb.setVisibility(View.GONE);
                        btn.setEnabled(true);
                        btn.setText(R.string.c_save);

                        if (taskEmail.isSuccessful()) {
                            // Actualizar en la BBDD
                            mDatabase.child("usuarios").child(user.getUid()).child("correo").setValue(nuevoCorreo);
                            Toast.makeText(this, getString(R.string.c_email_change_success), Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            
                            // Cerrar sesión
                            mAuth.signOut();
                            getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit().clear().apply();
                            Intent intent = new Intent(ConfiguracionActivity.this, inicioSesion.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            String error = taskEmail.getException() != null ? taskEmail.getException().getMessage() : "Unknown";
                            Toast.makeText(this, String.format(getString(R.string.c_email_update_error), error), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    pb.setVisibility(View.GONE);
                    btn.setEnabled(true);
                    btn.setText(R.string.c_save);
                    Toast.makeText(this, getString(R.string.c_wrong_password), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean isValidPassword(String password) {
        // Al menos 8 caracteres, una mayúscula, una minúscula y un número
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        return password.matches(pattern);
    }

    private void promptAndEnableBiometrics(androidx.appcompat.widget.SwitchCompat switchBiometria) {
        androidx.biometric.BiometricManager biometricManager = androidx.biometric.BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG | androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            case androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS:
                break;
            case androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(this, "Este dispositivo no cuenta con sensor biométrico", Toast.LENGTH_SHORT).show();
                switchBiometria.setChecked(false);
                return;
            case androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(this, "No hay huellas registradas en el dispositivo.", Toast.LENGTH_LONG).show();
                switchBiometria.setChecked(false);
                return;
            default:
                Toast.makeText(this, "Autenticación biométrica no disponible", Toast.LENGTH_SHORT).show();
                switchBiometria.setChecked(false);
                return;
        }

        android.content.SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String savedPassword = prefs.getString("password", "");
        String savedEmail = prefs.getString("email", "");

        if (savedPassword.isEmpty() || savedEmail.isEmpty()) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null && currentUser.getEmail() != null) {
                showPasswordForBiometricDialog(prefs, switchBiometria, currentUser.getEmail());
            } else {
                switchBiometria.setChecked(false);
            }
        } else {
            authenticateWithBiometric(prefs, savedEmail, savedPassword, switchBiometria);
        }
    }

    private void showPasswordForBiometricDialog(android.content.SharedPreferences prefs, androidx.appcompat.widget.SwitchCompat switchBiometria, String email) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        TextView tv = new TextView(this);
        tv.setText("Ingresa tu contraseña actual para activar el inicio con biometría:");
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(14f);
        layout.addView(tv);

        final EditText etPass = new EditText(this);
        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPass.setHint("Contraseña");
        layout.addView(etPass);

        Button btnConfirm = new Button(this);
        btnConfirm.setText("Continuar");
        btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(android.R.color.holo_blue_dark)));
        btnConfirm.setTextColor(Color.WHITE);
        layout.addView(btnConfirm);

        dialog.setContentView(layout);
        dialog.show();

        if (dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        dialog.setOnCancelListener(dialogInterface -> switchBiometria.setChecked(false));

        btnConfirm.setOnClickListener(v -> {
            String pass = etPass.getText().toString().trim();
            FirebaseUser user = mAuth.getCurrentUser();
            if (pass.isEmpty() || user == null || user.getEmail() == null) {
                Toast.makeText(this, "Ingresa una contraseña válida", Toast.LENGTH_SHORT).show();
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), pass);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    dialog.dismiss();
                    android.content.SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("email", user.getEmail());
                    editor.putString("password", pass);
                    editor.putBoolean("remember", true);
                    editor.apply();

                    authenticateWithBiometric(prefs, user.getEmail(), pass, switchBiometria);
                } else {
                    Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void authenticateWithBiometric(android.content.SharedPreferences prefs, String email, String password, androidx.appcompat.widget.SwitchCompat switchBiometria) {
        java.util.concurrent.Executor executor = androidx.core.content.ContextCompat.getMainExecutor(this);
        androidx.biometric.BiometricPrompt biometricPrompt = new androidx.biometric.BiometricPrompt(ConfiguracionActivity.this, executor, new androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull androidx.biometric.BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("biometric_enabled", true);
                editor.putString("biometric_email", email);
                editor.putString("biometric_password", password);
                editor.putBoolean("remember", true);
                editor.apply();

                Toast.makeText(ConfiguracionActivity.this, "¡Inicio con biometría activado!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                switchBiometria.setChecked(false);
                Toast.makeText(ConfiguracionActivity.this, "Cancelado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(ConfiguracionActivity.this, "Huella no reconocida", Toast.LENGTH_SHORT).show();
            }
        });

        androidx.biometric.BiometricPrompt.PromptInfo promptInfo = new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verificar Identidad")
                .setSubtitle("Confirma tu huella para activar el acceso biométrico")
                .setNegativeButtonText("Cancelar")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
