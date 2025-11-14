package com.example.myapplication.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.models.Producto;
import com.example.myapplication.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


public class AddProductActivity extends AppCompatActivity {

    private TextInputEditText editName, editBrand, editCategory, editCantidad, editStock, editMinStock, editBarcode;
    private ImageView imagePreview;
    private MaterialButton btnGuardar, btnSelectImage;
    private FirebaseRepository repository;
    private Uri imageUri;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {

                    Log.d("AddProduct", "📸 Imagen seleccionada: " + uri);

                    try {
                        // Leer tamaño real de la imagen seleccionada
                        InputStream is = getContentResolver().openInputStream(uri);
                        int sizeMB = is.available() / (1024 * 1024);
                        is.close();

                        Log.d("AddProduct", "📦 Peso imagen: " + sizeMB + " MB");

                        if (sizeMB > 5) {
                            // Imagen muy grande → comprimir
                            Toast.makeText(this,
                                    "La imagen es muy pesada (" + sizeMB + " MB). Se comprimirá automáticamente.",
                                    Toast.LENGTH_LONG).show();

                            imageUri = comprimirImagen(uri);

                        } else {
                            // Imagen pequeña → copiar normal
                            imageUri = copiarImagenLocal(uri);
                        }

                        if (imageUri != null) {
                            imagePreview.setImageURI(imageUri);
                        } else {
                            Toast.makeText(this, "Error procesando la imagen", Toast.LENGTH_SHORT).show();
                        }

                        // Permisos persistentes (solo en Android 12+)
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
                    Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show();
                }
            });



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_product);

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
        btnGuardar = findViewById(R.id.btn_save_product);

        btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnGuardar.setOnClickListener(v -> guardarProducto());
    }

    private void guardarProducto() {
        String nombre = editName.getText().toString().trim();
        String marca = editBrand.getText().toString().trim();
        String categoria = editCategory.getText().toString().trim();
        String cantidad = editCantidad.getText().toString().trim();
        String stockStr = editStock.getText().toString().trim();
        String minStockStr = editMinStock.getText().toString().trim();
        String codigoBarras = editBarcode.getText().toString().trim();

        if (nombre.isEmpty() || marca.isEmpty() || categoria.isEmpty() || cantidad.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        int stock = Integer.parseInt(stockStr);
        int stockMinimo = minStockStr.isEmpty() ? 0 : Integer.parseInt(minStockStr);

        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setMarca(marca);
        producto.setCategoria(categoria);
        producto.setCantidad(cantidad);
        producto.setStock(stock);
        producto.setStockMinimo(stockMinimo);
        producto.setCodigoBarras(codigoBarras);

        repository.agregarProducto(producto, imageUri);
        Toast.makeText(this, "⏳ Subiendo producto...", Toast.LENGTH_SHORT).show();
        finish();
    }
    // ============================================================
    // 🔹 Copia normal de la imagen al caché local
    // ============================================================
    private Uri copiarImagenLocal(Uri originalUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(originalUri);
            File tempFile = new File(getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
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
            Log.e("AddProduct", "❌ Error copiando imagen: " + e.getMessage());
            return null;
        }
    }

    // ============================================================
    // 🔹 Compresión automática para imágenes grandes (>5 MB)
    // ============================================================
    private Uri comprimirImagen(Uri originalUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(originalUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            // 🚫 Si el bitmap es demasiado grande, redúcelo
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int maxSize = 1280; // 🔹 resolución máxima

            if (width > maxSize || height > maxSize) {
                float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
                width = Math.round(width * ratio);
                height = Math.round(height * ratio);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            File tempFile = new File(getCacheDir(), "compressed_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(tempFile);

            // 🔹 Compresión al 80% de calidad
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();

            Log.d("AddProduct", "✅ Imagen comprimida: " + tempFile.getAbsolutePath());
            return Uri.fromFile(tempFile);
        } catch (Exception e) {
            Log.e("AddProduct", "❌ Error comprimiendo imagen: " + e.getMessage());
            return null;
        }
    }
}
