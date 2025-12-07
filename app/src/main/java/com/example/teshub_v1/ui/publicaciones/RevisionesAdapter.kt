package com.example.teshub_v1.ui.publicaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Publicacion
import com.example.teshub_v1.data.model.RevisionPendienteInfo

class RevisionesAdapter(
    private var pendientes: List<RevisionPendienteInfo>, // <--- TIPO CAMBIADO
    private val onClick: (RevisionPendienteInfo) -> Unit
) : RecyclerView.Adapter<RevisionesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tv_titulo_tesis)
        val tvAutor: TextView = view.findViewById(R.id.tv_autor_tesis)
        val btnRevisar: View = view.findViewById(R.id.btn_ir_revision)
        val tvEstado: TextView = view.findViewById(R.id.tv_estado_tesis)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_revision_pendiente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = pendientes[position]
        holder.tvTitulo.text = item.titulo
        holder.tvAutor.text = item.autor ?: "Desconocido"
        holder.tvEstado.text = "• ${item.estado?.uppercase() ?: "Pendiente"}"

        holder.itemView.setOnClickListener { onClick(item) }
        holder.btnRevisar.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = pendientes.size

    fun updateList(newList: List<RevisionPendienteInfo>) {
        pendientes = newList
        notifyDataSetChanged()
    }
}