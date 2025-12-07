package com.example.teshub_v1.ui.eventos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.teshub_v1.ui.eventos.EventosAdapter
import com.example.teshub_v1.data.network.RetrofitClient
import com.example.teshub_v1.databinding.FragmentEventosBinding
import kotlinx.coroutines.launch

class EventosFragment : Fragment() {

    private var _binding: FragmentEventosBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: EventosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar RecyclerView
        binding.rvEventos.layoutManager = LinearLayoutManager(context)
        adapter = EventosAdapter(emptyList()) { evento ->
            // Al hacer click, ir al detalle
            val intent = Intent(context, EventoDetalleActivity::class.java)
            intent.putExtra("evento_id", evento.id)
            startActivity(intent)
        }
        binding.rvEventos.adapter = adapter

        // CORRECCIÓN: Usar el ID correcto (fab_add_event -> fabAddEvent)
        binding.fabAddEvent.setOnClickListener {
            val intent = Intent(context, CrearEventoActivity::class.java)
            startActivity(intent)
        }

        cargarEventos()
    }

    override fun onResume() {
        super.onResume()
        cargarEventos()
    }

    private fun cargarEventos() {
        val token = activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)
            ?.getString("token", null)

        if (token == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // CORRECCIÓN: Usar el ID correcto (progressBarEventos)
                binding.progressBarEventos.visibility = View.VISIBLE

                val response = RetrofitClient.eventosService.getEventos("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val lista = response.body()!!.eventos

                    if (lista.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE // Ahora sí existe
                        binding.rvEventos.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvEventos.visibility = View.VISIBLE
                        adapter.updateList(lista)
                    }
                } else {
                    Toast.makeText(context, "Error al cargar eventos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBarEventos.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}