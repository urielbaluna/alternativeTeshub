package com.example.teshub_v1.ui.usuarios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.PublicacionInfo

class PerfilPublicacionAdapter(
    private var publicaciones: MutableList<PublicacionInfo>,
    private val onItemClick: (PublicacionInfo) -> Unit,
    private val onDeleteClick: (PublicacionInfo) -> Unit // callback para eliminar
) : RecyclerView.Adapter<PerfilPublicacionAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloPublicacion)
        val tvHace: TextView = view.findViewById(R.id.tvHaceCuanto)
        val btnEliminar: ImageView = view.findViewById(R.id.btnEliminarPublicacion)


        init {
            // Click en toda la tarjeta
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(publicaciones[position])
                }
            }

            // Click en botón eliminar
            btnEliminar.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(publicaciones[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_publicacion_perfil, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = publicaciones[position]
        holder.tvTitulo.text = item.proyecto_nombre
        holder.tvHace.text = item.hace_cuanto
    }

    override fun getItemCount() = publicaciones.size

    fun updateList(newList: List<PublicacionInfo>) {
        publicaciones.clear()
        publicaciones.addAll(newList)
        notifyDataSetChanged()
    }

    fun removeItem(publicacion: PublicacionInfo) {
        val position = publicaciones.indexOf(publicacion)
        if (position != -1) {
            publicaciones.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
