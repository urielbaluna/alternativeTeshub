package com.example.teshub_v1.ui.publicaciones

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.RevisionRequest
import com.example.teshub_v1.data.model.PublicacionDetalle
import com.example.teshub_v1.data.network.RetrofitClient
import com.example.teshub_v1.ui.publicaciones.ComentariosActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import android.widget.RatingBar
import com.example.teshub_v1.data.model.CalificarRequest
import org.json.JSONObject

class PublicacionDetalleActivity : AppCompatActivity() {

    private var publicacionId: Int = 0
    private var esModoAsesor: Boolean = false

    // Vistas
    private lateinit var tvTitulo: TextView
    private lateinit var tvAutores: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var ivPortada: ImageView
    private lateinit var cgTags: ChipGroup
    private lateinit var layoutArchivos: LinearLayout
    private lateinit var cardRevision: View
    private lateinit var chipEstado: Chip
    private lateinit var btnVerHistorial: TextView
    private lateinit var fabEditar: FloatingActionButton
    private lateinit var ratingBar: RatingBar
    private lateinit var btnCalificar: Button
    private lateinit var cardCalificacion: View
    private var esAutor: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publicacion_detalle)

        // Configurar Toolbar
        setSupportActionBar(findViewById(R.id.toolbar_detalle))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        // Recibir datos del Intent
        publicacionId = intent.getIntExtra("id_publi", 0)
        esModoAsesor = intent.getBooleanExtra("modo_asesor", false)

        if (publicacionId != 0) {
            initViews()
            cargarDetalle()
            registrarVistaBackend()
        }

        initViews()
        cargarDetalle()
    }

    private fun registrarVistaBackend() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "") ?: return

        lifecycleScope.launch {
            try {
                RetrofitClient.publicacionesService.registrarVista("Bearer $token", publicacionId)
                Log.d("STATS", "Vista registrada")
            } catch (e: Exception) {
                Log.e("STATS", "Error al registrar vista", e)
            }
        }
    }

    private fun registrarDescargaBackend() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "") ?: return

        lifecycleScope.launch {
            try {
                RetrofitClient.publicacionesService.registrarDescarga("Bearer $token", publicacionId)
            } catch (e: Exception) {
                Log.e("STATS", "Error al registrar descarga", e)
            }
        }
    }

    private fun initViews() {
        tvTitulo = findViewById(R.id.tv_detalle_titulo)
        tvAutores = findViewById(R.id.tv_detalle_autores)
        tvDescripcion = findViewById(R.id.tv_detalle_descripcion)
        ivPortada = findViewById(R.id.iv_detalle_portada)
        cgTags = findViewById(R.id.cg_tags)
        layoutArchivos = findViewById(R.id.layout_archivos_lista)
        cardRevision = findViewById(R.id.card_revision)
        chipEstado = findViewById(R.id.chip_estado)
        btnVerHistorial = findViewById(R.id.tv_ver_historial)
        fabEditar = findViewById(R.id.fab_editar)
        btnVerHistorial.setOnClickListener { mostrarHistorialRevisiones() }
        ratingBar = findViewById(R.id.rating_bar)
        btnCalificar = findViewById(R.id.btn_enviar_calificacion)
        cardCalificacion = findViewById(R.id.card_calificacion)
        btnVerHistorial.setOnClickListener { mostrarHistorialRevisiones() }
        btnCalificar.setOnClickListener {
            val estrellas = ratingBar.rating
            if (estrellas > 0) {
                enviarCalificacion(estrellas.toInt())
            } else {
                Toast.makeText(this, "Selecciona al menos 1 estrella", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón ir a Comentarios
        findViewById<FloatingActionButton>(R.id.fab_comentarios).setOnClickListener {
            val intent = Intent(this, ComentariosActivity::class.java)
            intent.putExtra("id_publi", publicacionId)
            startActivity(intent)
        }

        fabEditar.setOnClickListener {
            val intent = Intent(this, CrearPublicacionActivity::class.java)
            intent.putExtra("MODO_EDICION", true)
            intent.putExtra("ID_PUBLI", publicacionId)
            intent.putExtra("TITULO_ACTUAL", tvTitulo.text.toString())
            intent.putExtra("DESC_ACTUAL", tvDescripcion.text.toString())
            startActivity(intent)
        }

        // Configurar botones de Asesor
        if (esModoAsesor) {
            cardRevision.visibility = View.VISIBLE
            findViewById<Button>(R.id.btn_aprobar).setOnClickListener {
                mostrarDialogoConfirmacion("aprobado")
            }
            findViewById<Button>(R.id.btn_correcciones).setOnClickListener {
                mostrarDialogoConfirmacion("correcciones")
            }
        }
    }

    private fun cargarDetalle() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null) ?: return

        lifecycleScope.launch {
            try {
                // Llamada al endpoint 'verPublicacion'
                val response = RetrofitClient.publicacionesService.verPublicacion("Bearer $token", publicacionId)

                // Usamos el modelo correcto 'PublicacionDetalle'
                val publi = response.publicacion

                // 1. Obtener mi matrícula de la sesión
                val miMatricula = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("matricula", "")

                // 2. Comprobar si soy autor (asumiendo que 'detalle.integrantes' es una lista de objetos con 'matricula')
                // Si tu backend devuelve un flag 'es_autor', úsalo directamente. Si no, iteramos:
                esAutor = publi.integrantes?.any { it.matricula == miMatricula } ?: false
                if (esAutor) {
                    fabEditar.visibility = View.VISIBLE
                    cardCalificacion.visibility = View.GONE
                } else {
                    fabEditar.visibility = View.GONE
                    if (publi.miCalificacion != null && publi.miCalificacion > 0) {
                        // YA CALIFICÓ: Mostrar su nota y bloquear
                        cardCalificacion.visibility = View.VISIBLE
                        ratingBar.rating = publi.miCalificacion.toFloat()
                        ratingBar.setIsIndicator(true) // Hace que las estrellas sean solo lectura
                        btnCalificar.visibility = View.GONE // Ocultar botón

                        // Opcional: Cambiar título de la tarjeta
                        val tvTituloCard = findViewById<TextView>(R.id.tv_titulo_calificacion) // (Asumiendo que le pongas ID al TextView "Calificar Proyecto")
                        tvTituloCard?.text = "Tu Calificación"

                    } else {
                        // NO HA CALIFICADO: Permitir interacción
                        cardCalificacion.visibility = View.VISIBLE
                        ratingBar.rating = 0f
                        ratingBar.setIsIndicator(false)
                        btnCalificar.visibility = View.VISIBLE
                        btnCalificar.isEnabled = true
                        btnCalificar.text = "Enviar"
                    }
                }

                tvTitulo.text = publi.nombre
                tvDescripcion.text = publi.descripcion

                // Mapeo de integrantes
                val autoresTexto = publi.integrantes?.joinToString(", ") { "${it.nombre} ${it.apellido ?: ""}" } ?: "Anónimo"
                tvAutores.text = "Por: $autoresTexto"

                // Estado (Badge)
                if (esModoAsesor || !publi.estado.isNullOrEmpty()) {
                    chipEstado.visibility = View.VISIBLE
                    chipEstado.text = (publi.estado ?: "Desconocido").uppercase()
                }

                // Portada
                if (!publi.imagenPortada.isNullOrEmpty()) {
                    val baseUrl = BuildConfig.API_BASE_URL
                    Glide.with(this@PublicacionDetalleActivity)
                        .load("$baseUrl${publi.imagenPortada}")
                        .placeholder(R.drawable.ic_image)
                        .error(R.drawable.ic_image)
                        .into(ivPortada)
                }

                // Tags
                cgTags.removeAllViews()
                publi.tags?.forEach { tag ->
                    val chip = Chip(this@PublicacionDetalleActivity)
                    chip.text = tag
                    cgTags.addView(chip)
                }

                // Archivos Adjuntos (Aquí usamos la función que faltaba)
                layoutArchivos.removeAllViews()
                publi.archivos?.forEach { archivoRuta ->
                    agregarBotonArchivo(archivoRuta)
                }

            } catch (e: Exception) {
                Toast.makeText(this@PublicacionDetalleActivity, "Error al cargar: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    // --- Función que faltaba ---
    private fun agregarBotonArchivo(rutaRelativa: String) {
        // Inflar el diseño simple para el archivo
        val view = layoutInflater.inflate(R.layout.item_archivo, layoutArchivos, false)
        val tvNombre = view.findViewById<TextView>(R.id.txt_nombre_archivo)
        val btnEliminar = view.findViewById<ImageView>(R.id.btn_eliminar_archivo)
        val icono = view.findViewById<ImageView>(R.id.icono_archivo)

        // Limpiar ruta para mostrar solo el nombre del archivo
        val nombreArchivo = rutaRelativa.substringAfterLast("/")
        tvNombre.text = nombreArchivo

        // Reutilizamos item_archivo.xml pero cambiamos el botón eliminar por descargar visualmente
        // (O simplemente lo ocultamos si prefieres que toda la fila sea clicable)
        btnEliminar.visibility = View.GONE
        // O si quieres cambiar el icono: btnEliminar.setImageResource(R.drawable.ic_download)

        if (nombreArchivo.lowercase().endsWith(".pdf")) {
            icono.setImageResource(R.drawable.ic_pdf)
        } else {
            icono.setImageResource(R.drawable.ic_image)
        }

        // Al hacer click, abrir en navegador
        view.setOnClickListener {
            registrarDescargaBackend()
            val fullUrl = "${BuildConfig.API_BASE_URL}$rutaRelativa"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
            startActivity(browserIntent)
        }

        layoutArchivos.addView(view)
    }

    // Lógica de Asesor
    private fun mostrarDialogoConfirmacion(nuevoEstado: String) {
        val input = EditText(this)
        input.hint = "Comentarios opcionales"

        AlertDialog.Builder(this)
            .setTitle("Confirmar Revisión")
            .setMessage("¿Marcar tesis como ${nuevoEstado.uppercase()}?")
            .setView(input)
            .setPositiveButton("Enviar") { _, _ ->
                enviarRevision(nuevoEstado, input.text.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun enviarRevision(estado: String, comentarios: String) {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null) ?: return

        lifecycleScope.launch {
            try {
                val body = RevisionRequest(publicacionId, estado, comentarios)
                val response = RetrofitClient.revisionesService.revisarPublicacion("Bearer $token", body)

                if (response.isSuccessful) {
                    Toast.makeText(this@PublicacionDetalleActivity, "Revisión enviada", Toast.LENGTH_SHORT).show()
                    finish() // Regresar al dashboard
                } else {
                    Toast.makeText(this@PublicacionDetalleActivity, "Error al enviar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PublicacionDetalleActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarHistorialRevisiones() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null) ?: return

        lifecycleScope.launch {
            try {
                // Llama al endpoint del historial con el ID de la publicación
                val response = RetrofitClient.revisionesService.obtenerHistorial("Bearer $token", publicacionId)

                if (response.isSuccessful && response.body() != null) {
                    val historial = response.body()!!.historial

                    if (historial.isEmpty()) {
                        Toast.makeText(this@PublicacionDetalleActivity, "No hay historial de revisiones.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val sb = StringBuilder()
                    historial.forEach { item ->
                        // Formato de presentación: Estado, Asesor, Comentarios
                        sb.append("--- ${item.estado.uppercase()} por ${item.nombreAsesor} ---\n")
                        sb.append("Fecha: ${item.fecha.substring(0, 10)}\n")
                        sb.append("Comentarios: ${item.comentarios}\n\n")
                    }

                    // Muestra el historial en un cuadro de diálogo desplazable
                    AlertDialog.Builder(this@PublicacionDetalleActivity)
                        .setTitle("Historial de Revisión")
                        .setMessage(sb.toString())
                        .setPositiveButton("Cerrar", null)
                        .show()

                } else {
                    Toast.makeText(this@PublicacionDetalleActivity, "Error al cargar historial", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PublicacionDetalleActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun enviarCalificacion(puntos: Int) {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "") ?: return

        btnCalificar.isEnabled = false
        btnCalificar.text = "..."

        lifecycleScope.launch {
            try {
                val request = CalificarRequest(
                    id_publi = publicacionId,
                    evaluacion = puntos
                )

                val response = RetrofitClient.publicacionesService.calificarPublicacion("Bearer $token", request)

                if (response.isSuccessful) {
                    Toast.makeText(this@PublicacionDetalleActivity, "¡Gracias por calificar!", Toast.LENGTH_SHORT).show()
                    cardCalificacion.visibility = View.GONE
                } else {
                    val errorBody = response.errorBody()?.string()
                    val msg = try {
                        JSONObject(errorBody).optString("mensaje", "Error al calificar")
                    } catch (e: Exception) { "Error desconocido" }

                    Toast.makeText(this@PublicacionDetalleActivity, msg, Toast.LENGTH_SHORT).show()

                    if (msg.contains("Ya calificaste")) {
                        cardCalificacion.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("PublicacionDetalleActivity", "Error al calificar", e)
                Toast.makeText(this@PublicacionDetalleActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            } finally {
                btnCalificar.isEnabled = true
                btnCalificar.text = "Enviar"
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}