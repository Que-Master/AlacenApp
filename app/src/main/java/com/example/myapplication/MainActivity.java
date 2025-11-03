package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapters.TransaccionesAdapter;
import com.example.myapplication.models.Producto;
import com.example.myapplication.models.Transaccion;
import com.example.myapplication.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TransaccionesAdapter adapter;
    private final List<Transaccion> lista = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);


        // CONFIGURACIÓN DE FIREBASE
        String apiKey = BuildConfig.FIREBASE_API_KEY;
        Log.d("FIREBASE_KEY", "✅ Clave detectada: " + (apiKey.isEmpty() ? "No encontrada" : "Cargada correctamente"));

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setApplicationId("1:1234567890:android:abcdef123456") // ficticio, solo para inicializar
                .setDatabaseUrl("https://alacena-inteligente-iot-default-rtdb.firebaseio.com/")
                .build();

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this, options);
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();


        // BOTONES DE NAVEGACIÓN PRINCIPAL
        MaterialButton btnAlacena = findViewById(R.id.btn_ver_alacena);
        MaterialButton btnCompras = findViewById(R.id.btn_lista_compras);

        btnAlacena.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AlacenaActivity.class);
            startActivity(intent);
        });

        btnCompras.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ComprasActivity.class);
            startActivity(intent);
        });


        // RECYCLERVIEW DE TRANSACCIONES
        recyclerView = findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TransaccionesAdapter(lista);
        recyclerView.setAdapter(adapter);

        // LECTURA DE SOLO LAS 25 ÚLTIMAS TRANSACCIONES
        DatabaseReference transRef = database.getReference("transacciones");
        transRef.orderByChild("timestamp").limitToLast(25)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        lista.clear(); // usamos la lista final declarada arriba
                        for (DataSnapshot item : snapshot.getChildren()) {
                            Transaccion t = item.getValue(Transaccion.class);
                            if (t != null) lista.add(t);
                        }

                        // Ordenar de más reciente a más antigua
                        Collections.sort(lista, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "❌ Error al leer transacciones: " + error.getMessage());
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
                    // Contar productos disponibles (stock mayor a 0)
                    if (p.getStock() > 0) {
                        disponibles++;
                    }

                    // Contar faltantes o bajo stock
                    if (p.getStock() == 0 || p.getStock() < p.getStockMinimo()) {
                        faltantes++;
                    }
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
                Log.e("Firebase", "Error al contar productos: " + error);
            }
        });
    }
}
