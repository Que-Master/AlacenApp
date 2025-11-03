package com.example.myapplication.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Transaccion;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransaccionesAdapter extends RecyclerView.Adapter<TransaccionesAdapter.ViewHolder> {

    private final List<Transaccion> lista;

    public TransaccionesAdapter(List<Transaccion> lista) {
        // Ordenar de más reciente a más antigua
        Collections.reverse(lista);
            this.lista = lista;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaccion t = lista.get(position);

        // Formatear hora legible
        Date fecha = new Date(t.getTimestamp());
        SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String hora = formatoHora.format(fecha);

        // Mensaje (ya viene listo desde Firebase)
        String texto = t.getMensaje() != null && !t.getMensaje().isEmpty()
                ? t.getMensaje()
                : "Acción sobre " + t.getProducto() + " a las " + hora;

        // Icono y color según tipo
        int icono;
        int colorIcono;

        switch (t.getTipo()) {
            case "creacion":
            case "entrada":
                icono = R.drawable.ic_up;
                colorIcono = Color.parseColor("#4CAF50"); // Verde
                break;

            case "cambio_stock":
            case "actualizado":
                icono = R.drawable.ic_edit;
                colorIcono = Color.parseColor("#FFC107"); // Amarillo
                break;

            case "eliminado":
            case "eliminacion":
                icono = R.drawable.ic_delete;
                colorIcono = Color.parseColor("#F44336"); // Rojo
                break;

            default:
                icono = R.drawable.ic_info;
                colorIcono = Color.parseColor("#2196F3"); // Azul
                break;
        }

        // Asignar valores al ViewHolder
        holder.textDescripcion.setText(texto);
        holder.icon.setImageResource(icono);
        holder.icon.setColorFilter(colorIcono);
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    // --- Clase ViewHolder ---
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textDescripcion;
        final ImageView icon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDescripcion = itemView.findViewById(R.id.text_action_description);
            icon = itemView.findViewById(R.id.icon_action_type);
        }
    }
}
