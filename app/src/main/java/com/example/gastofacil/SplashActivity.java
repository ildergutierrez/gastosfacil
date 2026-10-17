package com.example.gastofacil;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class SplashActivity extends AppCompatActivity {

    private boolean isNavigating = false;
    private FirebaseAuth mAuth;
    private ProgressBar pbSplashLoading;
    private TextView tvSplashLoadingText;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private final AtomicBoolean dataLoadedOrTimedOut = new AtomicBoolean(false);
    private boolean videoCompleted = false;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        pbSplashLoading = findViewById(R.id.pbSplashLoading);
        tvSplashLoadingText = findViewById(R.id.tvSplashLoadingText);

        VideoView videoView = findViewById(R.id.videoViewSplash);

        // Reproducir video
        try {
            String videoName = ThemeHelper.getSplashVideoName(this);
            InputStream is;
            try {
                is = getAssets().open(videoName);
            } catch (Exception ex) {
                videoName = "logo_animado.mp4";
                is = getAssets().open(videoName);
            }

            File videoFile = new File(getCacheDir(), videoName);
            FileOutputStream fos = new FileOutputStream(videoFile);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
            fos.close();
            is.close();

            videoView.setVideoPath(videoFile.getAbsolutePath());
            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(false);
                videoView.start();
            });
            videoView.setOnCompletionListener(mp -> {
                videoCompleted = true;
                checkAndNavigate();
            });
            videoView.setOnErrorListener((mp, what, extra) -> {
                videoCompleted = true;
                checkAndNavigate();
                return true;
            });

        } catch (Exception e) {
            videoCompleted = true;
            checkAndNavigate();
        }

        // Lógica de sesión e información
        if (mAuth.getCurrentUser() == null) {
            pbSplashLoading.setVisibility(View.GONE);
            tvSplashLoadingText.setVisibility(View.GONE);
            dataLoadedOrTimedOut.set(true);
        } else {
            pbSplashLoading.setVisibility(View.VISIBLE);
            tvSplashLoadingText.setVisibility(View.VISIBLE);

            // Timeout de seguridad de 10 segundos máximo para el splash
            timeoutRunnable = this::forceNavigateToDashboard;
            timeoutHandler.postDelayed(timeoutRunnable, 10000);

            loadUserDataAndTransactions();
        }
    }

    private void loadUserDataAndTransactions() {
        if (mAuth.getCurrentUser() == null) {
            markDataLoaded();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("usuarios").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        markDataLoaded();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        markDataLoaded();
                    }
                });
    }

    private void markDataLoaded() {
        if (dataLoadedOrTimedOut.compareAndSet(false, true)) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            checkAndNavigate();
        }
    }

    private void forceNavigateToDashboard() {
        if (dataLoadedOrTimedOut.compareAndSet(false, true)) {
            checkAndNavigate();
        }
    }

    private void checkAndNavigate() {
        if (isNavigating) return;

        // Si hay sesión, necesitamos que tanto el video HAYA TERMINADO COMO que los datos hayan cargado (o el timeout de 10s haya expirado)
        if (mAuth.getCurrentUser() != null) {
            if (videoCompleted && dataLoadedOrTimedOut.get()) {
                isNavigating = true;
                Intent intent = new Intent(SplashActivity.this, DashboardActivity.class);
                startActivity(intent);
                finish();
            }
        } else {
            // Si no hay sesión, en cuanto el video termine va al login
            if (videoCompleted || dataLoadedOrTimedOut.get()) {
                isNavigating = true;
                Intent intent = new Intent(SplashActivity.this, inicioSesion.class);
                startActivity(intent);
                finish();
            }
        }
    }
}
