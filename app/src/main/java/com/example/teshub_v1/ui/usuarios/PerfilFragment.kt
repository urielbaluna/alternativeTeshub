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
import android.widget.LinearLayout
import android.widget.PopupMenu
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
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class PerfilFragment : Fragment() {

    private lateinit var ivProfileAvatar: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserMatricula: TextView
    private lateinit var tvTotalPublications: TextView
    private lateinit var tvFeaturedPublicationTitle: TextView
    private lateinit var layoutFeaturedPublication: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterPerfil: PerfilPublicacionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        ivProfileAvatar = view.findViewById(R.id.iv_profile_avatar)
        tvUserName = view.findViewById(R.id.tv_user_name)
        tvUserRole = view.findViewById(R.id.tv_user_role)
        tvUserEmail = view.findViewById(R.id.tv_user_email)
        tvUserMatricula = view.findViewById(R.id.tv_user_matricula)
        tvTotalPublications = view.findViewById(R.id.tv_total_publications)
        tvFeaturedPublicationTitle = view.findViewById(R.id.tv_featured_publication_title)
        layoutFeaturedPublication = view.findViewById(R.id.layout_destacada)
        recyclerView = view.findViewById(R.id.rv_publicaciones_usuario)

        recyclerView.layoutManager = LinearLayoutManager(context)

        adapterPerfil = PerfilPublicacionAdapter(
            mutableListOf(),
            onItemClick = { pub -> Log.d("PERFIL", "Click en: ${pub.proyecto_nombre}") },
            onDeleteClick = { pub -> confirmDelete(pub) }
        )
        recyclerView.adapter = adapterPerfil

        view.findViewById<ImageView>(R.id.btn_logout).setOnClickListener { logout() }

        view.findViewById<ImageView>(R.id.iv_edit_name).setOnClickListener {
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

        loadUserProfile()

        return view
    }

    private fun loadUserProfile() {
        val sharedPref = activity?.getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val token = sharedPref?.getString("token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "Sesión expirada", Toast.LENGTH_LONG).show()
            logout()
            return
        }

        loadUserPublications(token)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val perfil = RetrofitClient.usuariosService.getPerfil("Bearer $token")

                if (!isAdded || context == null) return@launch

                tvUserName.text = "${perfil.nombre} ${perfil.apellido}"
                tvUserRole.text = "Rol: ${perfil.rol}"
                tvUserEmail.text = perfil.correo
                tvUserMatricula.text = "Matrícula: ${perfil.matricula}"
                tvTotalPublications.text = perfil.totalPublicaciones.toString()

                if (!perfil.imagen.isNullOrEmpty()) {
                    val baseUrl = BuildConfig.API_BASE_URL
                    val fullUrl = if (baseUrl.endsWith("/")) baseUrl + perfil.imagen else "$baseUrl/${perfil.imagen}"

                    Glide.with(this@PerfilFragment)
                        .load(fullUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(ivProfileAvatar)
                } else {
                    ivProfileAvatar.setImageResource(R.drawable.ic_profile)
                }

                if (!perfil.publicacionDestacada.isNullOrEmpty()) {
                    tvFeaturedPublicationTitle.text = perfil.publicacionDestacada
                    layoutFeaturedPublication.visibility = View.VISIBLE
                } else {
                    layoutFeaturedPublication.visibility = View.GONE
                }

            } catch (e: HttpException) {
                if (!isAdded || context == null) return@launch
                val errorMsg = try {
                    JSONObject(e.response()?.errorBody()?.string()).optString("mensaje", "Error al cargar")
                } catch (ex: Exception) { "Error ${e.code()}" }

                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                if (e.code() == 401) logout()
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(context, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserPublications(token: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.publicacionesService.obtenerSoloPublicaciones("Bearer $token")
                if (isAdded && context != null) {
                    adapterPerfil.updateList(response.publicaciones)
                }
            } catch (e: Exception) {
                Log.e("PERFIL", "Error publicaciones: ${e.message}")
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