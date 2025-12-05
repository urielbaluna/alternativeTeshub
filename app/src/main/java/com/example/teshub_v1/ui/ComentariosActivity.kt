package com.example.teshub_v1.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.CrearComentarioResponse
import com.example.teshub_v1.data.model.PublicacionDetalleResponse
import com.example.teshub_v1.data.network.RetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.teshub_v1.data.model.ComentarioRequest

class ComentariosActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etComentario: EditText
    private lateinit var btnEnviar: Button
    private lateinit var adapter: ComentariosAdapter
    private var idPublicacion: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comentarios)

        recyclerView = findViewById(R.id.recyclerComentarios)
        etComentario = findViewById(R.id.etComentario)
        btnEnviar = findViewById(R.id.btnEnviarComentario)

        recyclerView.layoutManager = LinearLayoutManager(this)

        idPublicacion = intent.getIntExtra("idPublicacion", -1)
        if (idPublicacion <= 0) {
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cargarComentarios()

        btnEnviar.setOnClickListener {
            val texto = etComentario.text.toString().trim()
            if (texto.isEmpty()) {
                Toast.makeText(this, "Escribe un comentario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (texto.length > 300) {
                Toast.makeText(this, "Máximo 300 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val token = obtenerToken()
            lifecycleScope.launch {
                try {
                    val request = ComentarioRequest(
                        id_publi = idPublicacion,
                        comentario = texto
                    )
                    val respuesta: CrearComentarioResponse =
                        RetrofitClient.publicacionesService.comentarPublicacion(token, request)
                    Toast.makeText(this@ComentariosActivity, respuesta.mensaje, Toast.LENGTH_SHORT).show()
                    etComentario.text.clear()
                    cargarComentarios()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@ComentariosActivity, "Error al comentar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cargarComentarios() {
        val token = obtenerToken()
        lifecycleScope.launch {
            try {
                val response: PublicacionDetalleResponse =
                    RetrofitClient.publicacionesService.verPublicacion(token, idPublicacion)

                val lista = response.publicacion.comentarios ?: emptyList()

                adapter = ComentariosAdapter(lista)
                recyclerView.adapter = adapter

                val fechaFormateada = formatearFecha(response.publicacion.fecha)
                Toast.makeText(this@ComentariosActivity, "Fecha: $fechaFormateada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ComentariosActivity, "Error al cargar comentarios: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerToken(): String {
        val sharedPref = getSharedPreferences("sesion", MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""
        return "Bearer $token"
    }

    private fun formatearFecha(fechaIso: String?): String {
        return try {
            val formatoIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            formatoIso.timeZone = TimeZone.getTimeZone("UTC")
            val date = fechaIso?.let { formatoIso.parse(it) }
            val formatoLatam = SimpleDateFormat("dd/MM/yyyy", Locale("es", "MX"))
            date?.let { formatoLatam.format(it) } ?: "Sin fecha"
        } catch (e: Exception) {
            "Sin fecha"
        }
    }
}