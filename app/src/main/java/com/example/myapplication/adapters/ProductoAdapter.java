package com.example.myapplication.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.myapplication.EditProductActivity;
import com.example.myapplication.R;
import com.example.myapplication.models.Producto;
import com.example.myapplication.repository.FirebaseRepository;

import java.util.List;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder> {

    private final Context context;
    private final List<Producto> listaProductos;
    private final FirebaseRepository repository;

    public ProductoAdapter(Context context, List<Producto> listaProductos) {
        this.context = context;
        this.listaProductos = listaProductos;
        this.repository = new FirebaseRepository(context);
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alacena, parent, false);
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        Producto producto = listaProductos.get(position);

        holder.textNombre.setText(producto.getNombre());
        holder.textMarca.setText("Marca: " + producto.getMarca());
        holder.textStock.setText("Stock: " + producto.getStock());

        String imagenUrl = producto.getImagenUrl();

        if (imagenUrl != null && !imagenUrl.isEmpty()) {
            if (imagenUrl.startsWith("content://") || imagenUrl.startsWith("file://")) {
                // 📁 Imagen local
                Glide.with(context)
                        .load(Uri.parse(imagenUrl))
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.imageProducto);
            } else {
                // Imagen en Firebase Storage
                Glide.with(context)
                        .load(imagenUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.imageProducto);
            }
        } else {
            holder.imageProducto.setImageResource(R.drawable.placeholder_image);
        }

        // Botón editar
        holder.btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditProductActivity.class);
            intent.putExtra("producto", producto);
            context.startActivity(intent);
        });

        // Botón eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Eliminar producto")
                    .setMessage("¿Deseas eliminar " + producto.getNombre() + "?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        repository.eliminarProducto(producto.getCodigoBarras());
                        Toast.makeText(context, "Producto eliminado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return listaProductos.size();
    }

    public static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView textNombre, textMarca, textStock;
        ImageView imageProducto;
        ImageButton btnEditar, btnEliminar;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            textNombre = itemView.findViewById(R.id.text_product_name);
            textMarca = itemView.findViewById(R.id.text_marca);
            textStock = itemView.findViewById(R.id.text_stock);
            imageProducto = itemView.findViewById(R.id.image_product);
            btnEditar = itemView.findViewById(R.id.btn_edit_product);
            btnEliminar = itemView.findViewById(R.id.btn_delete_product);
        }
    }
}
