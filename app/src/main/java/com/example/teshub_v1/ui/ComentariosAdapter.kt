package com.example.teshub_v1.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Comentario

class ComentariosAdapter(
    private val comentarios: List<Comentario>
) : RecyclerView.Adapter<ComentariosAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreUsuario)
        val tvComentario: TextView = view.findViewById(R.id.tvTextoComentario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comentario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = comentarios[position]

        holder.tvNombre.text = "${item.nombre}"
        holder.tvComentario.text = item.comentario
    }

    override fun getItemCount() = comentarios.size
}