package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private FirebaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alacena);

        repository = new FirebaseRepository(this);
        productos = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view_alacena);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductoAdapter(this, productos);
        recyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton btnAgregar = findViewById(R.id.btn_Agregar);
        btnAgregar.setOnClickListener(v -> {
            Intent intent = new Intent(AlacenaActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

        cargarProductos();
    }

    private void cargarProductos() {
        repository.leerProductos(new FirebaseRepository.OnProductosListener() {
            @Override
            public void onProductosObtenidos(List<Producto> lista) {
                productos.clear();
                productos.addAll(lista);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AlacenaActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarProductos();
    }
}
