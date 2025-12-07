package com.example.teshub_v1.ui.publicaciones

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Publicacion
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class PublicacionesAdapter(
    private var publicaciones: List<Publicacion>,
    private val onClick: (Publicacion) -> Unit
) : RecyclerView.Adapter<PublicacionesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tv_titulo)
        val tvDescripcion: TextView = view.findViewById(R.id.tv_descripcion)
        val tvAutor: TextView = view.findViewById(R.id.tv_autor)
        val ivPortada: ImageView = view.findViewById(R.id.iv_portada)
        val chipGroup: ChipGroup = view.findViewById(R.id.chip_group_tags)

        val tvRating: TextView = view.findViewById(R.id.tv_rating)
        val tvDownloads: TextView = view.findViewById(R.id.tv_downloads)
        val tvViews: TextView = view.findViewById(R.id.tv_views)
        val btnVer: MaterialButton = view.findViewById(R.id.btn_ver)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_publicacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val publicacion = publicaciones[position]

        holder.tvTitulo.text = publicacion.titulo
        holder.tvDescripcion.text = publicacion.descripcion

        val tiempo = publicacion.haceCuanto ?: ""
        holder.tvAutor.text = "${publicacion.autor ?: "Anónimo"} • $tiempo"

        holder.tvRating.text = publicacion.rating ?: "0.0"
        holder.tvDownloads.text = publicacion.downloads.toString()
        holder.tvViews.text = publicacion.views.toString()

        // --- LÓGICA DE IMAGEN LIMPIA Y SIN LISTENER ---
        if (!publicacion.imagenPortada.isNullOrEmpty()) {

            var baseUrl = BuildConfig.API_BASE_URL
            // Limpieza de URL
            if (baseUrl.endsWith("/api")) baseUrl = baseUrl.replace("/api", "")
            if (baseUrl.endsWith("/")) baseUrl = baseUrl.dropLast(1)

            var rutaLimpia = publicacion.imagenPortada.replace("\\", "/")
            if (rutaLimpia.startsWith("/")) rutaLimpia = rutaLimpia.substring(1)

            val fullUrl = "$baseUrl/$rutaLimpia"

            // 1. Resetear el ImageView (CRUCIAL en RecyclerView)
            holder.ivPortada.setPadding(0, 0, 0, 0)
            holder.ivPortada.colorFilter = null

            // 2. Cargar la imagen
            Glide.with(holder.itemView.context)
                .load(fullUrl)
                // ... (listener) ...
                .placeholder(R.drawable.ic_pdf)
                .error(R.drawable.ic_close)
                .centerCrop()
                .into(holder.ivPortada)

        } else {
            // Lógica de Placeholder
            holder.ivPortada.setImageResource(R.drawable.ic_pdf)
            val padding = (20 * holder.itemView.context.resources.displayMetrics.density).toInt()
            // 3. Usamos la función de Java con los 4 parámetros
            holder.ivPortada.setPadding(padding, padding, padding, padding)
            holder.ivPortada.setColorFilter(android.graphics.Color.parseColor("#757575"))
        }

        // Tags
        holder.chipGroup.removeAllViews()
        publicacion.tags?.take(3)?.forEach { tag ->
            val chip = Chip(holder.itemView.context)
            chip.text = tag
            chip.isClickable = false
            chip.setChipBackgroundColorResource(android.R.color.transparent)
            chip.chipStrokeWidth = 1f
            chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0"))
            chip.minHeight = 0
            chip.textSize = 10f
            holder.chipGroup.addView(chip)
        }

        holder.btnVer.setOnClickListener { onClick(publicacion) }
        holder.itemView.setOnClickListener { onClick(publicacion) }
    }

    override fun getItemCount() = publicaciones.size

    fun updateList(newList: List<Publicacion>) {
        publicaciones = newList
        notifyDataSetChanged()
    }
}