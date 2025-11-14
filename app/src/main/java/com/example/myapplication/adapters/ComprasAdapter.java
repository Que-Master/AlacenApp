package com.example.myapplication.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.view.DetallesActivity;
import com.example.myapplication.R;
import com.example.myapplication.models.Producto;
import com.example.myapplication.repository.FirebaseRepository;
import com.example.myapplication.view.EditProductActivity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComprasAdapter extends RecyclerView.Adapter<ComprasAdapter.ViewHolder> {

    private final Context context;
    private final List<Producto> lista;
    private final FirebaseRepository repository;

    public ComprasAdapter(Context context, List<Producto> lista) {
        this.context = context;
        this.lista = lista;
        this.repository = new FirebaseRepository(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Producto producto = lista.get(position);

        holder.textNombre.setText(producto.getNombre());
        holder.textDetalle.setText(producto.getCategoria() + " • " + producto.getCantidad());

        // Imagen del producto
        if (producto.getImagenUrl() != null && !producto.getImagenUrl().isEmpty()) {
            Glide.with(context)
                    .load(producto.getImagenUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(holder.imageProducto);
        } else {
            holder.imageProducto.setImageResource(R.drawable.placeholder_image);
        }

        // SharedPreferences para guardar marcados localmente
        SharedPreferences prefs = context.getSharedPreferences("compras_prefs", Context.MODE_PRIVATE);
        Set<String> marcados = new HashSet<>(prefs.getStringSet("productos_comprados", new HashSet<>()));

        // Restaurar estado guardado localmente
        boolean estaMarcado = marcados.contains(producto.getCodigoBarras());
        holder.checkComprado.setOnCheckedChangeListener(null);
        holder.checkComprado.setChecked(estaMarcado);

        // Guardar cambios localmente (sin Firebase ni campos extra)
        holder.checkComprado.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Set<String> actualizados = new HashSet<>(prefs.getStringSet("productos_comprados", new HashSet<>()));

            if (isChecked) {
                actualizados.add(producto.getCodigoBarras());
                Toast.makeText(context, "✅ Marcado como comprado: " + producto.getNombre(), Toast.LENGTH_SHORT).show();
            } else {
                actualizados.remove(producto.getCodigoBarras());
                Toast.makeText(context, "❎ Desmarcado: " + producto.getNombre(), Toast.LENGTH_SHORT).show();
            }

            prefs.edit().putStringSet("productos_comprados", actualizados).apply();
        });

        // Editar → EditProductActivity
        holder.btnEditar.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditProductActivity.class);
            intent.putExtra("producto", producto);
            context.startActivity(intent);
        });

        // Eliminar → Confirmación
        holder.btnEliminar.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Eliminar producto")
                    .setMessage("¿Seguro que deseas eliminar \"" + producto.getNombre() + "\"?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        repository.eliminarProducto(producto.getCodigoBarras());
                        lista.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(context, "🗑️ Producto eliminado", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    // ViewHolder limpio
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProducto;
        TextView textNombre, textDetalle;
        ImageButton btnEditar, btnEliminar;
        CheckBox checkComprado;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProducto = itemView.findViewById(R.id.image_producto);
            textNombre = itemView.findViewById(R.id.text_nombre);
            textDetalle = itemView.findViewById(R.id.text_detalle);
            btnEditar = itemView.findViewById(R.id.btn_editar);
            btnEliminar = itemView.findViewById(R.id.btn_eliminar);
            checkComprado = itemView.findViewById(R.id.check_comprado);
        }
    }
}
