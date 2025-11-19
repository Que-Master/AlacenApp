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
import java.util.List;
import java.util.Locale;
import java.util.Date;

public class TransaccionesAdapter extends RecyclerView.Adapter<TransaccionesAdapter.ViewHolder> {

    private final List<Transaccion> lista;

    public TransaccionesAdapter(List<Transaccion> lista) {
        // La lista ya viene ordenada desde el MainActivity, así que la usamos directo
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

        // 1. Formatear hora para mostrarla al final del mensaje (opcional pero útil)
        Date fecha = new Date(t.getTimestamp());
        SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String hora = formatoHora.format(fecha);

        // 2. Construir el mensaje personalizado
        String nombreProducto = (t.getProducto() != null) ? t.getProducto() : "Producto desconocido";
        String tipo = (t.getTipo() != null) ? t.getTipo() : "";
        String mensajeFinal;

        // Lógica de mensajes según tu requerimiento
        switch (tipo) {
            case "creacion":
                mensajeFinal = "Nuevo producto: " + nombreProducto;
                break;
            case "entrada":
                mensajeFinal = "Has ingresado: " + nombreProducto;
                break;
            case "salida":
                mensajeFinal = "Has retirado: " + nombreProducto;
                break;
            case "cambio_stock":
                // Intentamos obtener el stock final. Si tu modelo no tiene getStockFinal(),
                // asegúrate de agregarlo o cambiarlo por getCambio()
                int stock = t.getStock_final();
                mensajeFinal = "Stock actualizado: " + nombreProducto + " (" + stock + ")";
                break;
            case "actualizado":
                mensajeFinal = "Producto modificado: " + nombreProducto;
                break;
            case "eliminado":
            case "eliminacion":
                mensajeFinal = "Producto eliminado: " + nombreProducto;
                break;
            default:
                // Fallback para tipos desconocidos
                mensajeFinal = "Acción: " + tipo + " - " + nombreProducto;
                break;
        }

        // Agregamos la hora al final para contexto
        holder.textDescripcion.setText(mensajeFinal + " • " + hora);


        // 3. Configurar Iconos y Colores
        int iconoRes;
        int colorRes;

        switch (tipo) {
            case "creacion":
                iconoRes = R.drawable.ic_agregar; // Usamos el + que ya tienes
                colorRes = Color.parseColor("#2E7D32"); // Verde oscuro
                break;
            case "entrada":
                iconoRes = R.drawable.ic_up; // Flecha arriba nativa
                colorRes = Color.parseColor("#4CAF50"); // Verde
                break;
            case "salida":
                iconoRes = R.drawable.ic_down; // Flecha abajo nativa
                colorRes = Color.parseColor("#F44336"); // Rojo
                break;
            case "cambio_stock":
            case "actualizado":
                iconoRes = R.drawable.ic_edit; // Lápiz de edición
                colorRes = Color.parseColor("#FFC107"); // Ámbar/Amarillo
                break;
            case "eliminado":
            case "eliminacion":
                iconoRes = R.drawable.ic_delete; // Basurero
                colorRes = Color.parseColor("#D32F2F"); // Rojo alerta
                break;
            default:
                iconoRes = R.drawable.ic_info;
                colorRes = Color.parseColor("#9E9E9E"); // Gris
                break;
        }

        holder.icon.setImageResource(iconoRes);
        holder.icon.setColorFilter(colorRes);
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