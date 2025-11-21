package com.example.myapplication.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode; // Necesario para UDP simple en hilos rápidos
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String AP_ESP_SSID = "AlacenaSetup";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int UDP_PORT = 4210; // Mismo puerto que en el ESP8266

    // --- Componentes del Menú Lateral ---
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView btnMenuLateral;

    // --- Componentes UI Generales ---
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

        // Permitir operaciones de red en hilo principal (solo para UDP rápido)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // -----------------------------------------------------------
        // 1. CONFIGURACIÓN DEL MENÚ LATERAL
        // -----------------------------------------------------------
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnMenuLateral = findViewById(R.id.btn_menu_lateral);

        // Abrir menú al tocar el botón hamburguesa
        btnMenuLateral.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Manejar clics en las opciones del menú de ayuda
        navigationView.setNavigationItemSelectedListener(this::manejarMenuLateral);

        // Manejo del botón ATRÁS para cerrar el menú primero
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // -----------------------------------------------------------
        // 2. CONFIGURACIÓN FIREBASE
        // -----------------------------------------------------------
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

        // -----------------------------------------------------------
        // 3. BOTONES Y NAVEGACIÓN
        // -----------------------------------------------------------
        MaterialButton btnAlacena = findViewById(R.id.btn_ver_alacena);
        MaterialButton btnCompras = findViewById(R.id.btn_lista_compras);
        btnWifi = findViewById(R.id.btn_wifi_setup);

        btnAlacena.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AlacenaActivity.class))
        );

        btnCompras.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ComprasActivity.class))
        );

        // Lógica del botón WiFi
        btnWifi.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                startWifiFlow();
            } else {
                requestLocationPermission();
            }
        });

        // -----------------------------------------------------------
        // 4. LISTAS Y CONTADORES
        // -----------------------------------------------------------
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

        // Contadores
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
    // LÓGICA DEL MENÚ LATERAL (AYUDA)
    // =================================================================
    private boolean manejarMenuLateral(@NonNull MenuItem item) {
        int id = item.getItemId();

        Intent intent = null;

        if (id == R.id.nav_wifi) {
            intent = new Intent(this, GuideActivity.class);
            intent.putExtra("GUIDE_TYPE", "WIFI");
        } else if (id == R.id.nav_usar) {
            intent = new Intent(this, GuideActivity.class);
            intent.putExtra("GUIDE_TYPE", "USAR");
        } else if (id == R.id.nav_agregar) {
            intent = new Intent(this, GuideActivity.class);
            intent.putExtra("GUIDE_TYPE", "AGREGAR");
        } else if (id == R.id.nav_eliminar) {
            intent = new Intent(this, GuideActivity.class);
            intent.putExtra("GUIDE_TYPE", "ELIMINAR");
        } else if (id == R.id.nav_editar) {
            intent = new Intent(this, GuideActivity.class);
            intent.putExtra("GUIDE_TYPE", "EDITAR");
        } else if (id == R.id.nav_faq) {
            intent = new Intent(this, FaqActivity.class);
        } else if (id == R.id.nav_acerca) {
            intent = new Intent(this, AboutActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void mostrarAyuda(String titulo, String mensaje) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setMessage(mensaje)
                .setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // CICLO DE VIDA: Limpiar selección al volver
    @Override
    protected void onResume() {
        super.onResume();
        if (navigationView != null) {
            int size = navigationView.getMenu().size();
            for (int i = 0; i < size; i++) {
                navigationView.getMenu().getItem(i).setChecked(false);
            }
        }
    }

    // =================================================================
    // LÓGICA DE FLUJO Y RED (CON AUTO-DESCUBRIMIENTO)
    // =================================================================

    private void startWifiFlow() {
        currentDeviceIp = DevicePrefs.obtenerIP(this);

        if (isPhoneConnectedToAP()) {
            Toast.makeText(this, "Conectado a red de Configuración.", Toast.LENGTH_SHORT).show();
            navigateToSetup();
            return;
        }

        // Paso 1: Intentar con la IP guardada
        if (currentDeviceIp != null && !currentDeviceIp.isEmpty()) {
            checkDeviceStatusAsync(currentDeviceIp, false); // false = no es intento final
        } else {
            // Paso 2: Si no hay IP, intentar descubrir
            discoverDeviceUDP();
        }
    }

    // --- PING HTTP ---
    private void checkDeviceStatusAsync(String ip, boolean isDiscoveryAttempt) {
        if (!isDiscoveryAttempt) Toast.makeText(this, "Buscando...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            boolean online = false;
            try {
                URL url = new URL("http://" + ip + "/status");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                if (conn.getResponseCode() == 200) online = true;
                conn.disconnect();
            } catch (Exception e) { online = false; }

            final boolean finalOnline = online;
            runOnUiThread(() -> {
                if (finalOnline) {
                    // ¡Encontrado! Guardamos la IP y entramos
                    DevicePrefs.guardarIP(MainActivity.this, ip);
                    currentDeviceIp = ip;
                    navigateToStatus();
                } else {
                    if (!isDiscoveryAttempt) {
                        // Si falló la IP guardada, intentamos Auto-Descubrimiento
                        discoverDeviceUDP();
                    } else {
                        // Si falló todo, pedimos reconfigurar
                        Toast.makeText(MainActivity.this, "No se encontró la Alacena.", Toast.LENGTH_LONG).show();
                        navigateToSetup();
                    }
                }
            });
        }).start();
    }

    // --- AUTO-DESCUBRIMIENTO UDP ---
    private void discoverDeviceUDP() {
        Toast.makeText(this, "Escaneando red...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            String foundIp = null;
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setBroadcast(true);
                socket.setSoTimeout(2000); // Esperar 2 segundos por respuesta

                // Enviar mensaje "ALACENA_DISCOVER"
                byte[] sendData = "ALACENA_DISCOVER".getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), UDP_PORT);
                socket.send(sendPacket);

                // Escuchar respuesta
                byte[] recvBuf = new byte[255];
                DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(receivePacket);

                // Analizar respuesta: "ALACENA_HERE|192.168.1.X"
                String message = new String(receivePacket.getData()).trim();
                if (message.startsWith("ALACENA_HERE")) {
                    String[] parts = message.split("\\|");
                    if (parts.length > 1) {
                        foundIp = parts[1];
                    }
                }
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error UDP: " + e.getMessage());
            }

            final String finalIp = foundIp;
            runOnUiThread(() -> {
                if (finalIp != null) {
                    Toast.makeText(MainActivity.this, "¡Alacena encontrada en " + finalIp + "!", Toast.LENGTH_SHORT).show();
                    checkDeviceStatusAsync(finalIp, true); // Verificar HTTP final
                } else {
                    Toast.makeText(MainActivity.this, "No se encontró automáticamente.", Toast.LENGTH_SHORT).show();
                    navigateToSetup();
                }
            });
        }).start();
    }

    // ... Métodos de navegación y permisos ...

    private void navigateToSetup() {
        startActivity(new Intent(MainActivity.this, WifiSetupActivity.class));
    }

    private void navigateToStatus() {
        Intent i = new Intent(MainActivity.this, DeviceStatusActivity.class);
        i.putExtra("DEVICE_IP", currentDeviceIp);
        startActivity(i);
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isPhoneConnectedToAP() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            return false;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String currentSsid = wifiInfo.getSSID();
        if (currentSsid != null) {
            String cleanSsid = currentSsid.replaceAll("^\"|\"$", "");
            return cleanSsid.equals(AP_ESP_SSID);
        }
        return false;
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
                startWifiFlow();
            } else {
                Toast.makeText(this, "Permiso de Ubicación requerido para verificar el WiFi.", Toast.LENGTH_LONG).show();
                navigateToSetup();
            }
        }
    }
}