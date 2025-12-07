package com.example.teshub_v1.ui.usuarios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.PerfilResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ConnectionsAdapter(
    private var usuarios: MutableList<PerfilResponse>,
    private val onConnectClick: (PerfilResponse) -> Unit,
    private val onItemClick: (PerfilResponse) -> Unit
) : RecyclerView.Adapter<ConnectionsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_avatar)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvCarrera: TextView = view.findViewById(R.id.tv_carrera)
        val chipGroup: ChipGroup = view.findViewById(R.id.chip_group_intereses)
        val btnAccion: MaterialButton = view.findViewById(R.id.btn_accion)

        init {
            btnAccion.setOnClickListener {
                onConnectClick(usuarios[adapterPosition])
            }
            itemView.setOnClickListener {
                onItemClick(usuarios[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario_conexion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = usuarios[position]

        holder.tvNombre.text = "${user.nombre} ${user.apellido}"
        holder.tvCarrera.text = "${user.carrera ?: "Sin carrera"} • ${user.rol}"

        // Cargar imagen
        if (!user.imagen.isNullOrEmpty()) {
            val fullUrl = if (user.imagen.startsWith("http")) user.imagen
            else "${BuildConfig.API_BASE_URL}/${user.imagen}"
            Glide.with(holder.itemView.context)
                .load(fullUrl)
                .placeholder(R.drawable.ic_profile)
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_profile)
        }

        // Cargar Intereses (Chips)
        holder.chipGroup.removeAllViews()
        user.intereses?.take(3)?.forEach { interes -> // Mostrar solo los primeros 3
            val chip = Chip(holder.itemView.context)
            chip.text = interes.nombre
            chip.isClickable = false
            chip.setEnsureMinTouchTargetSize(false)
            chip.chipMinHeight = 32f // Hacerlos pequeños
            holder.chipGroup.addView(chip)
        }

        // Configurar botón (puedes cambiar lógica si ya están conectados)
        holder.btnAccion.text = "Conectar"
        holder.btnAccion.isEnabled = true
    }

    override fun getItemCount() = usuarios.size

    fun updateList(newList: List<PerfilResponse>) {
        usuarios.clear()
        usuarios.addAll(newList)
        notifyDataSetChanged()
    }
}