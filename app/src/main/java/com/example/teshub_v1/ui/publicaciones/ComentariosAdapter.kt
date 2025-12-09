package com.example.teshub_v1.ui.publicaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Comentario

class ComentariosAdapter(
    private var lista: MutableList<Comentario>,
    private val miMatricula: String, // <--- IMPORTANTE
    private val onDeleteClick: (Comentario) -> Unit
) : RecyclerView.Adapter<ComentariosAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar_comentario)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre_comentario)
        val tvTexto: TextView = view.findViewById(R.id.tv_texto_comentario)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha_comentario)
        val btnBorrar: ImageView = view.findViewById(R.id.btn_borrar_comentario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comentario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        holder.tvNombre.text = item.nombre
        holder.tvTexto.text = item.comentario

        // Formatear fecha simple (puedes usar tu helper de tiempoTranscurrido si lo traes)
        holder.tvFecha.text = item.fecha?.take(10) ?: ""

        // Cargar Avatar
        if (!item.imagen.isNullOrEmpty()) {
            val url = if (item.imagen.startsWith("http")) item.imagen else "${BuildConfig.API_BASE_URL}${item.imagen}"
            Glide.with(holder.itemView.context).load(url).placeholder(R.drawable.ic_profile).into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_profile)
        }

        // Lógica de Borrado: Solo si es MI comentario
        if (item.matricula == miMatricula) {
            holder.btnBorrar.visibility = View.VISIBLE
            holder.btnBorrar.setOnClickListener { onDeleteClick(item) }
        } else {
            holder.btnBorrar.visibility = View.GONE
        }
    }

    override fun getItemCount() = lista.size

    fun updateList(newList: List<Comentario>) {
        lista.clear()
        lista.addAll(newList)
        notifyDataSetChanged()
    }
}