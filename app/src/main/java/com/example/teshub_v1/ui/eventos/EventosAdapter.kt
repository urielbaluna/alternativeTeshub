package com.example.teshub_v1.ui.eventos

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Evento
import java.text.SimpleDateFormat
import java.util.Locale

class EventosAdapter(
    private var eventos: List<Evento>,
    private val onItemClick: (Evento) -> Unit
) : RecyclerView.Adapter<EventosAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivImagen: ImageView = view.findViewById(R.id.iv_evento_imagen)
        val tvCategoria: TextView = view.findViewById(R.id.tv_categoria)
        val tvTitulo: TextView = view.findViewById(R.id.tv_titulo_evento)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha_hora)
        val tvUbicacion: TextView = view.findViewById(R.id.tv_ubicacion)
        val tvAsistentes: TextView = view.findViewById(R.id.tv_asistentes)
        val tvEstado: TextView = view.findViewById(R.id.tv_estado_registro)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(eventos[adapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_evento, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val evento = eventos[position]

        holder.tvTitulo.text = evento.titulo
        holder.tvCategoria.text = evento.categoria ?: "General"
        holder.tvUbicacion.text = evento.ubicacionNombre ?: "Ver mapa"
        holder.tvAsistentes.text = "${evento.asistentesRegistrados}/${evento.cupoMaximo} Asistentes"

        // Formatear Fecha (El backend manda "YYYY-MM-DD HH:MM:SS" o ISO)
        try {
            // Ajusta el formato de entrada según lo que envíe tu backend (MySQL suele ser este)
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'", Locale.getDefault())
            // O si tu backend manda simple: "yyyy-MM-dd HH:mm:ss"

            val date = inputFormat.parse(evento.fecha)
            val outputFormat = SimpleDateFormat("EEE, d MMM • h:mm a", Locale.getDefault())
            holder.tvFecha.text = outputFormat.format(date ?: "")
        } catch (e: Exception) {
            holder.tvFecha.text = evento.fecha // Fallback si falla el parseo
        }

        // Estado de Registro
        if (evento.usuarioRegistrado) {
            holder.tvEstado.text = "REGISTRADO"
            holder.tvEstado.setTextColor(Color.GRAY)
        } else if (!evento.hayLugaresDisponibles) {
            holder.tvEstado.text = "LLENO"
            holder.tvEstado.setTextColor(Color.RED)
        } else {
            holder.tvEstado.text = "REGISTRARSE"
            holder.tvEstado.setTextColor(Color.parseColor("#2DA036"))
        }

        // Cargar Imagen
        if (!evento.urlFoto.isNullOrEmpty()) {
            val fullUrl = "${BuildConfig.API_BASE_URL}${evento.urlFoto}"
            Glide.with(holder.itemView.context)
                .load(fullUrl)
                .placeholder(R.drawable.ic_image) // Placeholder gris
                .centerCrop()
                .into(holder.ivImagen)
        } else {
            holder.ivImagen.setImageResource(R.drawable.ic_image) // Imagen por defecto
        }
    }

    override fun getItemCount() = eventos.size

    fun updateList(newList: List<Evento>) {
        eventos = newList
        notifyDataSetChanged()
    }
}