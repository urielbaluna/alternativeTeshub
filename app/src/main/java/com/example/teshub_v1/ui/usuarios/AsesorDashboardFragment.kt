package com.example.teshub_v1.ui.usuarios

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Publicacion
import com.example.teshub_v1.data.model.ResponderSolicitudRequest
import com.example.teshub_v1.data.model.RevisionRequest
import com.example.teshub_v1.data.model.SolicitudInfo
import com.example.teshub_v1.data.network.RetrofitClient
import com.example.teshub_v1.ui.publicaciones.RevisionesAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import com.example.teshub_v1.ui.publicaciones.PublicacionDetalleActivity

class AsesorDashboardFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private lateinit var adapterSolicitudes: SolicitudesAdapter
    private lateinit var adapterRevisiones: RevisionesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_asesor_dashboard, container, false)

        tabLayout = view.findViewById(R.id.tab_layout_asesor)
        recyclerView = view.findViewById(R.id.rv_asesor)
        progressBar = view.findViewById(R.id.progress_asesor)
        tvEmpty = view.findViewById(R.id.tv_empty_asesor)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // Inicializar adaptadores
        adapterSolicitudes = SolicitudesAdapter(mutableListOf()) { solicitud, accion ->
            responderSolicitud(solicitud, accion)
        }

        adapterRevisiones = RevisionesAdapter(emptyList()) { publicacion ->
            val intent = Intent(context, PublicacionDetalleActivity::class.java)
            intent.putExtra("id_publi", publicacion.id)
            intent.putExtra("modo_asesor", true) // ¡Modo revisión activado!
            startActivity(intent)
        }

        // Listener Tabs
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) cargarSolicitudes()
                else cargarRevisiones()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Cargar inicial
        cargarSolicitudes()

        return view
    }

    private fun getToken(): String? {
        return activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)
            ?.getString("token", null)
    }

    private fun cargarSolicitudes() {
        val token = getToken() ?: return
        showLoading(true)
        recyclerView.adapter = adapterSolicitudes

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.asesoriasService.listarSolicitudesPendientes("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val lista = response.body()!!.solicitudes
                    adapterSolicitudes.updateList(lista)
                    showEmpty(lista.isEmpty())
                }else{
                    val errorBody = response.errorBody()?.string()
                    Log.e("ASESOR_DEBUG", "Error ${response.code()}: $errorBody")
                }
            } catch (e: Exception) {
                Log.e("ASESOR_DEBUG", "Crash de Parsing/Red", e)
                Toast.makeText(context, "Error al cargar solicitudes", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun cargarRevisiones() {
        val token = getToken() ?: return
        showLoading(true)
        recyclerView.adapter = adapterRevisiones

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.revisionesService.obtenerPendientes("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val lista = response.body()!!.pendientes
                    adapterRevisiones.updateList(lista)
                    showEmpty(lista.isEmpty())
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar revisiones", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun responderSolicitud(solicitud: SolicitudInfo, accion: String) {
        val token = getToken() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val body = ResponderSolicitudRequest(solicitud.id, accion)
                val response = RetrofitClient.asesoriasService.responderSolicitud("Bearer $token", body)

                if (response.isSuccessful) {
                    Toast.makeText(context, response.body()?.mensaje, Toast.LENGTH_SHORT).show()
                    adapterSolicitudes.removeItem(solicitud)
                } else {
                    Toast.makeText(context, "Error al responder", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoRevision(pub: Publicacion) {
        val input = EditText(context)
        input.hint = "Comentarios / Retroalimentación"
        input.minLines = 3

        // Contenedor con márgenes
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 0)
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Revisar Tesis")
            .setMessage("Acción para: ${pub.titulo}")
            .setView(container)
            .setPositiveButton("Aprobar") { _, _ -> enviarRevision(pub.id, "aprobado", input.text.toString()) }
            .setNegativeButton("Rechazar") { _, _ -> enviarRevision(pub.id, "rechazado", input.text.toString()) }
            .setNeutralButton("Correcciones") { _, _ -> enviarRevision(pub.id, "correcciones", input.text.toString()) }
            .show()
    }

    private fun enviarRevision(idPubli: Int, estado: String, comentarios: String) {
        val token = getToken() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val body = RevisionRequest(idPubli, estado, comentarios)
                val response = RetrofitClient.revisionesService.revisarPublicacion("Bearer $token", body)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Revisión enviada", Toast.LENGTH_SHORT).show()
                    cargarRevisiones() // Recargar lista
                } else {
                    Toast.makeText(context, "Error al enviar revisión", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
            tvEmpty.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showEmpty(empty: Boolean) {
        tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
    }
}