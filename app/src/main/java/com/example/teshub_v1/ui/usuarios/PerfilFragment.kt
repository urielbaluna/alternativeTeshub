package com.example.teshub_v1.ui.usuarios

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.PublicacionInfo
import com.example.teshub_v1.data.network.RetrofitClient
import com.example.teshub_v1.ui.auth.MainActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import android.widget.EditText
import android.widget.*
import org.json.JSONObject

class PerfilFragment : Fragment() {

    private lateinit var ivProfileAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserCareer: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserMatricula: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvLocation: TextView

    // Estadísticas
    private lateinit var tvStatPubs: TextView
    private lateinit var tvStatFollowers: TextView
    private lateinit var tvStatFollowing: TextView

    // Intereses
    private lateinit var chipGroupInterests: ChipGroup

    // Lista
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterPerfil: PerfilPublicacionAdapter
    private lateinit var layoutSinAsesor: LinearLayout
    private lateinit var layoutConAsesor: LinearLayout
    private lateinit var tvAsesorNombre: TextView
    private lateinit var tvAsesorEstado: TextView
    private lateinit var ivAsesorAvatar: ImageView
    private lateinit var btnSolicitarAsesor: Button
    private lateinit var cardAsesor: View // CardView completa

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        // Bind Views
        ivProfileAvatar = view.findViewById(R.id.iv_profile_avatar)
        tvUserName = view.findViewById(R.id.tv_user_name)
        tvUserCareer = view.findViewById(R.id.tv_user_career)
        tvUserEmail = view.findViewById(R.id.tv_user_email)
        tvUserMatricula = view.findViewById(R.id.tv_user_matricula)

        tvBio = view.findViewById(R.id.tv_biography)
        tvLocation = view.findViewById(R.id.tv_location)

        tvStatPubs = view.findViewById(R.id.tv_stat_publications)
        tvStatFollowers = view.findViewById(R.id.tv_stat_followers)
        tvStatFollowing = view.findViewById(R.id.tv_stat_following)

        chipGroupInterests = view.findViewById(R.id.chip_group_interests)
        recyclerView = view.findViewById(R.id.rv_publicaciones_usuario)

        // Setup Recycler
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapterPerfil = PerfilPublicacionAdapter(
            mutableListOf(),
            onItemClick = { pub -> Log.d("PERFIL", "Click en: ${pub.proyecto_nombre}") },
            onDeleteClick = { pub -> confirmDelete(pub) }
        )
        recyclerView.adapter = adapterPerfil

        // Listeners
        view.findViewById<ImageView>(R.id.btn_logout).setOnClickListener { logout() }
        view.findViewById<ImageView>(R.id.iv_edit_profile).setOnClickListener {
            startActivity(Intent(context, ActualizarUsuarioActivity::class.java))
        }
        val btnConfig = view.findViewById<ImageView>(R.id.btn_settings)
        btnConfig.setOnClickListener {
            val popup = PopupMenu(requireContext(), btnConfig)
            popup.menu.add("Eliminar cuenta")
            popup.setOnMenuItemClickListener { item ->
                if (item.title == "Eliminar cuenta") mostrarDialogoEliminar()
                true
            }
            popup.show()
        }

        layoutSinAsesor = view.findViewById(R.id.layout_sin_asesor)
        layoutConAsesor = view.findViewById(R.id.layout_con_asesor)
        tvAsesorNombre = view.findViewById(R.id.tv_asesor_nombre)
        tvAsesorEstado = view.findViewById(R.id.tv_asesor_estado)
        ivAsesorAvatar = view.findViewById(R.id.iv_asesor_avatar)
        btnSolicitarAsesor = view.findViewById(R.id.btn_solicitar_asesor)
        cardAsesor = view.findViewById(R.id.card_asesor)

        // Listener para solicitar
        btnSolicitarAsesor.setOnClickListener { mostrarDialogoSolicitud() }

        loadUserProfile()

        return view
    }

    private fun loadUserProfile() {
        val sharedPref = activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val token = sharedPref?.getString("token", null)

        if (token.isNullOrEmpty()) {
            logout()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Usamos el endpoint que actualizamos en el backend que trae TODO
                val perfil = RetrofitClient.usuariosService.getPerfil("Bearer $token")

                if (!isAdded || context == null) return@launch

                // 1. Datos Básicos
                tvUserName.text = "${perfil.nombre} ${perfil.apellido}"
                tvUserEmail.text = perfil.correo
                tvUserMatricula.text = perfil.matricula

                // 2. Datos Extendidos (Carrera, Bio, Ubicación)
                val carrera = perfil.carrera ?: "Carrera no especificada"
                val semestre = perfil.semestre ?: "?"
                tvUserCareer.text = "$carrera • $semestre semestre"

                tvBio.text = if (!perfil.biografia.isNullOrEmpty()) perfil.biografia else "Sin biografía disponible."
                tvLocation.text = if (!perfil.ubicacion.isNullOrEmpty()) perfil.ubicacion else "Ubicación no especificada"

                // 3. Estadísticas
                tvStatPubs.text = perfil.totalPublicaciones.toString()
                tvStatFollowers.text = (perfil.estadisticas?.seguidores ?: 0).toString()
                tvStatFollowing.text = (perfil.estadisticas?.seguidos ?: 0).toString()

                // 4. Imagen
                if (!perfil.imagen.isNullOrEmpty()) {
                    val baseUrl = BuildConfig.API_BASE_URL
                    val fullUrl = if (baseUrl.endsWith("/")) baseUrl + perfil.imagen else "$baseUrl${perfil.imagen}"
                    Glide.with(this@PerfilFragment)
                        .load(fullUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(ivProfileAvatar)
                }

                // 5. Intereses (Dinámicos)
                chipGroupInterests.removeAllViews()
                perfil.intereses?.forEach { interes ->
                    val chip = Chip(context)
                    chip.text = interes.nombre
                    chip.setChipBackgroundColorResource(R.color.white) // O un color verde bajito
                    chip.setChipStrokeColorResource(android.R.color.darker_gray)
                    chip.chipStrokeWidth = 1f
                    chipGroupInterests.addView(chip)
                }

                if (perfil.rol.equals("Estudiante", ignoreCase = true) || perfil.rol.contains("Estudiante")) {
                    cardAsesor.visibility = View.VISIBLE
                    cargarMiAsesor(token)
                } else {
                    cardAsesor.visibility = View.GONE // Los profesores no tienen asesor
                }

                // 6. Cargar Publicaciones (Si la respuesta del perfil ya las incluye o llamar aparte)
                // Tu backend actual "getPerfil" no devuelve la lista de publicaciones, solo contadores.
                // Así que llamamos a la otra función.
                loadUserPublications(token)

            } catch (e: Exception) {
                if (isAdded) Toast.makeText(context, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
                Log.e("PERFIL", "Error", e)
            }
        }
    }
    private suspend fun cargarMiAsesor(token: String) {
        try {
            val response = RetrofitClient.asesoriasService.obtenerMiAsesor("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!

                if (data.asesor != null) {
                    // TIENE ASESOR (Pendiente o Activo)
                    layoutSinAsesor.visibility = View.GONE
                    layoutConAsesor.visibility = View.VISIBLE

                    tvAsesorNombre.text = "${data.asesor.nombre} ${data.asesor.apellido}"
                    tvAsesorEstado.text = "Estado: ${data.estado}"

                    // Color del estado
                    if (data.estado == "Activo") {
                        tvAsesorEstado.setTextColor(resources.getColor(R.color.green_500, null))
                    } else {
                        tvAsesorEstado.setTextColor(resources.getColor(R.color.accentYellow, null))
                    }

                    // Foto Asesor
                    if (!data.asesor.imagen.isNullOrEmpty()) {
                        val fullUrl = "${BuildConfig.API_BASE_URL}${data.asesor.imagen}"
                        Glide.with(this).load(fullUrl).into(ivAsesorAvatar)
                    }
                } else {
                    // NO TIENE ASESOR
                    layoutSinAsesor.visibility = View.VISIBLE
                    layoutConAsesor.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.e("ASESOR", "Error cargando asesor", e)
        }
    }
    private fun mostrarDialogoSolicitud() {
        val input = EditText(context)
        input.hint = "Matrícula del Profesor (ej: 99001)"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 0) // Márgenes
        input.layoutParams = params
        container.addView(input)

        AlertDialog.Builder(requireContext())
            .setTitle("Solicitar Asesoría")
            .setMessage("Ingresa la matrícula del docente que revisará tus proyectos.")
            .setView(container)
            .setPositiveButton("Enviar Solicitud") { _, _ ->
                val matricula = input.text.toString().trim()
                if (matricula.isNotEmpty()) {
                    enviarSolicitud(matricula)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarSolicitud(matriculaProf: String) {
        val token = activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)?.getString("token", null) ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val body = mapOf("matricula_asesor" to matriculaProf)
                val response = RetrofitClient.asesoriasService.solicitarAsesoria("Bearer $token", body)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Solicitud enviada correctamente", Toast.LENGTH_LONG).show()
                    cargarMiAsesor(token) // Recargar para ver el estado "Pendiente"
                } else {
                    val errMsg = JSONObject(response.errorBody()?.string()).optString("mensaje", "Error al solicitar")
                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadUserPublications(token: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.publicacionesService.obtenerSoloPublicaciones("Bearer $token")

                if (isAdded) {
                    adapterPerfil.updateList(response.publicaciones ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e("PERFIL", "Error pubs", e)
            }
        }
    }
    private fun eliminarCuenta() {
        val token = activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)?.getString("token", null) ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.usuariosService.eliminarCuenta("Bearer $token", emptyMap())

                Toast.makeText(context, "Cuenta eliminada", Toast.LENGTH_LONG).show()
                logout()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(publicacion: PublicacionInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar publicación")
            .setMessage("¿Eliminar \"${publicacion.proyecto_nombre}\"?")
            .setPositiveButton("Sí") { _, _ -> eliminarPublicacion(publicacion) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun mostrarDialogoEliminar() {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar cuenta")
            .setMessage("¿Seguro que deseas eliminar tu cuenta? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> eliminarCuenta() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarPublicacion(publicacion: PublicacionInfo) {
        val token = activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)?.getString("token", null) ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.publicacionesService.eliminarPublicacion(publicacion.id_publi, "Bearer $token")

                if (response.isSuccessful) {
                    adapterPerfil.removeItem(publicacion)
                    Toast.makeText(context, "Eliminado correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)?.edit()?.remove("token")?.apply()
        val intent = Intent(activity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}