package com.example.myapplication;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.models.Producto;
import com.example.myapplication.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EditProductActivity extends AppCompatActivity {

    private TextInputEditText editName, editBrand, editCategory, editCantidad,
            editStock, editMinStock, editBarcode;
    private ImageView imagePreview;
    private MaterialButton btnSelectImage, btnUpdate;

    private FirebaseRepository repository;
    private Producto productoActual;
    private Uri nuevaImagenUri;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    nuevaImagenUri = uri;
                    Glide.with(this)
                            .load(uri)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imagePreview);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_product);

        repository = new FirebaseRepository(this);

        // Inicializar vistas
        editName = findViewById(R.id.edit_name);
        editBrand = findViewById(R.id.edit_brand);
        editCategory = findViewById(R.id.edit_category);
        editCantidad = findViewById(R.id.edit_cantidad);
        editStock = findViewById(R.id.edit_stock);
        editMinStock = findViewById(R.id.edit_min_stock);
        editBarcode = findViewById(R.id.edit_barcode);
        imagePreview = findViewById(R.id.image_preview);
        btnSelectImage = findViewById(R.id.btn_select_image);
        btnUpdate = findViewById(R.id.btn_update_product);

        // Recibir producto desde el intent
        productoActual = (Producto) getIntent().getSerializableExtra("producto");

        if (productoActual != null) {
            cargarDatosProductoFirebase(productoActual.getCodigoBarras());
        }

        // Cambiar imagen
        btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // Guardar cambios
        btnUpdate.setOnClickListener(v -> actualizarProducto());
    }

    private void cargarDatosProductoFirebase(String codigoBarras) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("productos_detalle")
                .child(codigoBarras);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(EditProductActivity.this, "❌ Producto no encontrado", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                // Crear objeto producto actualizado
                productoActual = snapshot.getValue(Producto.class);
                if (productoActual == null) return;

                // Cargar valores
                editName.setText(productoActual.getNombre());
                editBrand.setText(productoActual.getMarca());
                editCategory.setText(productoActual.getCategoria());
                editCantidad.setText(productoActual.getCantidad());
                editStock.setText(String.valueOf(productoActual.getStock()));
                editBarcode.setText(productoActual.getCodigoBarras());

                // Leer "stock_minimo" o "stockMinimo" según exista
                Integer stockMinimo = snapshot.child("stock_minimo").getValue(Integer.class);
                if (stockMinimo == null) {
                    stockMinimo = snapshot.child("stockMinimo").getValue(Integer.class);
                }
                editMinStock.setText(stockMinimo != null ? String.valueOf(stockMinimo) : "0");

                // Imagen
                String imagenUrl = snapshot.child("imagen_url").getValue(String.class);
                if (imagenUrl != null && !imagenUrl.isEmpty()) {
                    Glide.with(EditProductActivity.this)
                            .load(imagenUrl)
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.error_image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(imagePreview);
                } else {
                    imagePreview.setImageResource(R.drawable.placeholder_image);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditProductActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarProducto() {
        if (productoActual == null) return;

        // Validaciones básicas
        if (editName.getText().toString().trim().isEmpty() ||
                editBrand.getText().toString().trim().isEmpty() ||
                editCategory.getText().toString().trim().isEmpty() ||
                editCantidad.getText().toString().trim().isEmpty() ||
                editBarcode.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Actualizar datos
        productoActual.setNombre(editName.getText().toString().trim());
        productoActual.setMarca(editBrand.getText().toString().trim());
        productoActual.setCategoria(editCategory.getText().toString().trim());
        productoActual.setCantidad(editCantidad.getText().toString().trim());
        productoActual.setStock(Integer.parseInt(editStock.getText().toString().trim()));
        productoActual.setStockMinimo(Integer.parseInt(editMinStock.getText().toString().trim()));
        productoActual.setCodigoBarras(editBarcode.getText().toString().trim());

        // Enviar a Firebase
        repository.actualizarProducto(productoActual, nuevaImagenUri);

        Toast.makeText(this, "Producto actualizado correctamente ✅", Toast.LENGTH_SHORT).show();
        finish();
    }
}
