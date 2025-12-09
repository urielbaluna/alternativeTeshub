package com.example.teshub_v1.ui.usuarios

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Publicacion
import com.example.teshub_v1.data.network.RetrofitClient
import com.example.teshub_v1.ui.publicaciones.PublicacionDetalleActivity
import com.example.teshub_v1.ui.publicaciones.PublicacionesAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

class PerfilOtroUsuarioActivity : AppCompatActivity() {

    private lateinit var matriculaTarget: String
    private lateinit var adapter: PublicacionesAdapter
    private lateinit var btnConectar: MaterialButton
    private var isFollowing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_otro_usuario)

        // 1. Obtener la matrícula que nos manda el buscador
        matriculaTarget = intent.getStringExtra("matricula") ?: ""
        if (matriculaTarget.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Configurar Toolbar (Flecha atrás)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // 3. Configurar Lista de Publicaciones
        val rv = findViewById<RecyclerView>(R.id.rv_perfil_publicaciones)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = PublicacionesAdapter(mutableListOf()) { pub ->
            // Al hacer clic en una publicación, ir al detalle
            val intent = Intent(this, PublicacionDetalleActivity::class.java)
            intent.putExtra("id_publi", pub.id)
            startActivity(intent)
        }
        rv.adapter = adapter

        // 4. Configurar Botón Conectar
        findViewById<MaterialButton>(R.id.btn_conectar).setOnClickListener {
            conectarUsuario()
        }

        btnConectar = findViewById(R.id.btn_conectar)
        btnConectar.setOnClickListener {
            alternarConexion() // Cambiamos a función toggle
        }

        // 5. Cargar datos del backend
        cargarDatosUsuario()
    }

    private fun cargarDatosUsuario() {
        val token = "Bearer ${getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "")}"

        lifecycleScope.launch {
            try {
                // LLAMADA AL BACKEND
                val body = mapOf("matricula" to matriculaTarget)
                val response = RetrofitClient.usuariosService.verPerfilUsuario(token, body)

                // LLENAR VISTA
                findViewById<TextView>(R.id.tv_perfil_nombre).text = "${response.nombre} ${response.apellido}"
                findViewById<TextView>(R.id.tv_perfil_carrera).text = "${response.carrera ?: ""} • ${response.rol}"

                // Estadísticas (Si tu modelo las incluye)
                findViewById<TextView>(R.id.tv_count_seguidores).text = response.estadisticas?.seguidores?.toString() ?: "0"
                findViewById<TextView>(R.id.tv_count_publicaciones).text = response.estadisticas?.totalPublicaciones?.toString() ?: "0"

                // Imagen de Perfil
                val ivAvatar = findViewById<CircleImageView>(R.id.iv_perfil_avatar)
                if (!response.imagen.isNullOrEmpty()) {
                    val url = if (response.imagen.startsWith("http")) response.imagen else "${BuildConfig.API_BASE_URL}${response.imagen}"
                    Glide.with(this@PerfilOtroUsuarioActivity).load(url).placeholder(R.drawable.ic_profile).into(ivAvatar)
                }

                isFollowing = response.estadisticas?.siguiendo == true
                actualizarBotonUI()

                // Lista de Publicaciones
                if (response.publicaciones != null && response.publicaciones.isNotEmpty()) {
                    val listaMapeada = response.publicaciones.map { p ->
                        Publicacion(
                            // --- CORRECCIONES AQUÍ ---
                            id = p.id_publi,                // Antes era p.id
                            titulo = p.proyecto_nombre,     // Antes era p.titulo
                            descripcion = p.descripcion ?: "Sin descripción",
                            autor = "${response.nombre} ${response.apellido}",

                            // El backend en este endpoint no manda fecha exacta, solo "hace_cuanto"
                            fecha = p.hace_cuanto ?: "Reciente",
                            haceCuanto = p.hace_cuanto,     // Antes era p.haceCuanto (camelCase vs snake_case)

                            imagenPortada = p.imagen_portada, // Antes era p.imagenPortada

                            tipo = "PDF",
                            rating = "0.0",
                            tags = emptyList(),
                            views = 0,
                            downloads = 0
                        )
                    }
                    adapter.updateList(listaMapeada)
                }

            } catch (e: Exception) {
                Toast.makeText(this@PerfilOtroUsuarioActivity, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun conectarUsuario() {
        val token = "Bearer ${getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "")}"
        val body = mapOf("matricula_destino" to matriculaTarget)

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.usuariosService.conectarUsuario(token, body)
                Toast.makeText(this@PerfilOtroUsuarioActivity, resp.mensaje, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@PerfilOtroUsuarioActivity, "Error al conectar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun actualizarBotonUI() {
        if (isFollowing) {
            btnConectar.text = "Dejar de seguir"
            btnConectar.setBackgroundColor(getColor(android.R.color.darker_gray)) // O un rojo suave
            btnConectar.icon = getDrawable(R.drawable.ic_close) // Opcional: Icono X
        } else {
            btnConectar.text = "Conectar"
            btnConectar.setBackgroundColor(getColor(R.color.green_500)) // Tu verde original (#2DA036)
            btnConectar.icon = getDrawable(R.drawable.ic_add) // Opcional: Icono +
        }
    }

    private fun alternarConexion() {
        val token = "Bearer ${getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "")}"
        val body = mapOf("matricula_destino" to matriculaTarget)

        // Deshabilitar botón temporalmente para evitar doble click
        btnConectar.isEnabled = false

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.usuariosService.conectarUsuario(token, body)

                // Invertir estado localmente basado en la respuesta exitosa
                isFollowing = !isFollowing
                actualizarBotonUI()

                Toast.makeText(this@PerfilOtroUsuarioActivity, resp.mensaje, Toast.LENGTH_SHORT).show()

                // Opcional: Actualizar contador de seguidores manualmente sin recargar todo
                val tvSeguidores = findViewById<TextView>(R.id.tv_count_seguidores)
                var currentCount = tvSeguidores.text.toString().toIntOrNull() ?: 0
                if (isFollowing) currentCount++ else currentCount--
                tvSeguidores.text = currentCount.toString()

            } catch (e: Exception) {
                Toast.makeText(this@PerfilOtroUsuarioActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                btnConectar.isEnabled = true
            }
        }
    }
}