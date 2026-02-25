package com.example.proyectoaplicacinturismolocal.Adapters;

import android.view.*;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.proyectoaplicacinturismolocal.Models.Comentarios;
import com.example.proyectoaplicacinturismolocal.R;
import java.util.List;

public class ComentarioAdapter extends RecyclerView.Adapter<ComentarioAdapter.ViewHolder> {
    private List<Comentarios> lista;
    private OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDelete(Comentarios c);
    }

    public ComentarioAdapter(List<Comentarios> lista, OnDeleteClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comentario, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Comentarios c = lista.get(position);
        holder.txtUser.setText(c.getAutor());
        holder.txtTexto.setText(c.getTexto());
        holder.txtFecha.setText(c.getFecha());

        // Al mantener pulsado o si tienes un botón de borrar
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onDelete(c);
            return true;
        });
    }

    @Override public int getItemCount() { return lista.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtUser, txtTexto, txtFecha;
        public ViewHolder(View v) {
            super(v);
            txtUser = v.findViewById(R.id.item_com_autor);
            txtTexto = v.findViewById(R.id.item_com_texto);
            txtFecha = v.findViewById(R.id.item_com_fecha);
        }
    }
}