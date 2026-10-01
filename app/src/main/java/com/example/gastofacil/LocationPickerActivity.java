package com.example.gastofacil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        Button btnConfirm = findViewById(R.id.btnConfirmLocation);
        btnConfirm.setOnClickListener(v -> {
            if (mMap != null) {
                selectedLatLng = mMap.getCameraPosition().target;
                Intent resultIntent = new Intent();
                resultIntent.putExtra("lat", selectedLatLng.latitude);
                resultIntent.putExtra("lng", selectedLatLng.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        // Coordenadas por defecto (ej. Bogotá)
        LatLng defaultLocation = new LatLng(4.6097, -74.0817);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));
        
        // Si hay una ubicación previa, mover cámara allí
        double lat = getIntent().getDoubleExtra("lat", 0);
        double lng = getIntent().getDoubleExtra("lng", 0);
        if (lat != 0 && lng != 0) {
            LatLng previous = new LatLng(lat, lng);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(previous, 15f));
        }
    }
}
