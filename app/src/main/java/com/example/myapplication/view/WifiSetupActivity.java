package com.example.myapplication.view;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WifiSetupActivity extends AppCompatActivity {

    private EditText edtSSID, edtPassword;
    private TextView txtEstado;
    private Button btnGuardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_setup);

        edtSSID = findViewById(R.id.edtSSID);
        edtPassword = findViewById(R.id.edtPassword);
        txtEstado = findViewById(R.id.txtEstado);
        btnGuardar = findViewById(R.id.btnGuardarWifi);

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
                enviarCredencialesALaAlacena(ssidReal, passReal);
            }
        });
    }


    // ==============================================
    // ANDROID 10+ (Q o superior)
    // ==============================================
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void conectarAndroid10(Runnable onConnected) {

        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid("AlacenaSetup")
                .setWpa2Passphrase("12345678")
                .build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(Network network) {
                cm.bindProcessToNetwork(network);

                runOnUiThread(() -> {
                    txtEstado.setText("Conectado al AP del ESP8266. Enviando datos...");
                    onConnected.run(); // ENVÍA SSID Y PASSWORD AL ESP8266
                });
            }

            @Override
            public void onUnavailable() {
                runOnUiThread(() -> {
                    txtEstado.setText("Error: No se pudo conectar al AP del ESP");
                    Toast.makeText(WifiSetupActivity.this, "Error al conectar", Toast.LENGTH_SHORT).show();
                });
            }
        };

        cm.requestNetwork(request, callback);
    }


    // ==============================================
    // ANDROID 9 O MENOS
    // ==============================================
    private void conectarLegacy() {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"AlacenaSetup\"";
        config.preSharedKey = "\"12345678\"";

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        int id = wm.addNetwork(config);
        wm.disconnect();
        wm.enableNetwork(id, true);
        wm.reconnect();

        txtEstado.setText("Conectado al AP (modo legacy). Enviando datos...");
    }


    // ==============================================
    // ENVIAR SSID Y PASSWORD AL ESP8266
    // ==============================================
    private void enviarCredencialesALaAlacena(String ssid, String password) {

        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.4.1/setwifi");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String body = "ssid=" + ssid + "&password=" + password;

                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes());
                os.close();

                int response = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (response == 200) {
                        txtEstado.setText("✔ Datos enviados. El ESP se reiniciará...");
                        Toast.makeText(this, "WiFi enviada correctamente", Toast.LENGTH_LONG).show();
                    } else {
                        txtEstado.setText("❌ Error al enviar datos");
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> txtEstado.setText("❌ Error: " + e.getMessage()));
            }
        }).start();
    }
}
