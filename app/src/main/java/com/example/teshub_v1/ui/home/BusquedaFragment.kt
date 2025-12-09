package com.example.teshub_v1.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Interes
import com.example.teshub_v1.data.model.PerfilResponse
import com.example.teshub_v1.data.model.Publicacion
import com.example.teshub_v1.data.network.RetrofitClient
import com.example.teshub_v1.ui.eventos.EventosAdapter
import com.example.teshub_v1.ui.eventos.EventoDetalleActivity
import com.example.teshub_v1.ui.publicaciones.PublicacionDetalleActivity
import com.example.teshub_v1.ui.publicaciones.PublicacionesAdapter
import com.example.teshub_v1.ui.usuarios.ConnectionsAdapter // <--- AHORA SÍ LO USAMOS
import com.example.teshub_v1.ui.usuarios.PerfilOtroUsuarioActivity
import kotlinx.coroutines.launch
import com.example.teshub_v1.data.model.Evento
import com.example.teshub_v1.data.model.Ubicacion


class BusquedaFragment : Fragment() {

    private lateinit var rvUsuarios: RecyclerView
    private lateinit var rvPublicaciones: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var tvTitleUsuarios: TextView
    private lateinit var tvTitlePublicaciones: TextView

    private lateinit var publicacionesAdapter: PublicacionesAdapter
    private lateinit var usuariosAdapter: ConnectionsAdapter
    private lateinit var rvEventos: RecyclerView
    private lateinit var tvTitleEventos: TextView
    private lateinit var eventosAdapter: EventosAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_busqueda, container, false)

        // Vincular vistas
        rvUsuarios = view.findViewById(R.id.rv_resultados_usuarios)
        rvPublicaciones = view.findViewById(R.id.rv_resultados_publicaciones)
        rvEventos = view.findViewById(R.id.rv_resultados_eventos)
        tvNoResults = view.findViewById(R.id.tv_no_results)
        tvTitleUsuarios = view.findViewById(R.id.tv_title_usuarios)
        tvTitlePublicaciones = view.findViewById(R.id.tv_title_publicaciones)
        tvTitleEventos = view.findViewById(R.id.tv_title_eventos)

        // Configurar RecyclerViews
        rvUsuarios.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvPublicaciones.layoutManager = LinearLayoutManager(context)
        rvEventos.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // 1. Inicializar Adapter de Publicaciones
        publicacionesAdapter = PublicacionesAdapter(mutableListOf()) { publicacion ->
            val intent = Intent(context, PublicacionDetalleActivity::class.java)
            intent.putExtra("id_publi", publicacion.id)
            startActivity(intent)
        }
        rvPublicaciones.adapter = publicacionesAdapter

        // 2. Inicializar Adapter de Usuarios (Reutilizando ConnectionsAdapter)
        usuariosAdapter = ConnectionsAdapter(
            mutableListOf(),
            onConnectClick = { perfil, position ->
                toggleSeguirUsuario(perfil, position)
            },
            onItemClick = { perfil ->
                val intent = Intent(context, PerfilOtroUsuarioActivity::class.java)
                intent.putExtra("matricula", perfil.matricula)
                startActivity(intent)
            }
        )
        rvUsuarios.adapter = usuariosAdapter

        eventosAdapter = EventosAdapter(mutableListOf()) { evento ->
            val intent = Intent(context, EventoDetalleActivity::class.java)
            // Asegúrate de pasar el ID o el objeto que tu Activity espera
            intent.putExtra("id_evento", evento.id)
            startActivity(intent)
        }
        rvEventos.adapter = eventosAdapter

        // Ejecutar búsqueda si hay argumentos
        val query = arguments?.getString(ARG_QUERY) ?: ""
        if (query.isNotEmpty()) {
            realizarBusqueda(query)
        }

        return view
    }

    private fun realizarBusqueda(palabra: String) {
        val token = "Bearer ${requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "")}"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Llamada al endpoint
                val respuesta = RetrofitClient.busquedaService.buscarGeneral(token, palabra)

                // --- PROCESAR USUARIOS ---
                if (respuesta.perfiles.isNotEmpty()) {
                    tvTitleUsuarios.visibility = View.VISIBLE
                    rvUsuarios.visibility = View.VISIBLE

                    // CONVERSIÓN: De PerfilBusquedaItem -> PerfilResponse
                    // Esto es necesario porque tu adaptador usa PerfilResponse
                    val listaUsuariosMapeada = respuesta.perfiles.map { item ->
                        PerfilResponse(
                            matricula = item.matricula,
                            nombre = item.nombre,
                            apellido = item.apellido,
                            correo = "", // No viene en búsqueda, no es crítico para la vista
                            rol = item.rol,
                            imagen = item.imagen,
                            carrera = item.carrera,
                            semestre = item.semestre,
                            biografia = null,
                            ubicacion = null,
                            estado = 1,
                            intereses = item.intereses.map { nombreInteres ->
                                Interes(id = 0, nombre = nombreInteres)
                            },
                            estadisticas = null,
                            totalPublicaciones = 0,
                            publicacionDestacada = null,
                            siguiendo = item.siguiendo
                        )
                    }
                    usuariosAdapter.updateList(listaUsuariosMapeada)
                } else {
                    tvTitleUsuarios.visibility = View.GONE
                    rvUsuarios.visibility = View.GONE
                }

                if (respuesta.eventos.isNotEmpty()) {
                    tvTitleEventos.visibility = View.VISIBLE
                    rvEventos.visibility = View.VISIBLE

                    val listaEventos = respuesta.eventos.map { item ->
                        val latDouble = item.ubicacion.lat?.toDoubleOrNull()
                        val lngDouble = item.ubicacion.lng?.toDoubleOrNull()
                        val ubicacionLocal = if (latDouble != null && lngDouble != null) {
                            Ubicacion(latDouble, lngDouble)
                        } else {
                            null
                        }
                        Log.e("item:", item.toString())

                        Evento(
                            id = item.id,
                            titulo = item.titulo,
                            fecha = item.fecha,
                            descripcion = item.descripcion,

                            // Mapeo de imagen
                            urlFoto = item.imagen,

                            categoria = item.categoria,

                            // Mapeo de ubicación
                            ubicacionObj = ubicacionLocal,
                            ubicacionNombre = item.ubicacion.nombre,

                            tags = emptyList(), // La búsqueda no devuelve tags, ponemos vacío

                            // IMPORTANTE: La búsqueda ligera no trae organizadores,
                            // pasamos lista vacía para cumplir con el modelo.
                            organizadores = emptyList(),

                            // Cupos y registros
                            cupoMaximo = item.cupo.maximo,
                            asistentesRegistrados = item.cupo.registrados,
                            usuarioRegistrado = item.inscrito,
                            inscrito = item.inscrito,

                            // Banderas por defecto
                            esOrganizador = false,
                            esAsistente = item.inscrito
                        )
                    }
                    eventosAdapter = EventosAdapter(mutableListOf()) { evento ->
                        val intent = Intent(context, EventoDetalleActivity::class.java);
                        intent.putExtra("evento_id", evento.id)
                        startActivity(intent)
                    }
                    rvEventos.adapter = eventosAdapter
                    eventosAdapter.updateList(listaEventos)
                } else {
                    tvTitleEventos.visibility = View.GONE
                    rvEventos.visibility = View.GONE
                }

                // --- PROCESAR PUBLICACIONES ---
                if (respuesta.publicaciones.isNotEmpty()) {
                    tvTitlePublicaciones.visibility = View.VISIBLE
                    rvPublicaciones.visibility = View.VISIBLE

                    val listaPubsMapeada = respuesta.publicaciones.map { item ->
                        Publicacion(
                            id = item.idPubli,
                            titulo = item.nombre,
                            descripcion = item.descripcion,
                            autor = item.autor,
                            fecha = item.fecha,
                            imagenPortada = item.imagenPortada,
                            tipo = "PDF",
                            haceCuanto = item.haceCuanto,

                            rating = item.rating,
                            downloads = 0,
                            views = item.vistas,
                            tags = item.tags
                        )
                    }
                    publicacionesAdapter.updateList(listaPubsMapeada)
                } else {
                    tvTitlePublicaciones.visibility = View.GONE
                    rvPublicaciones.visibility = View.GONE
                }

                // Estado Vacío
                tvNoResults.isVisible = respuesta.perfiles.isEmpty() &&
                        respuesta.publicaciones.isEmpty() &&
                        respuesta.eventos.isEmpty()

            } catch (e: Exception) {
                Log.e("BusquedaFragment", "Error en la búsqueda", e)
                if(isAdded) Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función auxiliar para conectar desde la búsqueda
    private fun conectarUsuario(matriculaDestino: String) {
        val token = "Bearer ${requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "")}"
        val body = mapOf("matricula_destino" to matriculaDestino)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.usuariosService.conectarUsuario(token, body)
                Toast.makeText(context, resp.mensaje, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al conectar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val ARG_QUERY = "query"
        fun newInstance(query: String) = BusquedaFragment().apply {
            arguments = Bundle().apply { putString(ARG_QUERY, query) }
        }
    }
    private fun toggleSeguirUsuario(perfil: PerfilResponse, position: Int) {
        val token = "Bearer ${
            requireContext().getSharedPreferences("sesion", Context.MODE_PRIVATE)
                .getString("token", "")
        }"
        val body = mapOf("matricula_destino" to perfil.matricula)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Llamada a la API
                val resp = RetrofitClient.usuariosService.conectarUsuario(token, body)

                // 2. Si es exitoso, invertimos el estado localmente
                perfil.siguiendo = !perfil.siguiendo

                // 3. Notificamos al adaptador que ese item cambió para que se repinte el botón
                usuariosAdapter.notifyItemChanged(position)

                Toast.makeText(context, resp.mensaje, Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Error al conectar: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}