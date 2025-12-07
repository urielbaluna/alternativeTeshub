package com.example.teshub_v1.ui.usuarios

import android.content.Context
import android.os.Bundle
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
import com.example.teshub_v1.data.model.PerfilResponse
import com.example.teshub_v1.data.network.RetrofitClient
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class ConnectionsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: ConnectionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connections, container, false)

        recyclerView = view.findViewById(R.id.rv_connections)
        progressBar = view.findViewById(R.id.progress_bar)
        tvEmpty = view.findViewById(R.id.tv_empty_state)
        tabLayout = view.findViewById(R.id.tab_layout)

        setupRecyclerView()
        setupTabs()

        // Cargar sugerencias por defecto
        cargarDatos(esSugerencias = true)

        return view
    }

    private fun setupRecyclerView() {
        adapter = ConnectionsAdapter(
            mutableListOf(),
            onConnectClick = { user -> conectarConUsuario(user) },
            onItemClick = { user ->
                // Aquí podrías abrir un "PerfilAjenoActivity" para ver su perfil completo
                Toast.makeText(context, "Ver perfil de ${user.nombre}", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> cargarDatos(esSugerencias = true)
                    1 -> cargarDatos(esSugerencias = false)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun cargarDatos(esSugerencias: Boolean) {
        val token = activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)
            ?.getString("token", null) ?: return

        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvEmpty.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val usuarios = if (esSugerencias) {
                    RetrofitClient.usuariosService.obtenerSugerencias("Bearer $token")
                } else {
                    // Si aún no implementas el endpoint "obtenerConexiones" en backend,
                    // puedes devolver lista vacía o implementar esa función rápido.
                    // RetrofitClient.usuariosService.obtenerMisConexiones("Bearer $token")
                    emptyList() // Placeholder hasta que tengas el endpoint
                }

                if (usuarios.isEmpty()) {
                    tvEmpty.text = if (esSugerencias) "No hay sugerencias por ahora" else "Aún no tienes conexiones"
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    adapter.updateList(usuarios)
                    recyclerView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun conectarConUsuario(usuario: PerfilResponse) {
        val token = activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)
            ?.getString("token", null) ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val body = mapOf("matricula_destino" to usuario.matricula)
                val response = RetrofitClient.usuariosService.conectarUsuario("Bearer $token", body)

                Toast.makeText(context, response.mensaje, Toast.LENGTH_SHORT).show()

                // Recargar lista para reflejar cambios (quitar de sugerencias)
                cargarDatos(esSugerencias = tabLayout.selectedTabPosition == 0)

            } catch (e: Exception) {
                Toast.makeText(context, "Error al conectar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}