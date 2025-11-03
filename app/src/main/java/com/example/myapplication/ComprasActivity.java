package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapters.ComprasAdapter;
import com.example.myapplication.models.Producto;
import com.example.myapplication.repository.FirebaseRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComprasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ComprasAdapter adapter;
    private List<Producto> productos;
    private List<Producto> productosFiltrados;
    private FirebaseRepository repository;
    private EditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_compras);

        repository = new FirebaseRepository(this);
        productos = new ArrayList<>();
        productosFiltrados = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_shopping_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ComprasAdapter(this, productosFiltrados);
        recyclerView.setAdapter(adapter);

        searchBar = findViewById(R.id.search_shopping);

        // Detectar cambios en la barra de búsqueda
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarProductos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        cargarProductosCriticos();
    }

    private void cargarProductosCriticos() {
        repository.leerProductos(new FirebaseRepository.OnProductosListener() {
            @Override
            public void onProductosObtenidos(List<Producto> lista) {
                productos.clear();

                SharedPreferences prefs = getSharedPreferences("compras_prefs", MODE_PRIVATE);
                Set<String> marcados = new HashSet<>(prefs.getStringSet("productos_comprados", new HashSet<>()));
                Set<String> actualizados = new HashSet<>(marcados);

                for (Producto p : lista) {
                    boolean esCritico = (p.getStock() == 0 || p.getStock() < p.getStockMinimo());

                    if (esCritico) {
                        productos.add(p);
                    }


                    if (marcados.contains(p.getCodigoBarras()) && p.getStock() > 0) {
                        actualizados.remove(p.getCodigoBarras());
                    }
                }

                // Guardar los cambios en SharedPreferences
                prefs.edit().putStringSet("productos_comprados", actualizados).apply();

                if (productos.isEmpty()) {
                    Toast.makeText(ComprasActivity.this, "🎉 No hay productos faltantes o con poco stock", Toast.LENGTH_SHORT).show();
                }

                productosFiltrados.clear();
                productosFiltrados.addAll(productos);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ComprasActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void filtrarProductos(String texto) {
        productosFiltrados.clear();

        if (texto.isEmpty()) {
            productosFiltrados.addAll(productos);
        } else {
            String query = texto.toLowerCase().trim();
            for (Producto p : productos) {
                if ((p.getNombre() != null && p.getNombre().toLowerCase().contains(query)) ||
                        (p.getCategoria() != null && p.getCategoria().toLowerCase().contains(query)) ||
                        (p.getMarca() != null && p.getMarca().toLowerCase().contains(query))) {
                    productosFiltrados.add(p);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarProductosCriticos();
    }
}
