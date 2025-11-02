package com.example.myapplication.repository;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.myapplication.models.Producto;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {

    private final DatabaseReference productosRef;
    private final DatabaseReference transaccionesRef;
    private final Context context;

    public interface OnProductosListener {
        void onProductosObtenidos(List<Producto> lista);
        void onError(String error);
    }

    public FirebaseRepository(Context context) {
        this.context = context;
        productosRef = FirebaseDatabase.getInstance().getReference("productos");
        transaccionesRef = FirebaseDatabase.getInstance().getReference("transacciones");
    }


    // LEER PRODUCTOS
    public void leerProductos(OnProductosListener listener) {
        DatabaseReference refProductos = FirebaseDatabase.getInstance().getReference("productos");
        DatabaseReference refDetalles = FirebaseDatabase.getInstance().getReference("productos_detalle");

        refProductos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotProductos) {
                refDetalles.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshotDetalles) {
                        List<Producto> lista = new ArrayList<>();

                        for (DataSnapshot snapProd : snapshotProductos.getChildren()) {
                            Producto p = new Producto();

                            //Leer los campos del nodo "productos"
                            String codigo = snapProd.child("codigo_barras").getValue(String.class);
                            if (codigo == null || codigo.isEmpty()) {
                                codigo = snapProd.getKey(); // fallback
                            }
                            p.setCodigoBarras(codigo);
                            p.setNombre(snapProd.child("nombre").getValue(String.class));
                            p.setStock(snapProd.child("stock").getValue(Integer.class));

                            //Buscar detalles usando el mismo código de barras (no el key del nodo)
                            DataSnapshot snapDetalle = snapshotDetalles.child(p.getCodigoBarras());
                            if (snapDetalle.exists()) {
                                p.setImagenUrl(snapDetalle.child("imagen_url").getValue(String.class));
                                p.setCantidad(snapDetalle.child("cantidad").getValue(String.class));
                                p.setCategoria(snapDetalle.child("categoria").getValue(String.class));
                                p.setMarca(snapDetalle.child("marca").getValue(String.class));

                                Long stockMinimoLong = snapDetalle.child("stockMinimo").getValue(Long.class);
                                p.setStockMinimo(stockMinimoLong != null ? stockMinimoLong.intValue() : 0);
                            }

                            lista.add(p);
                        }

                        listener.onProductosObtenidos(lista);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onError(error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });
    }





    //AGREGAR PRODUCTO
    public void agregarProducto(Producto producto, Uri imagenUri) {
        String codigo = producto.getCodigoBarras();
        if (codigo == null || codigo.isEmpty()) {
            codigo = productosRef.push().getKey();
            producto.setCodigoBarras(codigo);
        }

        long timestamp = System.currentTimeMillis();
        producto.setUltimaActualizacion(timestamp);

        StorageReference storageRef = FirebaseStorage.getInstance()
                .getReference("productos/" + codigo + ".jpg");

        if (imagenUri != null) {
            try {
                context.getContentResolver().takePersistableUriPermission(
                        imagenUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception e) {
                Log.w("FirebaseRepo", "⚠️ No se pudo persistir permiso: " + e.getMessage());
            }

            try {
                java.io.InputStream inputStream = context.getContentResolver().openInputStream(imagenUri);
                java.io.File tempFile = java.io.File.createTempFile("upload_", ".jpg", context.getCacheDir());
                java.io.OutputStream outputStream = new java.io.FileOutputStream(tempFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();

                Uri fileUri = Uri.fromFile(tempFile);

                storageRef.putFile(fileUri)
                        .addOnProgressListener(snapshot -> {
                            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                            Log.d("FirebaseRepo", "📤 Subiendo imagen... " + (int) progress + "%");
                        })
                        .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            producto.setImagenUrl(downloadUri.toString());
                            Log.d("FirebaseRepo", "✅ Imagen subida correctamente: " + downloadUri);
                            guardarEstructuraCorrecta(producto, timestamp);
                            registrarTransaccion("creacion", producto);
                        }))
                        .addOnFailureListener(e -> {
                            Log.e("FirebaseRepo", "❌ Error subiendo imagen: " + e.getMessage());
                            producto.setImagenUrl("");
                            guardarEstructuraCorrecta(producto, timestamp);
                            registrarTransaccion("creacion", producto);
                        });

            } catch (Exception e) {
                Log.e("FirebaseRepo", "❌ Error procesando imagen local: " + e.getMessage());
                producto.setImagenUrl("");
                guardarEstructuraCorrecta(producto, timestamp);
                registrarTransaccion("creacion", producto);
            }

        } else {
            producto.setImagenUrl("");
            guardarEstructuraCorrecta(producto, timestamp);
            registrarTransaccion("creacion", producto);
            Log.w("FirebaseRepo", "⚠️ Producto guardado sin imagen");
        }
    }





    private void guardarProducto(Producto producto, long timestamp) {
        productosRef.child(producto.getCodigoBarras()).setValue(producto)
                .addOnSuccessListener(aVoid -> {
                    registrarTransaccion("creacion", producto);
                    Toast.makeText(context, "✅ Producto agregado correctamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Log.e("FirebaseRepo", "Error guardando producto: " + e.getMessage()));
    }



    // ACTUALIZAR PRODUCTO
    public void actualizarProducto(Producto producto, Uri nuevaImagenUri) {
        String codigo = producto.getCodigoBarras();
        if (codigo == null || codigo.isEmpty()) {
            Log.e("FirebaseRepo", "❌ No se puede actualizar: código vacío");
            return;
        }

        DatabaseReference refDetalles = FirebaseDatabase.getInstance().getReference("productos_detalle").child(codigo);

        refDetalles.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int stockAnterior = snapshot.child("stock").getValue(Integer.class) != null
                        ? snapshot.child("stock").getValue(Integer.class)
                        : 0;

                long timestamp = System.currentTimeMillis();
                StorageReference storageRef = FirebaseStorage.getInstance()
                        .getReference("productos/" + codigo + ".jpg");

                Runnable guardar = () -> {
                    guardarEstructuraCorrecta(producto, timestamp);

                    if (producto.getStock() != stockAnterior) {
                        registrarTransaccion("cambio_stock", producto);
                    } else {
                        registrarTransaccion("actualizado", producto);
                    }
                };

                if (nuevaImagenUri != null) {
                    storageRef.putFile(nuevaImagenUri)
                            .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                producto.setImagenUrl(downloadUri.toString());
                                guardar.run();
                            }))
                            .addOnFailureListener(e -> Log.e("FirebaseRepo", "Error subiendo nueva imagen: " + e.getMessage()));
                } else {
                    guardar.run();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseRepo", "Error al leer stock anterior: " + error.getMessage());
            }
        });
    }



    // ELIMINAR PRODUCTO
    public void eliminarProducto(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.isEmpty()) {
            Log.e("FirebaseRepo", "❌ Código vacío, no se puede eliminar.");
            return;
        }

        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        db.child("productos").child(codigoBarras).removeValue();
        db.child("productos_detalle").child(codigoBarras).get()
                .addOnSuccessListener(snapshot -> {
                    String nombre = snapshot.child("nombre").getValue(String.class);
                    Producto p = new Producto();
                    p.setCodigoBarras(codigoBarras);
                    p.setNombre(nombre != null ? nombre : "Producto");
                    registrarTransaccion("eliminado", p);
                });

        FirebaseStorage.getInstance().getReference("productos/" + codigoBarras + ".jpg")
                .delete()
                .addOnSuccessListener(aVoid -> Log.d("FirebaseRepo", "Imagen eliminada correctamente"))
                .addOnFailureListener(e -> Log.w("FirebaseRepo", "No se pudo eliminar la imagen: " + e.getMessage()));
    }



    // REGISTRAR TRANSACCIÓN
    private void registrarTransaccion(String tipo, Producto producto) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("transacciones");
        String id = ref.push().getKey();

        if (producto == null) return;

        long timestamp = System.currentTimeMillis();
        String hora = new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date(timestamp));
        String mensaje = "";

        switch (tipo) {
            case "creacion":
                mensaje = "Ingresaste " + producto.getStock() + " " + producto.getNombre() + " a las " + hora;
                break;

            case "actualizado":
                mensaje = "Editaste " + producto.getNombre() + " a las " + hora;
                break;

            case "cambio_stock":
                mensaje = "Actualizaste el stock de " + producto.getNombre() + " (" + producto.getStock() + ") a las " + hora;
                break;

            case "eliminado":
                mensaje = "Eliminaste " + producto.getNombre() + " a las " + hora;
                break;

            default:
                mensaje = "Acción realizada sobre " + producto.getNombre() + " a las " + hora;
                break;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("tipo", tipo);
        data.put("codigo", producto.getCodigoBarras());
        data.put("producto", producto.getNombre());
        data.put("stock_final", producto.getStock());
        data.put("mensaje", mensaje);
        data.put("timestamp", timestamp);

        ref.child(id).setValue(data);
    }


    private void guardarEstructuraCorrecta(Producto producto, long timestamp) {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();

        // --- Nodo productos ---
        Map<String, Object> datosBasicos = new HashMap<>();
        datosBasicos.put("codigo_barras", producto.getCodigoBarras());
        datosBasicos.put("nombre", producto.getNombre());
        datosBasicos.put("stock", producto.getStock());
        datosBasicos.put("ultima_actualizacion", timestamp);
        datosBasicos.put("ultima_operacion", "entrada");
        db.child("productos").child(producto.getCodigoBarras()).setValue(datosBasicos);

        // --- Nodo productos_detalle ---
        Map<String, Object> detalles = new HashMap<>();
        detalles.put("codigo_barras", producto.getCodigoBarras());
        detalles.put("nombre", producto.getNombre());
        detalles.put("marca", producto.getMarca());
        detalles.put("categoria", producto.getCategoria());
        detalles.put("cantidad", producto.getCantidad());
        detalles.put("stock", producto.getStock());
        detalles.put("stockMinimo", producto.getStockMinimo());
        detalles.put("imagen_url", producto.getImagenUrl());
        detalles.put("ultima_actualizacion", timestamp);

        db.child("productos_detalle").child(producto.getCodigoBarras()).setValue(detalles);
    }


}
