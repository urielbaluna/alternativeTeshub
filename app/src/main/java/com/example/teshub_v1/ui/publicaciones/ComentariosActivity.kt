package com.example.teshub_v1.ui.publicaciones

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Comentario
import com.example.teshub_v1.data.model.ComentarioRequest
import com.example.teshub_v1.data.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import org.json.JSONObject

class ComentariosActivity : AppCompatActivity() {

    private var idPublicacion: Int = 0
    private lateinit var adapter: ComentariosAdapter
    private lateinit var etComentario: EditText
    private lateinit var btnEnviar: ImageButton
    private lateinit var rvComentarios: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comentarios)

        idPublicacion = intent.getIntExtra("id_publi", 0)
        if (idPublicacion == 0) {
            finish()
            return
        }

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_comentarios)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // Vistas
        etComentario = findViewById(R.id.et_nuevo_comentario)
        btnEnviar = findViewById(R.id.btn_enviar_comentario)
        rvComentarios = findViewById(R.id.rv_comentarios)

        setupRecyclerView()

        btnEnviar.setOnClickListener {
            val texto = etComentario.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarComentario(texto)
            }
        }

        cargarComentarios()
    }

    private fun setupRecyclerView() {
        val miMatricula = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("matricula", "") ?: ""

        adapter = ComentariosAdapter(mutableListOf(), miMatricula) { comentario ->
            confirmarBorrado(comentario)
        }
        rvComentarios.layoutManager = LinearLayoutManager(this)
        rvComentarios.adapter = adapter
    }

    private fun cargarComentarios() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "") ?: return

        lifecycleScope.launch {
            try {
                // Reutilizamos 'verPublicacion' porque ahí vienen los comentarios
                val response = RetrofitClient.publicacionesService.verPublicacion("Bearer $token", idPublicacion)
                val lista = response.publicacion.comentarios ?: emptyList()
                Log.e("ComentariosActivity", "Comentarios: $lista")

                adapter.updateList(lista)
                // Scrollear al último si hay muchos? O al primero?
                // rvComentarios.scrollToPosition(0)
            } catch (e: Exception) {
                Log.e("ComentariosActivity", "Error al cargar comentarios", e)
                Toast.makeText(this@ComentariosActivity, "Error al cargar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enviarComentario(texto: String) {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "") ?: return

        btnEnviar.isEnabled = false

        lifecycleScope.launch {
            try {
                val body = ComentarioRequest(idPublicacion, texto)
                val response = RetrofitClient.publicacionesService.comentarPublicacion("Bearer $token", body)
                etComentario.text.clear()
                cargarComentarios()
                Toast.makeText(this@ComentariosActivity, response.mensaje, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ComentariosActivity", "Error al enviar comentario", e)
                Toast.makeText(this@ComentariosActivity, "Error al enviar", Toast.LENGTH_SHORT).show()
            } finally {
                btnEnviar.isEnabled = true
            }
        }
    }

    private fun confirmarBorrado(comentario: Comentario) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar comentario")
            .setMessage("¿Estás seguro?")
            .setPositiveButton("Sí") { _, _ -> eliminarComentarioBackend(comentario) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun eliminarComentarioBackend(comentario: Comentario) {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "") ?: return

        lifecycleScope.launch {
            try {
                // El backend pide: matricula, id_publi, comentario
                val body = mapOf(
                    "id_publi" to idPublicacion.toString(),
                    "matricula" to comentario.matricula,
                    "comentario" to comentario.comentario
                )

                val response = RetrofitClient.publicacionesService.eliminarComentario("Bearer $token", body)

                if (response.isSuccessful) {
                    Toast.makeText(this@ComentariosActivity, "Eliminado", Toast.LENGTH_SHORT).show()
                    cargarComentarios() // Recargar lista
                } else {
                    Log.e("ComentariosActivity", "Error al eliminar comentario: ${response.errorBody()?.string()}")
                    Toast.makeText(this@ComentariosActivity, "No se pudo eliminar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ComentariosActivity", "Error al eliminar comentario", e)
                Toast.makeText(this@ComentariosActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }
}