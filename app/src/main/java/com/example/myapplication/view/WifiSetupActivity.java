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

import org.json.JSONObject; // IMPORTANTE: Necesario para leer el JSON del ESP

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
                // Esperar 3.5 segundos para asegurar conexión en modelos viejos
                new Handler().postDelayed(() -> {
                    enviarCredencialesALaAlacena(ssidReal, passReal);
                }, 3500);
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
    // ESTA ES LA FUNCIÓN PRINCIPAL CORREGIDA
    // --------------------------------------------------------------------
    private void enviarCredencialesALaAlacena(String ssid, String password) {

        final String finalSsid = ssid;
        final String finalPassword = password;

        new Thread(() -> {
            HttpURLConnection conn = null;
            String ipObtenida = null;
            int responseCode = -1;

            try {
                URL url = new URL("http://192.168.4.1/setwifi");
                conn = (HttpURLConnection) url.openConnection();

                // CORRECCIÓN 1: TIEMPOS DE ESPERA AUMENTADOS
                conn.setConnectTimeout(10000); // 10 seg para conectar al ESP
                conn.setReadTimeout(25000);    // 25 seg para esperar la respuesta (el ESP tarda ~15s probando wifi)

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String body = "ssid=" + finalSsid + "&password=" + finalPassword;
                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes());
                os.close();

                responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    // Leer respuesta
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }

                        // CORRECCIÓN 2: PARSEAR JSON EN LUGAR DE TEXTO PLANO
                        // El ESP envía: {"status":"ok", "ip":"192.168.X.X"}
                        String jsonRespuesta = sb.toString();
                        JSONObject json = new JSONObject(jsonRespuesta);

                        if (json.has("ip")) {
                            ipObtenida = json.getString("ip");
                        }
                    }
                }

                final int finalResponseCode = responseCode;
                final String finalIpObtenida = ipObtenida;

                runOnUiThread(() -> {
                    if (finalResponseCode == 200 && finalIpObtenida != null) {
                        // ÉXITO REAL
                        DevicePrefs.guardarIP(WifiSetupActivity.this, finalIpObtenida);
                        txtEstado.setText("✔ Éxito. Nueva IP: " + finalIpObtenida);
                        Toast.makeText(WifiSetupActivity.this, "WiFi Configurado Correctamente", Toast.LENGTH_LONG).show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && wifiCallback != null) {
                            connectivityManager.unregisterNetworkCallback(wifiCallback);
                        }
                        finish(); // Volver a MainActivity

                    } else {
                        // ERROR: El ESP respondió 400 (clave mal) o no devolvió IP
                        txtEstado.setText("❌ Clave incorrecta o fallo de conexión.");
                        Toast.makeText(WifiSetupActivity.this, "Verifica la contraseña", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (java.net.SocketTimeoutException e) {
                // Error específico de tiempo de espera
                runOnUiThread(() -> txtEstado.setText("❌ Tiempo de espera agotado. El ESP tardó demasiado."));
            } catch (Exception e) {
                // Otros errores
                final String errorMsg = e.getMessage();
                runOnUiThread(() -> txtEstado.setText("❌ Error: " + errorMsg));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.bindProcessToNetwork(null);
        }
        // Nota: No desregistramos el callback aquí si ya se hizo en éxito,
        // pero por seguridad se puede dejar un try-catch si se desea.
    }
}