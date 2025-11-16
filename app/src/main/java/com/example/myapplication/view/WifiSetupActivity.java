package com.example.myapplication.view;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.utils.DevicePrefs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WifiSetupActivity extends AppCompatActivity {

    private EditText edtSSID, edtPassword;
    private TextView txtEstado;
    private Button btnGuardar;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback wifiCallback;

    private static final String ESP_SSID = "AlacenaSetup";
    private static final String ESP_PASS = "12345678";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_setup);

        pedirPermisos();

        edtSSID = findViewById(R.id.edtSSID);
        edtPassword = findViewById(R.id.edtPassword);
        txtEstado = findViewById(R.id.txtEstado);
        btnGuardar = findViewById(R.id.btnGuardarWifi);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        btnGuardar.setOnClickListener(v -> {

            String ssidReal = edtSSID.getText().toString().trim();
            String passReal = edtPassword.getText().toString().trim();

            if (ssidReal.isEmpty() || passReal.isEmpty()) {
                Toast.makeText(this, "Ingresa SSID y Contraseña", Toast.LENGTH_SHORT).show();
                return;
            }

            txtEstado.setText("Conectando al AP del ESP8266...");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                conectarAndroid10(() -> enviarCredencialesALaAlacena(ssidReal, passReal));
            } else {
                conectarLegacy();

                // Esperar 2.5 segundos para que Android termine de conectarse
                new Handler().postDelayed(() -> {
                    enviarCredencialesALaAlacena(ssidReal, passReal);
                }, 2500);

            }
        });
    }

    // --------------------------------------------------------------------
    private void pedirPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE
            }, 101);
        }
    }

    // --------------------------------------------------------------------
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void conectarAndroid10(Runnable onConnected) {

        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(ESP_SSID)
                .setWpa2Passphrase(ESP_PASS)
                .build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();

        wifiCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {

                connectivityManager.bindProcessToNetwork(network);

                runOnUiThread(() -> {
                    txtEstado.setText("Conectado al AP del ESP. Enviando datos...");
                    onConnected.run();
                });
            }

            @Override
            public void onUnavailable() {
                runOnUiThread(() ->
                        txtEstado.setText("❌ No se pudo conectar al AP del ESP.")
                );
            }
        };

        connectivityManager.requestNetwork(request, wifiCallback);
    }

    // --------------------------------------------------------------------
    private void conectarLegacy() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ESP_SSID + "\"";
        config.preSharedKey = "\"" + ESP_PASS + "\"";

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int id = wm.addNetwork(config);

        wm.disconnect();
        wm.enableNetwork(id, true);
        wm.reconnect();

        txtEstado.setText("Conectado al AP (Legacy). Enviando datos...");
    }

    // --------------------------------------------------------------------
    private void enviarCredencialesALaAlacena(String ssid, String password) {

        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.4.1/setwifi");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String body = "ssid=" + ssid + "&password=" + password;
                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes());
                os.close();

                int response = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (response == 200) {
                        txtEstado.setText("✔ Credenciales enviadas. Reiniciando ESP...");
                        new Handler().postDelayed(this::obtenerInfoDespuesDeConfig, 5000);
                    } else {
                        txtEstado.setText("❌ Error al enviar datos (HTTP " + response + ")");
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        txtEstado.setText("❌ Error: " + e.getMessage())
                );
            }
        }).start();
    }

    // --------------------------------------------------------------------
    private void obtenerInfoDespuesDeConfig() {

        new Thread(() -> {

            int intentos = 0;
            boolean exito = false;

            while (intentos < 5 && !exito) {
                try {
                    URL url = new URL("http://192.168.4.1/info");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);

                    if (conn.getResponseCode() != 200) {
                        throw new Exception("ESP aún no responde");
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null)
                        sb.append(line).append("\n");

                    br.close();

                    String[] rows = sb.toString().trim().split("\n");
                    String modo = rows[0].replace("MODE:", "").trim();

                    if (modo.equals("STA") && rows.length >= 3) {
                        String ip = rows[2].replace("IP:", "").trim();

                        runOnUiThread(() -> {
                            DevicePrefs.guardarIP(WifiSetupActivity.this, ip);
                            Toast.makeText(this, "✔ Dispositivo configurado y guardado (" + ip + ")", Toast.LENGTH_LONG).show();
                            finish();
                        });

                        exito = true;

                    } else {
                        // Aún en AP o formato extraño
                        intentos++;
                        Thread.sleep(2000);
                    }

                } catch (Exception e) {
                    intentos++;
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }

            if (!exito) {
                runOnUiThread(() ->
                        txtEstado.setText("⚠ El ESP aún no está disponible o no logró conectarse al WiFi.")
                );
            }

        }).start();
    }


    // --------------------------------------------------------------------
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.bindProcessToNetwork(null);
        }

        if (wifiCallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectivityManager.unregisterNetworkCallback(wifiCallback);
        }
    }
}
