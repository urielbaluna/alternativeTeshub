package com.example.teshub_v1.ui.publicaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Publicacion

class PublicacionesAdapter(
    private var publicaciones: List<Publicacion>,
    private val onClick: (Publicacion) -> Unit,
    private val onComentariosClick: (Publicacion) -> Unit
) : RecyclerView.Adapter<PublicacionesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tv_titulo_proyecto)
        val tvDescripcion: TextView = view.findViewById(R.id.tv_descripcion_proyecto)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha_proyecto)
        val tvCalificacion: TextView = view.findViewById(R.id.tv_calificacion)
        val layoutComentarios: LinearLayout = view.findViewById(R.id.comentariosLayout)
        val tvContadorComentarios: TextView = view.findViewById(R.id.contadorComentarios)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_publicacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = publicaciones[position]
        holder.tvTitulo.text = item.nombre
        holder.tvDescripcion.text = item.descripcion
        holder.tvFecha.text = if (item.fecha.length >= 10) item.fecha.substring(0, 10) else item.fecha
        holder.tvCalificacion.text = "\u2605 ${item.calificacion ?: "0.0"}"
        val cantidad = item.comentarios?.size ?: 0
        holder.tvContadorComentarios.text = cantidad.toString()
        holder.itemView.setOnClickListener { onClick(item) }
        holder.layoutComentarios.setOnClickListener {
            onComentariosClick(item)
        }
    }

    override fun getItemCount(): Int = publicaciones.size

    fun updateList(newList: List<Publicacion>) {
        publicaciones = newList
        notifyDataSetChanged()
    }
}