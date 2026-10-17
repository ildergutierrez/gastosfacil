package com.example.gastofacil;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class LocationPickerActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 100;
    private WebView webViewMap;
    private double currentLat = 4.6097; // Default Bogotá
    private double currentLng = -74.0817;
    private double selectedLat = 4.6097;
    private double selectedLng = -74.0817;
    private boolean routeMode = false;
    private double destLat = 0.0;
    private double destLng = 0.0;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        routeMode = getIntent().getBooleanExtra("route_mode", false);
        destLat = getIntent().getDoubleExtra("dest_lat", 0.0);
        destLng = getIntent().getDoubleExtra("dest_lng", 0.0);

        webViewMap = findViewById(R.id.id_mapa);
        Button btnConfirm = findViewById(R.id.btnConfirmLocation);

        if (routeMode) {
            btnConfirm.setText("Cerrar Ruta");
            btnConfirm.setOnClickListener(v -> finish());
        } else {
            btnConfirm.setOnClickListener(v -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("lat", selectedLat);
                resultIntent.putExtra("lng", selectedLng);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }

        WebSettings webSettings = webViewMap.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webViewMap.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");
        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                loadMapLeaflet();
            }
        });

        webViewMap.loadDataWithBaseURL("https://example.com", getLeafletHtml(), "text/html", "UTF-8", null);

        obtainUserLocation();
    }

    private void obtainUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                    if (!routeMode) {
                        selectedLat = currentLat;
                        selectedLng = currentLng;
                    }
                    loadMapLeaflet();
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
        }
    }

    private void loadMapLeaflet() {
        double lat = routeMode ? currentLat : selectedLat;
        double lng = routeMode ? currentLng : selectedLng;
        
        String jsCall;
        if (routeMode) {
            jsCall = String.format(java.util.Locale.ENGLISH, "updateRouteMap(%f, %f, %f, %f);", currentLat, currentLng, destLat, destLng);
        } else {
            jsCall = String.format(java.util.Locale.ENGLISH, "updateMapCenter(%f, %f);", lat, lng);
        }
        webViewMap.evaluateJavascript("javascript:" + jsCall, null);
    }

    private String getLeafletHtml() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\" />\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n" +
                "    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
                "    <style>\n" +
                "        html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; }\n" +
                "        .center-marker {\n" +
                "            position: absolute;\n" +
                "            top: 50%; left: 50%;\n" +
                "            transform: translate(-50%, -100%);\n" +
                "            width: 40px; height: 40px;\n" +
                "            background-image: url('https://cdn-icons-png.flaticon.com/512/684/684908.png');\n" +
                "            background-size: cover;\n" +
                "            z-index: 1000;\n" +
                "            pointer-events: none;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"map\"></div>\n" +
                "    <div class=\"center-marker\" id=\"markerCenter\"></div>\n" +
                "    <script>\n" +
                "        var map = L.map('map').setView([4.6097, -74.0817], 15);\n" +
                "        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "            maxZoom: 19,\n" +
                "            attribution: '&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors'\n" +
                "        }).addTo(map);\n" +
                "\n" +
                "        var routeLine = null;\n" +
                "        var destMarker = null;\n" +
                "\n" +
                "        map.on('move', function() {\n" +
                "            var center = map.getCenter();\n" +
                "            if (window.AndroidBridge && !window.isRouteMode) {\n" +
                "                AndroidBridge.onLocationChanged(center.lat, center.lng);\n" +
                "            }\n" +
                "        });\n" +
                "\n" +
                "        function updateMapCenter(lat, lng) {\n" +
                "            map.setView([lat, lng], 15);\n" +
                "        }\n" +
                "\n" +
                "        function updateRouteMap(origLat, origLng, dLat, dLng) {\n" +
                "            window.isRouteMode = true;\n" +
                "            document.getElementById('markerCenter').style.display = 'none';\n" +
                "            map.setView([origLat, origLng], 14);\n" +
                "            L.marker([origLat, origLng]).addTo(map).bindPopup('Tu ubicación').openPopup();\n" +
                "            destMarker = L.marker([dLat, dLng]).addTo(map).bindPopup('Destino');\n" +
                "            \n" +
                "            var latlngs = [\n" +
                "                [origLat, origLng],\n" +
                "                [dLat, dLng]\n" +
                "            ];\n" +
                "            routeLine = L.polyline(latlngs, {color: 'blue', weight: 5}).addTo(map);\n" +
                "            map.fitBounds(routeLine.getBounds(), {padding: [50, 50]});\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void onLocationChanged(double lat, double lng) {
            selectedLat = lat;
            selectedLng = lng;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtainUserLocation();
        }
    }
}
