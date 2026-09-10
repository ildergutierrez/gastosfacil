package com.example.gastofacil;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegistroActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("usuarios");

        setContentView(R.layout.activity_registro);
        
        //obtener los datos de los inputs
        EditText fullName= findViewById(R.id.atfullName);
        EditText correo = findViewById(R.id.atcorreo);
        EditText tel = findViewById(R.id.atcell);
        EditText contra = findViewById(R.id.atpassword);
        EditText confirmar = findViewById(R.id.atconfirmar);
        androidx.appcompat.widget.AppCompatButton btnRegistrarse = findViewById(R.id.btnRegistrarse);

        // Feedback views
        TextView tvReqLength = findViewById(R.id.tvReqLength);
        TextView tvReqUpper = findViewById(R.id.tvReqUpper);
        TextView tvReqLower = findViewById(R.id.tvReqLower);
        TextView tvReqNumber = findViewById(R.id.tvReqNumber);
        TextView tvReqSpecial = findViewById(R.id.tvReqSpecial);
        TextView tvReqMatch = findViewById(R.id.tvReqMatch);

        // Deshabilitar el botón inicialmente
        btnRegistrarse.setEnabled(false);
        btnRegistrarse.setAlpha(0.5f); // Opcional: para que se vea visualmente deshabilitado

        // Lógica de validación
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String name = fullName.getText().toString().trim();
                String email = correo.getText().toString().trim();
                String phone = tel.getText().toString().trim();
                String password = contra.getText().toString();
                String confirmPass = confirmar.getText().toString();

                // Validaciones individuales
                boolean hasLength = password.length() >= 8;
                boolean hasUpper = password.matches(".*[A-Z].*");
                boolean hasLower = password.matches(".*[a-z].*");
                boolean hasNumber = password.matches(".*\\d.*");
                boolean hasSpecial = password.matches(".*[@$!%*?&].*");
                boolean matches = !password.isEmpty() && password.equals(confirmPass);

                // Actualizar colores
                updateReqColor(tvReqLength, hasLength);
                updateReqColor(tvReqUpper, hasUpper);
                updateReqColor(tvReqLower, hasLower);
                updateReqColor(tvReqNumber, hasNumber);
                updateReqColor(tvReqSpecial, hasSpecial);
                updateReqColor(tvReqMatch, matches);

                boolean allFieldsFilled = !name.isEmpty() && !email.isEmpty() && !phone.isEmpty() && !password.isEmpty() && !confirmPass.isEmpty();
                
                if (allFieldsFilled && hasLength && hasUpper && hasLower && hasNumber && hasSpecial && matches) {
                    btnRegistrarse.setEnabled(true);
                    btnRegistrarse.setAlpha(1.0f);
                } else {
                    btnRegistrarse.setEnabled(false);
                    btnRegistrarse.setAlpha(0.5f);
                }
            }
        };

        // Agregar el watcher a todos los campos
        fullName.addTextChangedListener(validationWatcher);
        correo.addTextChangedListener(validationWatcher);
        tel.addTextChangedListener(validationWatcher);
        contra.addTextChangedListener(validationWatcher);
        confirmar.addTextChangedListener(validationWatcher);

        ProgressBar pbRegistro = findViewById(R.id.pbRegistro);
        btnRegistrarse.setOnClickListener(view -> {
            String name = fullName.getText().toString().trim();
            String email = correo.getText().toString().trim();
            String phone = tel.getText().toString().trim();
            String password = contra.getText().toString();

            btnRegistrarse.setEnabled(false);
            btnRegistrarse.setText(""); // Ocultar texto
            pbRegistro.setVisibility(View.VISIBLE);

            // 1. Crear el usuario en Firebase Authentication usando Email y Password
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // 2. Si se crea con éxito, obtenemos el UID
                            String userId = mAuth.getCurrentUser().getUid();
                            
                            // 3. Creamos el objeto de usuario SIN la contraseña para la base de datos
                            gasto_user usuario = new gasto_user(name, email, phone, 0.0);
                            usuario.setTotalIngresos(0.0);
                            usuario.setTotalEgresos(0.0);
                            
                            // 4. Guardamos en Realtime Database bajo el nodo del UID
                            myRef.child(userId).setValue(usuario).addOnCompleteListener(dbTask -> {
                                pbRegistro.setVisibility(View.GONE);
                                btnRegistrarse.setEnabled(true);
                                btnRegistrarse.setText(R.string.r_boton_registro);

                                if (dbTask.isSuccessful()) {
                                    android.widget.Toast.makeText(RegistroActivity.this, getString(R.string.r_success), android.widget.Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
                        } else {
                            pbRegistro.setVisibility(View.GONE);
                            btnRegistrarse.setEnabled(true);
                            btnRegistrarse.setText(R.string.r_boton_registro);
                            
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "";
                            android.widget.Toast.makeText(RegistroActivity.this, getString(R.string.r_error_generic) + ": " + errorMsg, android.widget.Toast.LENGTH_LONG).show();
                        }
                    });
        });




        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);


        // Volver al inicio de sesión desde la flecha
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Volver al inicio de sesión desde el texto inferior
        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void updateReqColor(TextView tv, boolean met) {
        if (met) {
            tv.setTextColor(android.graphics.Color.parseColor("#00BFA5")); // nav_active green
        } else {
            tv.setTextColor(android.graphics.Color.parseColor("#E91E63")); // expense_red
        }
    }
}