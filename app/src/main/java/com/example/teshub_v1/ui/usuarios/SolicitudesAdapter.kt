package com.example.teshub_v1.ui.usuarios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.SolicitudInfo

class SolicitudesAdapter(
    private var solicitudes: MutableList<SolicitudInfo>,
    private val onActionClick: (SolicitudInfo, String) -> Unit // "aceptar" o "rechazar"
) : RecyclerView.Adapter<SolicitudesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_alumno_avatar)
        val tvNombre: TextView = view.findViewById(R.id.tv_alumno_nombre)
        val tvCarrera: TextView = view.findViewById(R.id.tv_alumno_carrera)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha_solicitud)
        val btnAceptar: Button = view.findViewById(R.id.btn_aceptar)
        val btnRechazar: Button = view.findViewById(R.id.btn_rechazar)

        init {
            btnAceptar.setOnClickListener { onActionClick(solicitudes[adapterPosition], "aceptar") }
            btnRechazar.setOnClickListener { onActionClick(solicitudes[adapterPosition], "rechazar") }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_solicitud_asesoria, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = solicitudes[position]
        holder.tvNombre.text = "${item.nombre} ${item.apellido}"

        // CORRECCIÓN: Uso seguro del operador Elvis (?:)
        holder.tvCarrera.text = item.carrera ?: "Carrera no especificada"
        holder.tvFecha.text = "Matrícula: ${item.matricula}"

        // Carga de Imagen
        if (!item.imagen.isNullOrEmpty()) {
            // Limpieza de ruta para cargar desde el servidor local
            val rutaLimpia = item.imagen.replace("\\", "/")
            val fullUrl = "${BuildConfig.API_BASE_URL.removeSuffix("/api").removeSuffix("/")}/$rutaLimpia"
            Glide.with(holder.itemView.context).load(fullUrl).into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_profile)
        }
    }

    override fun getItemCount() = solicitudes.size

    fun removeItem(item: SolicitudInfo) {
        val pos = solicitudes.indexOf(item)
        if (pos != -1) {
            solicitudes.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }

    fun updateList(newList: List<SolicitudInfo>) {
        solicitudes.clear()
        solicitudes.addAll(newList)
        notifyDataSetChanged()
    }
}