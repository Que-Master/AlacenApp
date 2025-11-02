package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.models.Producto;
import com.example.myapplication.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DetallesActivity extends AppCompatActivity {

    private EditText textMarca, textCategoria, textCantidad,
            textStock, textStockMinimo, textCodigoBarras;
    private ImageView imageProduct;
    private MaterialButton btnEditar, btnEliminar;

    private String codigoBarras;
    private FirebaseRepository repository;
    private Producto productoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalles);

        repository = new FirebaseRepository(this);

        // Vistas
        imageProduct = findViewById(R.id.image_product);
        textMarca = findViewById(R.id.text_marca);
        textCategoria = findViewById(R.id.text_categoria);
        textCantidad = findViewById(R.id.text_cantidad);
        textStock = findViewById(R.id.text_stock);
        textStockMinimo = findViewById(R.id.text_stock_minimo);
        textCodigoBarras = findViewById(R.id.text_codigo_barras);
        btnEditar = findViewById(R.id.btn_edit);
        btnEliminar = findViewById(R.id.btn_delete);

        codigoBarras = getIntent().getStringExtra("codigo_barras");

        if (codigoBarras != null) {
            cargarDetallesProducto(codigoBarras);
        } else {
            Toast.makeText(this, "⚠️ No se recibió código de barras", Toast.LENGTH_SHORT).show();
            finish();
        }

        // --- Acciones ---
        btnEditar.setOnClickListener(v -> abrirEditProductActivity());
        btnEliminar.setOnClickListener(v -> confirmarEliminacion());
    }


    // CARGAR DETALLES DESDE FIREBASE
    private void cargarDetallesProducto(String codigo) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("productos_detalle")
                .child(codigo);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(DetallesActivity.this, "❌ Producto no encontrado", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                productoActual = snapshot.getValue(Producto.class);
                if (productoActual != null) {
                    textMarca.setText(productoActual.getMarca());
                    textCategoria.setText(productoActual.getCategoria());
                    textCantidad.setText(productoActual.getCantidad());
                    textStock.setText(String.valueOf(productoActual.getStock()));
                    textStockMinimo.setText(String.valueOf(productoActual.getStockMinimo()));
                    textCodigoBarras.setText(productoActual.getCodigoBarras());

                    if (productoActual.getImagenUrl() != null && !productoActual.getImagenUrl().isEmpty()) {
                        Glide.with(DetallesActivity.this)
                                .load(productoActual.getImagenUrl())
                                .into(imageProduct);
                    } else {
                        imageProduct.setImageResource(R.drawable.placeholder_image);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("DetallesActivity", "Error: " + error.getMessage());
            }
        });
    }


    // REDIRECCIÓN A EditProductActivity
    private void abrirEditProductActivity() {
        if (productoActual == null) {
            Toast.makeText(this, "⚠️ No hay producto cargado", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(DetallesActivity.this, EditProductActivity.class);
        intent.putExtra("producto", productoActual);
        startActivity(intent);
    }


    // CONFIRMAR Y ELIMINAR PRODUCTO
    private void confirmarEliminacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar producto")
                .setMessage("¿Seguro que deseas eliminar este producto?")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> eliminarProducto())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarProducto() {
        repository.eliminarProducto(codigoBarras);
        Toast.makeText(this, "🗑️ Producto eliminado", Toast.LENGTH_SHORT).show();
        finish();
    }
}
