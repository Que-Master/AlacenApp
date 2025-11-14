package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapters.ProductoAdapter;
import com.example.myapplication.models.Producto;
import com.example.myapplication.repository.FirebaseRepository;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class AlacenaActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductoAdapter adapter;
    private List<Producto> productos;
    private List<Producto> productosFiltrados;
    private FirebaseRepository repository;
    private EditText editSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alacena);

        repository = new FirebaseRepository(this);
        productos = new ArrayList<>();
        productosFiltrados = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view_alacena);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductoAdapter(this, productosFiltrados);
        recyclerView.setAdapter(adapter);

        editSearch = findViewById(R.id.edit_search);
        ExtendedFloatingActionButton btnAgregar = findViewById(R.id.btn_Agregar);

        btnAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(AlacenaActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

        cargarProductos();

        // Filtrado en tiempo real
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarProductos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void cargarProductos() {
        repository.leerProductos(new FirebaseRepository.OnProductosListener() {
            @Override
            public void onProductosObtenidos(List<Producto> lista) {
                productos.clear();

                // Solo agregamos productos con stock > 0
                for (Producto p : lista) {
                    if (p.getStock() > 0) {
                        productos.add(p);
                    }
                }

                productosFiltrados.clear();
                productosFiltrados.addAll(productos);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AlacenaActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Filtro por nombre, marca o categoría
    private void filtrarProductos(String texto) {
        String query = texto.toLowerCase().trim();
        productosFiltrados.clear();

        if (query.isEmpty()) {
            // Muestra todos los productos con stock > 0
            productosFiltrados.addAll(productos);
        } else {
            for (Producto p : productos) {
                if (p.getStock() > 0 && (
                        (p.getNombre() != null && p.getNombre().toLowerCase().contains(query)) ||
                                (p.getCategoria() != null && p.getCategoria().toLowerCase().contains(query)) ||
                                (p.getMarca() != null && p.getMarca().toLowerCase().contains(query))
                )) {
                    productosFiltrados.add(p);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarProductos();
    }
}
