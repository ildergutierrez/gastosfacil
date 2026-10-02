package com.example.gastofacil;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class inicioSesion extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();

        setContentView(R.layout.activity_inicio_sesion);

        android.content.SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        boolean biometricEnabled = loginPrefs.getBoolean("biometric_enabled", false);
        if (biometricEnabled && savedInstanceState == null) {
            triggerBiometricLogin(loginPrefs);
        }

        EditText etUser = findViewById(R.id.etUser);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnIngresar = findViewById(R.id.btnIngresar);
        TextView tvRegister = findViewById(R.id.tvRegister);
        ImageView ivLogo = findViewById(R.id.ivLogo);
        ImageButton btnShowPassword = findViewById(R.id.btnShowPassword);
        CheckBox cbRememberMe = findViewById(R.id.cbRememberMe);

        // Recordarme: Cargar datos guardados
        android.content.SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        boolean remember = prefs.getBoolean("remember", false);
        if (remember) {
            etUser.setText(prefs.getString("email", ""));
            etPassword.setText(prefs.getString("password", ""));
            cbRememberMe.setChecked(true);
            
            // Si ya hay datos, habilitar el botón
            if (!etUser.getText().toString().isEmpty() && !etPassword.getText().toString().isEmpty()) {
                btnIngresar.setEnabled(true);
                btnIngresar.setAlpha(1.0f);
            }
        }

        // Mostrar/Ocultar contraseña
        btnShowPassword.setOnClickListener(v -> {
            if (etPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnShowPassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnShowPassword.setImageResource(R.drawable.ic_eye);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // 1) El botón debe estar deshabilitado inicialmente si no hay datos
        if (etUser.getText().toString().isEmpty() || etPassword.getText().toString().isEmpty()) {
            btnIngresar.setEnabled(false);
            btnIngresar.setAlpha(0.5f);
        }

        // Lógica para habilitar el botón solo cuando ambos campos tengan información
        TextWatcher loginWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String email = etUser.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();

                if (!email.isEmpty() && !pass.isEmpty()) {
                    btnIngresar.setEnabled(true);
                    btnIngresar.setAlpha(1.0f);
                } else {
                    btnIngresar.setEnabled(false);
                    btnIngresar.setAlpha(0.5f);
                }
            }
        };

        etUser.addTextChangedListener(loginWatcher);
        etPassword.addTextChangedListener(loginWatcher);


        // 2) Acción de autenticación
        ProgressBar pbLogin = findViewById(R.id.pbLogin);
        btnIngresar.setOnClickListener(v -> {
            String email = etUser.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            btnIngresar.setEnabled(false);
            btnIngresar.setText(""); // Ocultar texto
            pbLogin.setVisibility(View.VISIBLE);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // Login exitoso, obtener idioma del usuario
                            String userId = mAuth.getCurrentUser().getUid();
                            FirebaseDatabase.getInstance().getReference("usuarios").child(userId).child("idioma").get()
                                    .addOnCompleteListener(dbTask -> {
                                        pbLogin.setVisibility(View.GONE);
                                        btnIngresar.setEnabled(true);
                                        btnIngresar.setText(R.string.login_button);

                                        String lang = "es";
                                        if (dbTask.isSuccessful() && dbTask.getResult().getValue() != null) {
                                            lang = dbTask.getResult().getValue().toString();
                                        }
                                        LocaleHelper.applyLocale(inicioSesion.this, lang);
                                        
                                        // Guardar preferencias de "Recordarme"
                                        android.content.SharedPreferences.Editor editor = prefs.edit();
                                        if (cbRememberMe.isChecked()) {
                                            editor.putString("email", email);
                                            editor.putString("password", password);
                                            editor.putBoolean("remember", true);
                                        } else {
                                            editor.clear();
                                        }
                                        editor.apply();

                                        Toast.makeText(inicioSesion.this, getString(R.string.login_welcome), Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(inicioSesion.this, DashboardActivity.class);
                                        startActivity(intent);
                                        finish();
                                    });
                        } else {
                            pbLogin.setVisibility(View.GONE);
                            btnIngresar.setEnabled(true);
                            btnIngresar.setText(R.string.login_button);
                            
                            // Error en el login
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "";
                            Toast.makeText(inicioSesion.this, getString(R.string.login_error_generic) + ": " + errorMsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Ir al Registro
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(inicioSesion.this, RegistroActivity.class);
            startActivity(intent);
        });
    }

    private void triggerBiometricLogin(android.content.SharedPreferences prefs) {
        String email = prefs.getString("biometric_email", prefs.getString("email", ""));
        String password = prefs.getString("biometric_password", prefs.getString("password", ""));

        if (email.isEmpty() || password.isEmpty()) {
            return;
        }

        java.util.concurrent.Executor executor = androidx.core.content.ContextCompat.getMainExecutor(this);
        androidx.biometric.BiometricPrompt biometricPrompt = new androidx.biometric.BiometricPrompt(inicioSesion.this, executor, new androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull androidx.biometric.BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                ProgressBar pbLogin = findViewById(R.id.pbLogin);
                Button btnIngresar = findViewById(R.id.btnIngresar);
                btnIngresar.setEnabled(false);
                pbLogin.setVisibility(View.VISIBLE);

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(inicioSesion.this, task -> {
                            pbLogin.setVisibility(View.GONE);
                            btnIngresar.setEnabled(true);
                            if (task.isSuccessful()) {
                                String userId = mAuth.getCurrentUser().getUid();
                                FirebaseDatabase.getInstance().getReference("usuarios").child(userId).child("idioma").get()
                                        .addOnCompleteListener(dbTask -> {
                                            String lang = "es";
                                            if (dbTask.isSuccessful() && dbTask.getResult().getValue() != null) {
                                                lang = dbTask.getResult().getValue().toString();
                                            }
                                            LocaleHelper.applyLocale(inicioSesion.this, lang);

                                            Toast.makeText(inicioSesion.this, getString(R.string.login_welcome), Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(inicioSesion.this, DashboardActivity.class);
                                            startActivity(intent);
                                            finish();
                                        });
                            } else {
                                Toast.makeText(inicioSesion.this, "Error al iniciar sesión con biometría", Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(inicioSesion.this, "Huella no reconocida", Toast.LENGTH_SHORT).show();
            }
        });

        androidx.biometric.BiometricPrompt.PromptInfo promptInfo = new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Iniciar sesión con Biometría")
                .setSubtitle("Confirma tu huella para acceder a tu cuenta")
                .setNegativeButtonText("Usar contraseña")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
