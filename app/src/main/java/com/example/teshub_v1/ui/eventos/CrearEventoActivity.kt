package com.example.teshub_v1.ui.eventos

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.teshub_v1.R
import com.example.teshub_v1.data.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CrearEventoActivity : AppCompatActivity() {

    // Variables de datos
    private var uriFoto: Uri? = null
    private var latitudSeleccionada: Double? = null
    private var longitudSeleccionada: Double? = null

    // UI
    private lateinit var ivEventoPreview: ImageView
    private lateinit var tvCoordenadas: TextView
    private lateinit var etFecha: TextInputEditText
    private lateinit var etHora: TextInputEditText
    private lateinit var spinnerCategoria: Spinner

    // Calendario
    private val calendar = Calendar.getInstance()

    // 1. Selector de Imagen
    private val selectorFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uriFoto = it
            ivEventoPreview.setImageURI(it)
            ivEventoPreview.setPadding(0,0,0,0) // Quitar padding del placeholder
            ivEventoPreview.imageTintList = null // Quitar tinte gris
        }
    }

    // 2. Selector de Mapa
    private val selectorMapa = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra("latitud", 0.0)
            val lng = result.data?.getDoubleExtra("longitud", 0.0)
            if (lat != null && lng != null && lat != 0.0) {
                latitudSeleccionada = lat
                longitudSeleccionada = lng
                tvCoordenadas.text = "Ubicación: $lat, $lng"
                tvCoordenadas.setTextColor(resources.getColor(R.color.black, null))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_evento)

        // Bind Views
        val etTitulo = findViewById<TextInputEditText>(R.id.et_titulo)
        val etUbicacionNombre = findViewById<TextInputEditText>(R.id.et_ubicacion_nombre)
        val etCupo = findViewById<TextInputEditText>(R.id.et_cupo)
        val etTags = findViewById<TextInputEditText>(R.id.et_tags)
        val etDescripcion = findViewById<TextInputEditText>(R.id.et_descripcion)
        val etOrganizadores = findViewById<TextInputEditText>(R.id.et_organizadores)
        val btnCrear = findViewById<Button>(R.id.btn_crear_evento)

        ivEventoPreview = findViewById(R.id.iv_evento_preview)
        tvCoordenadas = findViewById(R.id.tv_coordenadas)
        etFecha = findViewById(R.id.et_fecha)
        etHora = findViewById(R.id.et_hora)
        spinnerCategoria = findViewById(R.id.spinner_categoria)

        // Configurar Spinner Categorías
        val categorias = arrayOf("Conferencia", "Workshop", "Seminario", "Simposio", "Cultural", "Otro")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapter

        // Listeners
        findViewById<View>(R.id.btn_seleccionar_foto).setOnClickListener { selectorFoto.launch("image/*") }

        findViewById<Button>(R.id.btn_seleccionar_mapa).setOnClickListener {
            val intent = Intent(this, SeleccionarUbicacionActivity::class.java)
            selectorMapa.launch(intent)
        }

        etFecha.setOnClickListener { mostrarDatePicker() }
        etHora.setOnClickListener { mostrarTimePicker() }

        btnCrear.setOnClickListener {
            val titulo = etTitulo.text.toString().trim()
            val ubicacionNombre = etUbicacionNombre.text.toString().trim()
            val cupo = etCupo.text.toString().trim()
            val tags = etTags.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val organizadores = etOrganizadores.text.toString().trim()
            val categoria = spinnerCategoria.selectedItem.toString()
            val fechaStr = etFecha.text.toString()
            val horaStr = etHora.text.toString()

            // Validaciones
            if (titulo.isEmpty() || descripcion.isEmpty() || ubicacionNombre.isEmpty() ||
                fechaStr.isEmpty() || horaStr.isEmpty() || latitudSeleccionada == null) {
                Toast.makeText(this, "Por favor completa los campos obligatorios y selecciona ubicación en el mapa", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Formatear fecha para MySQL
            val fechaFinal = formatearFechaMySQL(fechaStr, horaStr)

            btnCrear.isEnabled = false
            btnCrear.text = "Creando..."

            enviarEvento(titulo, fechaFinal, descripcion, categoria, ubicacionNombre,
                latitudSeleccionada!!, longitudSeleccionada!!, cupo, tags, organizadores)
        }
    }

    private fun mostrarDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            etFecha.setText(format.format(calendar.time))
        }
        DatePickerDialog(this, dateSetListener,
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun mostrarTimePicker() {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            etHora.setText(format.format(calendar.time))
        }
        TimePickerDialog(this, timeSetListener,
            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun formatearFechaMySQL(fecha: String, hora: String): String {
        return try {
            val parts = fecha.split("/")
            "${parts[2]}-${parts[1]}-${parts[0]} $hora:00"
        } catch (e: Exception) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        }
    }

    private fun enviarEvento(
        titulo: String, fecha: String, desc: String, categoria: String, ubiNombre: String,
        lat: Double, lng: Double, cupo: String, tags: String, orgs: String
    ) {
        val sharedPref = getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", null) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Preparar Partes de Texto
                val pTitulo = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
                val pDesc = desc.toRequestBody("text/plain".toMediaTypeOrNull())
                val pFecha = fecha.toRequestBody("text/plain".toMediaTypeOrNull())
                val pLat = lat.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val pLng = lng.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val pCat = categoria.toRequestBody("text/plain".toMediaTypeOrNull())
                val pUbiNom = ubiNombre.toRequestBody("text/plain".toMediaTypeOrNull())
                val pTags = tags.toRequestBody("text/plain".toMediaTypeOrNull())
                val pOrgs = orgs.toRequestBody("text/plain".toMediaTypeOrNull())
                val pCupo = cupo.toRequestBody("text/plain".toMediaTypeOrNull())

                // Preparar Foto
                var pFoto: MultipartBody.Part? = null
                uriFoto?.let { uri ->
                    val file = uriToFile(uri, "evento_cover")
                    val reqFile = file.readBytes().toRequestBody("image/jpeg".toMediaTypeOrNull())
                    pFoto = MultipartBody.Part.createFormData("foto", file.name, reqFile)
                }

                // Llamada con todos los campos nuevos
                val response = RetrofitClient.eventosService.crearEvento(
                    "Bearer $token",
                    pTitulo, pDesc, pFecha, pLat, pLng,
                    pOrgs, pCupo, pCat, pUbiNom, pTags, pFoto
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CrearEventoActivity, "Evento creado exitosamente", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@CrearEventoActivity, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                        findViewById<Button>(R.id.btn_crear_evento).isEnabled = true
                        findViewById<Button>(R.id.btn_crear_evento).text = "Crear Evento"
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CrearEventoActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                    findViewById<Button>(R.id.btn_crear_evento).isEnabled = true
                }
            }
        }
    }

    private fun uriToFile(uri: Uri, nombreBase: String): File {
        val inputStream = contentResolver.openInputStream(uri)!!
        val safeName = nombreBase.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        val tempFile = File(cacheDir, "${safeName}_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return tempFile
    }
}