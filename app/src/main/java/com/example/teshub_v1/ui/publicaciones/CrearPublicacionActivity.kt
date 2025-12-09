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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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

    private var uriPortada: Uri? = null
    private var listaArchivosAdjuntos: MutableList<Uri> = mutableListOf()

    private lateinit var ivPortadaPreview: ImageView
    private lateinit var layoutPlaceholderPortada: LinearLayout
    private lateinit var layoutArchivos: LinearLayout
    private lateinit var btnPublicar: Button

    private lateinit var etTitulo: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var etTags: TextInputEditText
    private lateinit var etColaboradores: TextInputEditText
    private lateinit var tilColaboradores: TextInputLayout

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
        etTitulo = findViewById(R.id.et_titulo)
        etDescripcion = findViewById(R.id.et_descripcion)
        etTags = findViewById(R.id.et_tags)
        etColaboradores = findViewById(R.id.et_colaboradores)
        tilColaboradores = findViewById(R.id.til_colaboradores) //
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
        modoEdicion = intent.getBooleanExtra("MODO_EDICION", false)
        idPublicacionEditar = intent.getIntExtra("ID_PUBLI", -1)

        if (modoEdicion) {
            supportActionBar?.title = "Editar Publicación"
            btnPublicar.text = "Guardar Cambios"
            tilColaboradores.visibility = View.GONE
            cargarDatosOriginales(idPublicacionEditar)
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

    private fun cargarDatosOriginales(id: Int) {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "") ?: ""

        lifecycleScope.launch {
            try {
                // Llamamos a verPublicacion para traer TODOS los detalles
                val response = RetrofitClient.publicacionesService.verPublicacion("Bearer $token", id)
                val data = response.publicacion //

                // A. Llenar Textos
                etTitulo.setText(data.nombre)
                etDescripcion.setText(data.descripcion)
                // Convertir lista de tags a String separado por comas
                etTags.setText(data.tags?.joinToString(", "))

                // B. Cargar Portada con Glide
                if (!data.imagenPortada.isNullOrEmpty()) {
                    val fullUrl = "${BuildConfig.API_BASE_URL}${data.imagenPortada}"
                    Glide.with(this@CrearPublicacionActivity)
                        .load(fullUrl)
                        .into(ivPortadaPreview)
                    layoutPlaceholderPortada.visibility = View.GONE
                }

                data.archivos?.forEach { rutaArchivo ->
                    agregarVistaArchivoExistente(rutaArchivo)
                }

            } catch (e: Exception) {
                Toast.makeText(this@CrearPublicacionActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- FUNCIÓN POST (CREAR) ---
    private fun enviarPublicacion(titulo: String, desc: String, tags: String, colabs: String) {
        val sharedPref = getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", null) ?: return

        lifecycleScope.launch(Dispatchers.IO) {
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
                Log.e("CrearPublicacionActivity", "Error en la solicitud: ${e.message}", e)
            }
        }
    }

    // --- FUNCIÓN PUT (ACTUALIZAR) ---
    private fun actualizarPublicacion(id: Int, titulo: String, descripcion: String, tags: String) {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "") ?: ""

        // Textos
        val titlePart = RequestBody.create("text/plain".toMediaTypeOrNull(), titulo)
        val descPart = RequestBody.create("text/plain".toMediaTypeOrNull(), descripcion)
        val tagsPart = RequestBody.create("text/plain".toMediaTypeOrNull(), tags)

        // Imagen Portada (Si se cambió)
        var bodyImagen: MultipartBody.Part? = null
        uriPortada?.let { uri ->
            val file = uriToFile(uri, "portada_edit")
            val reqFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
            bodyImagen = MultipartBody.Part.createFormData("portada", file.name, reqFile)
        }

        // NUEVO: Archivos Adjuntos (Solo los nuevos en listaArchivosAdjuntos)
        val archivosParts = mutableListOf<MultipartBody.Part>()
        listaArchivosAdjuntos.forEach { uri ->
            val nombre = obtenerNombreArchivo(uri)
            val file = uriToFile(uri, nombre)
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val reqFile = file.readBytes().toRequestBody(mimeType.toMediaTypeOrNull())
            archivosParts.add(MultipartBody.Part.createFormData("archivos", nombre, reqFile))
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.publicacionesService.actualizarPublicacion(
                    "Bearer $token",
                    id,
                    titlePart,
                    descPart,
                    tagsPart,
                    bodyImagen,
                    if (archivosParts.isNotEmpty()) archivosParts else null // Enviamos lista o null
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@CrearPublicacionActivity, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@CrearPublicacionActivity, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    btnPublicar.isEnabled = true
                }
            } catch (e: Exception) {
                Toast.makeText(this@CrearPublicacionActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                btnPublicar.isEnabled = true
            }
        }
    }

    // --- Helper UI para Archivos Locales (Nuevos) ---
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

    // --- Helper UI para Archivos Remotos (Ya existentes) ---
    private fun agregarVistaArchivoExistente(rutaRelativa: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_archivo, layoutArchivos, false)
        val tvNombre = view.findViewById<TextView>(R.id.txt_nombre_archivo)
        val ivIcono = view.findViewById<ImageView>(R.id.icono_archivo)
        val btnEliminar = view.findViewById<ImageView>(R.id.btn_eliminar_archivo)

        val nombreArchivo = File(rutaRelativa).name
        tvNombre.text = nombreArchivo

        // Icono
        if (nombreArchivo.lowercase().endsWith(".pdf")) {
            ivIcono.setImageResource(R.drawable.ic_pdf)
        } else {
            ivIcono.setImageResource(R.drawable.ic_image)
        }

        // Lógica de Eliminación Remota
        btnEliminar.visibility = View.VISIBLE // Hacemos visible el botón
        btnEliminar.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar archivo")
                .setMessage("¿Seguro que deseas eliminar este archivo permanentemente?")
                .setPositiveButton("Eliminar") { _, _ ->
                    eliminarArchivoRemoto(rutaRelativa, view)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        layoutArchivos.addView(view)
    }

    private fun eliminarArchivoRemoto(ruta: String, viewToRemove: View) {
        val token =
            getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", "") ?: ""

        lifecycleScope.launch {
            try {
                val request = com.example.teshub_v1.data.model.EliminarArchivoRequest(
                    id_publi = idPublicacionEditar,
                    ruta = ruta
                )

                val response =
                    RetrofitClient.publicacionesService.eliminarArchivo("Bearer $token", request)

                if (response.isSuccessful) {
                    layoutArchivos.removeView(viewToRemove)
                    Toast.makeText(
                        this@CrearPublicacionActivity,
                        "Archivo eliminado",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@CrearPublicacionActivity,
                        "No se pudo eliminar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@CrearPublicacionActivity,
                    "Error de red: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            }
        }
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