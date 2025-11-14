package com.example.myapplication.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.R;
import com.example.myapplication.models.Producto;
import com.example.myapplication.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class EditProductActivity extends AppCompatActivity {

    private TextInputEditText editName, editBrand, editCategory, editCantidad,
            editStock, editMinStock, editBarcode;
    private ImageView imagePreview;
    private MaterialButton btnSelectImage, btnUpdate;

    private FirebaseRepository repository;
    private Producto productoActual;
    private Uri nuevaImagenUri;

    // ----------------------------
    //  PICKER DE IMÁGENES
    // ----------------------------
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {

                if (uri != null) {

                    Log.d("EditProduct", "📸 Imagen seleccionada: " + uri);

                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        int sizeMB = is.available() / (1024 * 1024);
                        is.close();

                        Log.d("EditProduct", "📦 Peso imagen: " + sizeMB + " MB");

                        Uri imagenProcesada;

                        if (sizeMB > 5) {
                            Toast.makeText(this,
                                    "La imagen supera los 5 MB (" + sizeMB + " MB). Se comprimirá automáticamente.",
                                    Toast.LENGTH_LONG).show();

                            imagenProcesada = comprimirImagen(uri);

                        } else {
                            imagenProcesada = copiarImagenLocal(uri);
                        }

                        nuevaImagenUri = imagenProcesada;

                        if (nuevaImagenUri != null) {
                            imagePreview.setImageURI(nuevaImagenUri);
                        } else {
                            Toast.makeText(this, "Error procesando imagen", Toast.LENGTH_SHORT).show();
                        }

                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        } catch (Exception ignored) {}

                    } catch (Exception e) {
                        Toast.makeText(this,
                                "Error leyendo imagen: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(this, "No seleccionaste ninguna imagen", Toast.LENGTH_SHORT).show();
                }

            });

    // ----------------------------
    //  ON CREATE
    // ----------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_product);

        repository = new FirebaseRepository(this);

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

        productoActual = (Producto) getIntent().getSerializableExtra("producto");

        if (productoActual != null) {
            cargarDatosProductoFirebase(productoActual.getCodigoBarras());
        }

        btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnUpdate.setOnClickListener(v -> actualizarProducto());
    }

    // ----------------------------
    // CARGAR DATOS
    // ----------------------------
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

                productoActual = snapshot.getValue(Producto.class);

                if (productoActual == null) return;

                editName.setText(productoActual.getNombre());
                editBrand.setText(productoActual.getMarca());
                editCategory.setText(productoActual.getCategoria());
                editCantidad.setText(productoActual.getCantidad());
                editStock.setText(String.valueOf(productoActual.getStock()));
                editBarcode.setText(productoActual.getCodigoBarras());

                Integer stockMinimo = snapshot.child("stock_minimo").getValue(Integer.class);
                if (stockMinimo == null)
                    stockMinimo = snapshot.child("stockMinimo").getValue(Integer.class);

                editMinStock.setText(stockMinimo != null ? String.valueOf(stockMinimo) : "0");

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

    // ----------------------------
    //  ACTUALIZAR
    // ----------------------------
    private void actualizarProducto() {

        if (productoActual == null) return;

        productoActual.setNombre(editName.getText().toString().trim());
        productoActual.setMarca(editBrand.getText().toString().trim());
        productoActual.setCategoria(editCategory.getText().toString().trim());
        productoActual.setCantidad(editCantidad.getText().toString().trim());
        productoActual.setStock(Integer.parseInt(editStock.getText().toString().trim()));
        productoActual.setStockMinimo(Integer.parseInt(editMinStock.getText().toString().trim()));
        productoActual.setCodigoBarras(editBarcode.getText().toString().trim());

        repository.actualizarProducto(productoActual, nuevaImagenUri);

        Toast.makeText(this, "Producto actualizado correctamente ✅", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ----------------------------
    //  COPIAR IMAGEN
    // ----------------------------
    private Uri copiarImagenLocal(Uri originalUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(originalUri);
            File tempFile = new File(getCacheDir(), "temp_edit_" + System.currentTimeMillis() + ".jpg");
            OutputStream outputStream = new FileOutputStream(tempFile);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return Uri.fromFile(tempFile);

        } catch (Exception e) {
            Log.e("EditProduct", "❌ Error copiando imagen: " + e.getMessage());
            return null;
        }
    }

    // ----------------------------
    //  COMPRIMIR IMAGEN
    // ----------------------------
    private Uri comprimirImagen(Uri originalUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(originalUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int maxSize = 1280;

            if (width > maxSize || height > maxSize) {
                float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
                width = Math.round(width * ratio);
                height = Math.round(height * ratio);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            File tempFile = new File(getCacheDir(), "compressed_edit_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(tempFile);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();

            return Uri.fromFile(tempFile);

        } catch (Exception e) {
            Log.e("EditProduct", "❌ Error comprimiendo imagen: " + e.getMessage());
            return null;
        }
    }
}
