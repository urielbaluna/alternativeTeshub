package com.example.teshub_v1.ui.eventos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teshub_v1.R
import com.example.teshub_v1.adapter.EventosAdapter
import com.example.teshub_v1.data.network.RetrofitClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class EventosFragment : Fragment() {

    private lateinit var rvEventos: RecyclerView
    private lateinit var eventosAdapter: EventosAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_eventos, container, false)
        rvEventos = view.findViewById(R.id.rvEventos)
        progressBar = view.findViewById(R.id.progressBarEventos)

        val fabAddEvent: FloatingActionButton = view.findViewById(R.id.fab_add_event)
        fabAddEvent.setOnClickListener {
            val intent = Intent(activity, CrearEventoActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        cargarEventosDesdeAPI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        eventosAdapter = EventosAdapter(emptyList()) { evento ->
            val intent = Intent(activity, EventoDetalleActivity::class.java)
            intent.putExtra("EVENTO_EXTRA", evento)
            startActivity(intent)
        }
        rvEventos.adapter = eventosAdapter
        rvEventos.layoutManager = LinearLayoutManager(context)
    }

    private fun cargarEventosDesdeAPI() {
        progressBar.visibility = View.VISIBLE

        val sharedPref = activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val token = sharedPref?.getString("token", null)

        if (token == null) {
            progressBar.visibility = View.GONE
            if (isAdded && context != null) {
                Toast.makeText(context, "Error de sesión. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
            }
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.eventosService.getEventos("Bearer $token")
                
                // Verificar que el fragment sigue adjunto antes de actualizar la UI
                if (!isAdded || context == null) return@launch
                
                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    // --- CORRECCIÓN: Extraer la lista del objeto de respuesta ---
                    val eventos = response.body()?.eventos ?: emptyList()
                    eventosAdapter = EventosAdapter(eventos) { evento ->
                        val intent = Intent(activity, EventoDetalleActivity::class.java)
                        intent.putExtra("EVENTO_EXTRA", evento)
                        startActivity(intent)
                    }
                    rvEventos.adapter = eventosAdapter
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("EventosFragment", "Error al cargar eventos: $errorBody")
                    Toast.makeText(context, "Error al cargar eventos: $errorBody", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Verificar que el fragment sigue adjunto antes de mostrar el error
                if (!isAdded || context == null) return@launch
                
                progressBar.visibility = View.GONE
                Log.e("EventosFragment", "Excepción: ${e.message}")
                Toast.makeText(context, "Excepción: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
