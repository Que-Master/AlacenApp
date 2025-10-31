package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.models.Producto;
import com.example.myapplication.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;

public class DetallesActivity extends AppCompatActivity {

    private ImageView imageProducto;
    private TextView textNombre, textMarca, textCategoria, textCantidad,
            textStock, textMinStock, textCodigoBarras;
    private MaterialButton btnEditar, btnEliminar;

    private FirebaseRepository repository;
    private Producto productoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalles);

        // Inicializar repositorio
        repository = new FirebaseRepository(this);

        // Referencias UI
        imageProducto = findViewById(R.id.image_product);
        textNombre = findViewById(R.id.text_nombre);
        textMarca = findViewById(R.id.text_marca);
        textCategoria = findViewById(R.id.text_categoria);
        textCantidad = findViewById(R.id.text_cantidad);
        textStock = findViewById(R.id.text_stock);
        textMinStock = findViewById(R.id.text_stock_minimo);
        textCodigoBarras = findViewById(R.id.text_codigo_barras);
        btnEditar = findViewById(R.id.btn_edit);
        btnEliminar = findViewById(R.id.btn_delete);

        // Obtener producto desde el Intent
        productoActual = (Producto) getIntent().getSerializableExtra("producto");

        if (productoActual != null) {
            mostrarDetalles(productoActual);
        }

        // Botón Editar → abre EditProductActivity
        btnEditar.setOnClickListener(v -> {
            if (productoActual != null) {
                Intent intent = new Intent(DetallesActivity.this, EditProductActivity.class);
                intent.putExtra("producto", productoActual);
                startActivity(intent);
            }
        });

        // Botón Eliminar → muestra diálogo de confirmación
        btnEliminar.setOnClickListener(v -> {
            if (productoActual != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Eliminar producto")
                        .setMessage("¿Deseas eliminar \"" + productoActual.getNombre() + "\" de la lista?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            repository.eliminarProducto(productoActual.getId());
                            Toast.makeText(this, "Producto eliminado ✅", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        });
    }

    private void mostrarDetalles(Producto producto) {
        textNombre.setText(producto.getNombre());
        textMarca.setText(producto.getMarca());
        textCategoria.setText(producto.getCategoria());
        textCantidad.setText(producto.getCantidad());
        textStock.setText(String.valueOf(producto.getStock()));
        textMinStock.setText(String.valueOf(producto.getStockMinimo()));
        textCodigoBarras.setText(producto.getCodigoBarras());

        String imagen = producto.getImagenUrl();

        if (imagen != null && !imagen.isEmpty()) {
            if (imagen.startsWith("content://") || imagen.startsWith("file://")) {
                // URI local del dispositivo
                Glide.with(this)
                        .load(Uri.parse(imagen))
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(imageProducto);
            } else {
                // URL remota (Firebase u otro)
                Glide.with(this)
                        .load(imagen)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(imageProducto);
            }
        } else {
            imageProducto.setImageResource(R.drawable.placeholder_image);
        }
    }

}

