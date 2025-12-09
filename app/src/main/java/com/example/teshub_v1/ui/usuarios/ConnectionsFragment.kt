package com.example.teshub_v1.ui.usuarios

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

    // Lista local para manipular los datos (borrar items al conectar)
    private var listaUsuarios: MutableList<PerfilResponse> = mutableListOf()

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
        // Inicializamos el adapter con nuestra lista local mutable
        adapter = ConnectionsAdapter(
            listaUsuarios,
            onConnectClick = { user, position ->
                // Pasamos la posición para poder actualizar la lista visualmente
                conectarConUsuario(user, position)
            },
            onItemClick = { user ->
                // Navegar al perfil del usuario
                val intent = Intent(context, PerfilOtroUsuarioActivity::class.java)
                intent.putExtra("matricula", user.matricula)
                startActivity(intent)
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
                    RetrofitClient.usuariosService.obtenerMisConexiones("Bearer $token")
                }

                // Actualizamos la lista local y notificamos al adapter
                listaUsuarios.clear()
                if (usuarios.isNotEmpty()) {
                    listaUsuarios.addAll(usuarios)
                    adapter.notifyDataSetChanged()
                    recyclerView.visibility = View.VISIBLE
                } else {
                    adapter.notifyDataSetChanged() // Limpiar vista si estaba llena
                    tvEmpty.text = if (esSugerencias) "No hay sugerencias por ahora" else "Aún no tienes conexiones"
                    tvEmpty.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                if (isAdded) {
                    Log.e("ConnectionsFragment", "Error al cargar: ${e.message}", e)
                    Toast.makeText(context, "Error al cargar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun conectarConUsuario(usuario: PerfilResponse, position: Int) {
        val token = activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)
            ?.getString("token", null) ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val body = mapOf("matricula_destino" to usuario.matricula)
                val response = RetrofitClient.usuariosService.conectarUsuario("Bearer $token", body)

                Toast.makeText(context, response.mensaje, Toast.LENGTH_SHORT).show()

                // Lógica visual después de conectar/desconectar
                if (tabLayout.selectedTabPosition == 0) {
                    // CASO 1: Estamos en "Sugerencias"
                    // Si conectamos con él, ya no es sugerencia -> Lo quitamos de la lista
                    if (position >= 0 && position < listaUsuarios.size) {
                        listaUsuarios.removeAt(position)
                        adapter.notifyItemRemoved(position)
                        adapter.notifyItemRangeChanged(position, listaUsuarios.size) // Ajustar índices restantes

                        if (listaUsuarios.isEmpty()) {
                            tvEmpty.text = "No hay más sugerencias"
                            tvEmpty.visibility = View.VISIBLE
                        }
                    }
                } else {
                    // CASO 2: Estamos en "Mis Conexiones"
                    // Invertimos el estado (seguir/dejar de seguir) y actualizamos el botón
                    usuario.siguiendo = !usuario.siguiendo
                    adapter.notifyItemChanged(position)
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error al conectar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}