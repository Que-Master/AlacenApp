package com.example.myapplication.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.R;
import com.example.myapplication.adapters.TransaccionesAdapter;
import com.example.myapplication.models.Producto;
import com.example.myapplication.models.Transaccion;
import com.example.myapplication.repository.FirebaseRepository;
import com.example.myapplication.utils.DevicePrefs;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String AP_ESP_SSID = "AlacenaSetup";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    private RecyclerView recyclerView;
    private TransaccionesAdapter adapter;
    private final List<Transaccion> lista = new ArrayList<>();
    private ImageView btnWifi;

    private String currentDeviceIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // CONFIGURACIÓN FIREBASE
        String apiKey = BuildConfig.FIREBASE_API_KEY;
        Log.d("FIREBASE_KEY", apiKey.isEmpty() ? "No encontrada" : "Cargada OK");

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setApplicationId("1:1234567890:android:abcdef123456")
                .setDatabaseUrl("https://alacena-inteligente-iot-default-rtdb.firebaseio.com/")
                .build();

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, options);
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        // BOTONES
        MaterialButton btnAlacena = findViewById(R.id.btn_ver_alacena);
        MaterialButton btnCompras = findViewById(R.id.btn_lista_compras);
        btnWifi = findViewById(R.id.btn_wifi_setup);

        btnAlacena.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AlacenaActivity.class))
        );

        btnCompras.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ComprasActivity.class))
        );

        // BOTÓN WIFI: Lógica de comprobación inteligente
        btnWifi.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                startWifiFlow();
            } else {
                requestLocationPermission();
            }
        });

        // RECYCLER TRANSACCIONES
        recyclerView = findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TransaccionesAdapter(lista);
        recyclerView.setAdapter(adapter);

        DatabaseReference transRef = database.getReference("transacciones");
        transRef.orderByChild("timestamp").limitToLast(25)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        lista.clear();
                        for (DataSnapshot item : snapshot.getChildren()) {
                            Transaccion t = item.getValue(Transaccion.class);
                            if (t != null) lista.add(t);
                        }
                        Collections.sort(lista, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Error: " + error.getMessage());
                    }
                });

        // CONTADORES DE PRODUCTOS
        TextView textTotalProductos = findViewById(R.id.text_total_productos);
        TextView textProductosAgotados = findViewById(R.id.text_productos_agotados);

        FirebaseRepository repo = new FirebaseRepository(this);

        repo.leerProductos(new FirebaseRepository.OnProductosListener() {
            @Override
            public void onProductosObtenidos(List<Producto> listaProductos) {
                int disponibles = 0;
                int faltantes = 0;

                for (Producto p : listaProductos) {
                    if (p.getStock() > 0) disponibles++;
                    if (p.getStock() == 0 || p.getStock() < p.getStockMinimo()) faltantes++;
                }

                final int totalDisponibles = disponibles;
                final int totalFaltantes = faltantes;

                runOnUiThread(() -> {
                    textTotalProductos.setText(String.valueOf(totalDisponibles));
                    textProductosAgotados.setText(String.valueOf(totalFaltantes));
                });
            }

            @Override
            public void onError(String error) {
                Log.e("Firebase", "Error al contar: " + error);
            }
        });
    }

    // =================================================================
    // LÓGICA DE FLUJO Y RED (CORREGIDA Y COMPLETADA)
    // =================================================================

    /**
     * Inicia el flujo de decisión inteligente al presionar el botón WiFi.
     */
    private void startWifiFlow() {
        currentDeviceIp = DevicePrefs.obtenerIP(this);

        // 1. Prioridad: ¿Está conectado directamente al AP de configuración?
        if (isPhoneConnectedToAP()) {
            Toast.makeText(this, "Conectado a red de Configuración.", Toast.LENGTH_SHORT).show();
            navigateToSetup();
            return;
        }

        // 2. ¿Tenemos una IP guardada de una configuración anterior?
        if (currentDeviceIp == null || currentDeviceIp.isEmpty()) {
            Log.d(TAG, "No hay IP guardada. Navegando a configuración.");
            Toast.makeText(this, "Configura tu dispositivo primero.", Toast.LENGTH_SHORT).show();
            navigateToSetup();
        } else {
            // 3. Tenemos IP, pero ¿sigue ahí el dispositivo? Hacemos Ping.
            checkDeviceStatusAsync(currentDeviceIp);
        }
    }

    /**
     * Verifica si el celular está conectado a la red "AlacenaSetup".
     */
    private boolean isPhoneConnectedToAP() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            return false;
        }

        // Necesitamos permiso de ubicación para leer el SSID en Android 8.1+
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String currentSsid = wifiInfo.getSSID();

        if (currentSsid != null) {
            // Android devuelve el SSID entre comillas (ej: "AlacenaSetup")
            String cleanSsid = currentSsid.replaceAll("^\"|\"$", "");
            return cleanSsid.equals(AP_ESP_SSID);
        }
        return false;
    }

    /**
     * Verifica asíncronamente si el ESP está vivo en la IP guardada.
     */
    private void checkDeviceStatusAsync(String ip) {
        Toast.makeText(this, "Buscando dispositivo...", Toast.LENGTH_SHORT).show();

        // Variable final para usar dentro del hilo
        final String targetIp = ip;

        new Thread(() -> {
            boolean isOnline = false;

            try {
                // Usamos HttpURLConnection estándar (sin librerías externas como OkHttp)
                // para mantener consistencia con el resto de la app.
                URL url = new URL("http://" + targetIp + "/status");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                // Timeout corto (3s): Si está en la red local, responde rápido.
                // Si no responde rápido, asumimos que no está disponible.
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();

                // El ESP devuelve 200 si está vivo (ya sea en STA o AP)
                if (responseCode == 200) {
                    isOnline = true;
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Fallo al conectar con ESP: " + e.getMessage());
                isOnline = false;
            }

            final boolean finalIsOnline = isOnline;

            // Volver al hilo principal para navegar
            runOnUiThread(() -> {
                if (finalIsOnline) {
                    // ÉXITO: El dispositivo respondió, vamos a ver su estado
                    navigateToStatus();
                } else {
                    // FALLO: No respondió, probablemente cambió de IP o se desconectó
                    Toast.makeText(MainActivity.this, "Dispositivo no responde. Reconfigurar.", Toast.LENGTH_LONG).show();
                    navigateToSetup();
                }
            });

        }).start();
    }

    // =================================================================
    // NAVEGACIÓN Y PERMISOS
    // =================================================================

    private void navigateToSetup() {
        startActivity(new Intent(MainActivity.this, WifiSetupActivity.class));
    }

    private void navigateToStatus() {
        Intent i = new Intent(MainActivity.this, DeviceStatusActivity.class);
        i.putExtra("DEVICE_IP", currentDeviceIp); // Pasamos la IP confirmada
        startActivity(i);
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Si el usuario da permiso, intentamos el flujo de nuevo
                startWifiFlow();
            } else {
                Toast.makeText(this, "Ubicación requerida para detectar WiFi.", Toast.LENGTH_LONG).show();
                // Sin permiso no podemos detectar el SSID "AlacenaSetup" fiablemente,
                // así que enviamos a setup por defecto.
                navigateToSetup();
            }
        }
    }
}