package com.example.teshub_v1.ui.publicaciones

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.teshub_v1.R
import com.example.teshub_v1.data.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class CrearPublicacionActivity : AppCompatActivity() {

    // Variables para archivos
    private var uriPortada: Uri? = null // Única variable para la portada
    private var listaArchivosAdjuntos: MutableList<Uri> = mutableListOf()

    // Vistas
    private lateinit var ivPortadaPreview: ImageView
    private lateinit var layoutPlaceholderPortada: LinearLayout
    private lateinit var layoutArchivos: LinearLayout
    private lateinit var btnPublicar: Button

    // Variables de Estado
    var modoEdicion = false
    var idPublicacionEditar = -1

    // Selectores de Archivos

    // 1. Selector de Portada
    private val selectorPortada = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uriPortada = it // Guardamos en la variable global
            ivPortadaPreview.setImageURI(it)
            layoutPlaceholderPortada.visibility = View.GONE
        }
    }

    // 2. Selector de Documentos
    private val selectorDocumentos = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            if (!listaArchivosAdjuntos.contains(uri)) {
                listaArchivosAdjuntos.add(uri)
                agregarVistaArchivo(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_publicacion)

        // Referencias UI
        val etTitulo = findViewById<TextInputEditText>(R.id.et_titulo)
        val etDescripcion = findViewById<TextInputEditText>(R.id.et_descripcion)
        val etTags = findViewById<TextInputEditText>(R.id.et_tags)
        val etColaboradores = findViewById<TextInputEditText>(R.id.et_colaboradores)

        ivPortadaPreview = findViewById(R.id.iv_portada_preview)
        layoutPlaceholderPortada = findViewById(R.id.layout_placeholder_portada)
        layoutArchivos = findViewById(R.id.layout_archivos)
        btnPublicar = findViewById(R.id.btn_publicar)

        // Listeners
        findViewById<View>(R.id.btn_seleccionar_portada).setOnClickListener {
            selectorPortada.launch("image/*")
        }

        findViewById<Button>(R.id.btn_seleccionar_archivo).setOnClickListener {
            selectorDocumentos.launch("*/*")
        }

        // Detectar Modo
        modoEdicion = intent.getBooleanExtra("MODO_EDICION", false)
        idPublicacionEditar = intent.getIntExtra("ID_PUBLI", -1)

        if (modoEdicion) {
            supportActionBar?.title = "Editar Publicación"
            btnPublicar.text = "Guardar Cambios"
            etTitulo.setText(intent.getStringExtra("TITULO_ACTUAL"))
            etDescripcion.setText(intent.getStringExtra("DESC_ACTUAL"))
        }

        // Botón Principal
        btnPublicar.setOnClickListener {
            val titulo = etTitulo.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val tags = etTags.text.toString().trim()
            val colaboradores = etColaboradores.text.toString().trim()

            if (titulo.isEmpty() || descripcion.isEmpty()) {
                Toast.makeText(this, "Título y descripción son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnPublicar.isEnabled = false
            btnPublicar.text = if (modoEdicion) "Guardando..." else "Subiendo..."

            if (modoEdicion) {
                actualizarPublicacion(idPublicacionEditar, titulo, descripcion, tags)
            } else {
                enviarPublicacion(titulo, descripcion, tags, colaboradores)
            }
        }
    }

    // --- FUNCIÓN POST (CREAR) ---
    private fun enviarPublicacion(titulo: String, desc: String, tags: String, colabs: String) {
        val sharedPref = getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", null) ?: return

        lifecycleScope.launch(Dispatchers.IO) { // Usamos lifecycleScope consistentemente
            try {
                val tituloPart = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
                val descPart = desc.toRequestBody("text/plain".toMediaTypeOrNull())
                val tagsPart = if (tags.isNotEmpty()) tags.toRequestBody("text/plain".toMediaTypeOrNull()) else null
                val colabsPart = if (colabs.isNotEmpty()) colabs.toRequestBody("text/plain".toMediaTypeOrNull()) else "".toRequestBody("text/plain".toMediaTypeOrNull())

                var portadaPart: MultipartBody.Part? = null
                uriPortada?.let { uri ->
                    val file = uriToFile(uri, "portada")
                    val reqFile = file.readBytes().toRequestBody("image/jpeg".toMediaTypeOrNull())
                    portadaPart = MultipartBody.Part.createFormData("portada", file.name, reqFile)
                }

                val archivosParts = mutableListOf<MultipartBody.Part>()
                listaArchivosAdjuntos.forEach { uri ->
                    val nombre = obtenerNombreArchivo(uri)
                    val file = uriToFile(uri, nombre)
                    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                    val reqFile = file.readBytes().toRequestBody(mimeType.toMediaTypeOrNull())
                    archivosParts.add(MultipartBody.Part.createFormData("archivos", nombre, reqFile))
                }

                val response = RetrofitClient.publicacionesService.crearPublicacion(
                    "Bearer $token",
                    tituloPart,
                    descPart,
                    colabsPart,
                    tagsPart,
                    portadaPart,
                    if (archivosParts.isNotEmpty()) archivosParts else null
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CrearPublicacionActivity, response.mensaje, Toast.LENGTH_LONG).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnPublicar.isEnabled = true
                    btnPublicar.text = "Publicar Proyecto"
                    Toast.makeText(this@CrearPublicacionActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    // --- FUNCIÓN PUT (ACTUALIZAR) ---
    private fun actualizarPublicacion(id: Int, titulo: String, descripcion: String, tags: String) {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "") ?: ""

        // Preparar partes de texto
        val titlePart = RequestBody.create("text/plain".toMediaTypeOrNull(), titulo)
        val descPart = RequestBody.create("text/plain".toMediaTypeOrNull(), descripcion)
        val tagsPart = RequestBody.create("text/plain".toMediaTypeOrNull(), tags)

        // Manejo de imagen (Ahora usa uriPortada y uriToFile correctamente)
        var bodyImagen: MultipartBody.Part? = null
        uriPortada?.let { uri ->
            val file = uriToFile(uri, "portada_edit") // Usamos el helper que YA existe
            val reqFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
            bodyImagen = MultipartBody.Part.createFormData("portada", file.name, reqFile)
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.publicacionesService.actualizarPublicacion(
                    "Bearer $token",
                    id,
                    titlePart,
                    descPart,
                    tagsPart,
                    bodyImagen
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@CrearPublicacionActivity, "Publicación actualizada", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string()
                    Toast.makeText(this@CrearPublicacionActivity, "Error al actualizar: $errorMsg", Toast.LENGTH_LONG).show()
                    btnPublicar.isEnabled = true
                    btnPublicar.text = "Guardar Cambios"
                }
            } catch (e: Exception) {
                Log.e("EDITAR_PUB", "Error", e)
                Toast.makeText(this@CrearPublicacionActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                btnPublicar.isEnabled = true
                btnPublicar.text = "Guardar Cambios"
            }
        }
    }

    // --- Helpers de UI y Archivos ---

    private fun agregarVistaArchivo(uri: Uri) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_archivo, layoutArchivos, false)
        val tvNombre = view.findViewById<TextView>(R.id.txt_nombre_archivo)
        val ivIcono = view.findViewById<ImageView>(R.id.icono_archivo)
        val btnEliminar = view.findViewById<ImageView>(R.id.btn_eliminar_archivo)

        val nombre = obtenerNombreArchivo(uri)
        tvNombre.text = nombre

        if (nombre.lowercase().endsWith(".pdf")) {
            ivIcono.setImageResource(R.drawable.ic_pdf)
        } else {
            ivIcono.setImageResource(R.drawable.ic_image)
        }

        btnEliminar.setOnClickListener {
            listaArchivosAdjuntos.remove(uri)
            layoutArchivos.removeView(view)
        }
        layoutArchivos.addView(view)
    }

    private fun obtenerNombreArchivo(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = it.getString(index)
                }
            }
        }
        return result ?: "archivo_desconocido"
    }

    private fun uriToFile(uri: Uri, nombreBase: String): File {
        val inputStream = contentResolver.openInputStream(uri)!!
        val safeName = nombreBase.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val tempFile = File(cacheDir, safeName)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return tempFile
    }
}