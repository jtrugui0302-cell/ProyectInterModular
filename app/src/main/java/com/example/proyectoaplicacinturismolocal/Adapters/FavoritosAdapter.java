package com.example.proyectoaplicacinturismolocal.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.proyectoaplicacinturismolocal.Models.Sitio;
import com.example.proyectoaplicacinturismolocal.R;

import java.util.List;

public class FavoritosAdapter extends RecyclerView.Adapter<FavoritosAdapter.ViewHolder> {
    private List<Sitio> lista;
    private OnEliminarClickListener listener;

    // Interfaz para avisar al Main cuando alguien pulsa borrar
    public interface OnEliminarClickListener {
        void onEliminarClick(int posicion);
    }

    public FavoritosAdapter(List<Sitio> lista, OnEliminarClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.lugarfavorito, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sitio s = lista.get(position);
        holder.titulo.setText(s.getNombre());
        holder.categoria.setText(s.getDescripcion()); // O la distancia si la tienes

        Glide.with(holder.itemView.getContext())
                .load(s.getUrlImagen())
                .into(holder.imagen);

        // Configurar el botón de borrar de tu layout
        holder.btnBorrar.setOnClickListener(v -> {
            if (listener != null) listener.onEliminarClick(position);
        });
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titulo, categoria;
        ImageView imagen;
        ImageButton btnBorrar;

        public ViewHolder(View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.item_titulo);
            categoria = itemView.findViewById(R.id.item_categoria_distancia);
            imagen = itemView.findViewById(R.id.item_imagen);
            btnBorrar = itemView.findViewById(R.id.item_btn_accion);
        }
    }
}