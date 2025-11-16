package com.example.myapplication.view;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.utils.DevicePrefs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DeviceStatusActivity extends AppCompatActivity {

    private TextView txtEstado, txtSSID, txtIP;
    private Button btnRefrescar, btnReset;

    private String deviceIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_status);

        txtEstado = findViewById(R.id.txtEstado);
        txtSSID = findViewById(R.id.txtSSID);
        txtIP = findViewById(R.id.txtIP);
        btnRefrescar = findViewById(R.id.btnRefrescar);
        btnReset = findViewById(R.id.btnResetWifi);

        // ✔ PRIORIDAD: primero usar la IP enviada desde el Intent
        String ipIntent = getIntent().getStringExtra("DEVICE_IP");
        if (ipIntent != null && !ipIntent.isEmpty()) {
            deviceIp = ipIntent;
        } else {
            deviceIp = DevicePrefs.obtenerIP(this);
        }

        if (deviceIp == null || deviceIp.isEmpty()) {
            Toast.makeText(this, "⚠ No hay dispositivo configurado", Toast.LENGTH_LONG).show();
            txtEstado.setText("No disponible");
            return;
        }

        btnRefrescar.setOnClickListener(v -> obtenerInfo());

        btnReset.setOnClickListener(v -> mostrarConfirmacionReset());

        obtenerInfo();
    }

    //   Diálogo de confirmación antes de resetear WiFi
    private void mostrarConfirmacionReset() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Restablecer WiFi")
                .setMessage("¿Seguro que quieres borrar la configuración WiFi del dispositivo?\n\n" +
                        "Después tendrás que configurarlo nuevamente con la app.")
                .setPositiveButton("Sí, resetear", (dialog, which) -> resetearWifi())
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }


    //   Obtener información del ESP
    private void obtenerInfo() {
        new Thread(() -> {
            try {
                URL url = new URL("http://" + deviceIp + "/info");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder resp = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    resp.append(line).append("\n");
                }

                reader.close();
                String data = resp.toString().trim();

                runOnUiThread(() -> mostrarInfo(data));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    txtEstado.setText("No disponible");
                    txtSSID.setText("---");
                    txtIP.setText("---");
                    Toast.makeText(this, "No se pudo conectar al dispositivo", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    //   Mostrar información
    private void mostrarInfo(String info) {

        if (info == null || info.isEmpty()) {
            txtEstado.setText("Error");
            return;
        }

        String[] lineas = info.split("\n");

        if (lineas.length < 1) {
            txtEstado.setText("Formato inválido");
            return;
        }

        String modo = lineas[0].replace("MODE:", "").trim();
        txtEstado.setText(modo);

        if (modo.equals("AP")) {
            txtSSID.setText("AlacenaSetup");
            txtIP.setText("192.168.4.1");
            return;
        }

        if (lineas.length >= 3) {
            txtSSID.setText(lineas[1].replace("SSID:", "").trim());
            txtIP.setText(lineas[2].replace("IP:", "").trim());
        } else {
            txtSSID.setText("Desconocido");
            txtIP.setText("Desconocido");
        }
    }


    //   Resetear WiFi desde la app

    private void resetearWifi() {
        new Thread(() -> {
            try {
                URL url = new URL("http://" + deviceIp + "/resetwifi");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);

                int res = conn.getResponseCode();

                runOnUiThread(() -> {
                    if (res == 200) {
                        DevicePrefs.BorrarIP(this);
                        Toast.makeText(this, "Dispositivo reiniciado. Volvió a modo AP.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error al resetear WiFi", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}
