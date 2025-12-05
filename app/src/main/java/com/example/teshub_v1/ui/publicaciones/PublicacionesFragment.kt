package com.example.teshub_v1.ui.publicaciones

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Publicacion
import com.example.teshub_v1.data.network.RetrofitClient
import com.example.teshub_v1.ui.ComentariosActivity
import kotlinx.coroutines.launch

class PublicacionesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PublicacionesAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private var allPublicaciones: List<Publicacion> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_publicaciones, container, false)

        recyclerView = view.findViewById(R.id.rv_publicaciones)
        progressBar = view.findViewById(R.id.progress_bar)
        tvEmpty = view.findViewById(R.id.tv_empty_view)

        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = PublicacionesAdapter(
            emptyList(),
            onClick = { publicacion ->
                Toast.makeText(context, "Publicación: ${publicacion.nombre}", Toast.LENGTH_SHORT).show()
            },
            onComentariosClick = { publicacion ->
                val intent = Intent(context, ComentariosActivity::class.java)
                intent.putExtra("idPublicacion", publicacion.id) // Pasamos el ID
                startActivity(intent)
            }
        )
        recyclerView.adapter = adapter

        cargarPublicaciones()

        return view
    }

    private fun cargarPublicaciones() {
        val sharedPref = activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val token = sharedPref?.getString("token", null)

        if (token == null) {
            if (isAdded && context != null) {
                Toast.makeText(context, "Error de sesión", Toast.LENGTH_SHORT).show()
            }
            return
        }

        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.publicacionesService.listarPublicaciones("Bearer $token")

                if (!isAdded || context == null) return@launch

                progressBar.visibility = View.GONE

                if (response.publicaciones.isNotEmpty()) {
                    allPublicaciones = response.publicaciones
                    adapter.updateList(allPublicaciones)
                    tvEmpty.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                } else {
                    tvEmpty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
            } catch (e: Exception) {
                if (!isAdded || context == null) return@launch

                progressBar.visibility = View.GONE
                Log.e("PublicacionesFragment", "Error: ${e.message}")
                Toast.makeText(context, "Error al cargar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun filter(query: String) {
        val filteredList = if (query.isEmpty()) {
            allPublicaciones
        } else {
            val lowerCaseQuery = query.lowercase()
            allPublicaciones.filter {
                it.nombre.lowercase().contains(lowerCaseQuery) ||
                        it.descripcion.lowercase().contains(lowerCaseQuery)
            }
        }
        adapter.updateList(filteredList)

        if (filteredList.isEmpty()) {
            tvEmpty.text = if (query.isEmpty()) "No hay publicaciones disponibles" else "No se encontraron resultados"
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}