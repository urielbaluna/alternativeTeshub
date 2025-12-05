package com.example.teshub_v1.adapter

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
import java.util.TimeZone

class EventosAdapter(
    private val eventos: List<Evento>,
    private val onItemClick: (Evento) -> Unit
) : RecyclerView.Adapter<EventosAdapter.EventoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_evento, parent, false)
        return EventoViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val evento = eventos[position]
        holder.bind(evento)
        holder.itemView.setOnClickListener { onItemClick(evento) }
    }

    override fun getItemCount(): Int = eventos.size

    class EventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImagen: ImageView = itemView.findViewById(R.id.ivImagenEvento)
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloEvento)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionEvento)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFechaEvento)
        private val tvHora: TextView = itemView.findViewById(R.id.tvHoraEvento)
        private val tvAsistentes: TextView = itemView.findViewById(R.id.tvAsistentesEvento)

        fun bind(evento: Evento) {
            tvTitulo.text = evento.titulo
            tvDescripcion.text = evento.descripcion
            tvAsistentes.text = evento.textoAsistencia()
            
            val (fecha, hora) = formatIsoDateSeparated(evento.fecha)
            tvFecha.text = fecha
            tvHora.text = hora
            
            // Cargar imagen con Glide
            evento.urlFoto?.let {
                val fullImageUrl = if (it.startsWith("http")) it else "${BuildConfig.API_BASE_URL}/$it"
                Glide.with(itemView.context)
                    .load(fullImageUrl)
                    .centerCrop()
                    .placeholder(android.R.color.darker_gray)
                    .into(ivImagen)
            } ?: run {
                ivImagen.setImageResource(android.R.color.darker_gray)
            }
        }

        private fun formatIsoDateSeparated(isoDate: String): Pair<String, String> {
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                val date = parser.parse(isoDate)
                
                val fechaFormatter = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
                val horaFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                
                val fechaStr = date?.let { fechaFormatter.format(it) } ?: isoDate
                val horaStr = date?.let { horaFormatter.format(it) } ?: ""
                
                Pair(fechaStr, horaStr)
            } catch (e: Exception) {
                try {
                    val fallbackParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    fallbackParser.timeZone = TimeZone.getTimeZone("UTC")
                    val date = fallbackParser.parse(isoDate)
                    
                    val fechaFormatter = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
                    val horaFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                    fechaFormatter.timeZone = TimeZone.getDefault()
                    horaFormatter.timeZone = TimeZone.getDefault()
                    
                    val fechaStr = date?.let { fechaFormatter.format(it) } ?: isoDate
                    val horaStr = date?.let { horaFormatter.format(it) } ?: ""
                    
                    Pair(fechaStr, horaStr)
                } catch (e2: Exception) {
                    Pair(isoDate, "")
                }
            }
        }
    }
}
