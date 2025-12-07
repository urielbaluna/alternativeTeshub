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
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Evento
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

class EditarEventoActivity : AppCompatActivity() {

    private var eventoId: Int = 0
    private var uriNuevaFoto: Uri? = null
    private var latitudSeleccionada: Double? = null
    private var longitudSeleccionada: Double? = null
    private var organizadoresOriginales: String = ""

    // Vistas
    private lateinit var ivPreview: ImageView
    private lateinit var etTitulo: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var etUbicacionNombre: TextInputEditText
    private lateinit var etCupo: TextInputEditText
    private lateinit var etTags: TextInputEditText
    private lateinit var etFecha: TextInputEditText
    private lateinit var etHora: TextInputEditText
    private lateinit var tvCoordenadas: TextView
    private lateinit var btnGuardar: Button
    private lateinit var spinnerCategoria: Spinner

    private val calendar = Calendar.getInstance()

    // Selector de Imagen
    private val selectorFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uriNuevaFoto = it
            ivPreview.setImageURI(it)
            ivPreview.setPadding(0, 0, 0, 0)
            ivPreview.imageTintList = null
        }
    }

    // Selector de Mapa
    private val selectorMapa = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra("latitud", 0.0)
            val lng = result.data?.getDoubleExtra("longitud", 0.0)
            if (lat != null && lng != null && lat != 0.0) {
                latitudSeleccionada = lat
                longitudSeleccionada = lng
                tvCoordenadas.text = "Nueva ubicación: $lat, $lng"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_evento)

        eventoId = intent.getIntExtra("evento_id", 0)
        if (eventoId == 0) {
            Toast.makeText(this, "Error de evento", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        cargarDatosEvento()
    }

    private fun initViews() {
        ivPreview = findViewById(R.id.iv_evento_preview)
        etTitulo = findViewById(R.id.et_titulo)
        etDescripcion = findViewById(R.id.et_descripcion)
        etUbicacionNombre = findViewById(R.id.et_ubicacion_nombre)
        etCupo = findViewById(R.id.et_cupo)
        etTags = findViewById(R.id.et_tags)
        etFecha = findViewById(R.id.et_fecha)
        etHora = findViewById(R.id.et_hora)
        tvCoordenadas = findViewById(R.id.tv_coordenadas)
        btnGuardar = findViewById(R.id.btn_guardar_cambios)
        spinnerCategoria = findViewById(R.id.spinner_categoria)

        val categorias = arrayOf("Conferencia", "Workshop", "Seminario", "Simposio", "Cultural", "Otro")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapter

        findViewById<View>(R.id.btn_cambiar_foto).setOnClickListener { selectorFoto.launch("image/*") }

        findViewById<Button>(R.id.btn_seleccionar_mapa).setOnClickListener {
            val intent = Intent(this, SeleccionarUbicacionActivity::class.java)
            intent.putExtra("lat_inicial", latitudSeleccionada)
            intent.putExtra("lng_inicial", longitudSeleccionada)
            selectorMapa.launch(intent)
        }

        etFecha.setOnClickListener { mostrarDatePicker() }
        etHora.setOnClickListener { mostrarTimePicker() }

        btnGuardar.setOnClickListener { guardarCambios() }
    }

    private fun cargarDatosEvento() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null) ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.eventosService.getEvento(eventoId, "Bearer $token")
                }

                if (response.isSuccessful && response.body() != null) {
                    // CORRECCIÓN AQUÍ: Extraemos el objeto .evento del wrapper
                    llenarFormulario(response.body()!!.evento)
                } else {
                    Toast.makeText(this@EditarEventoActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditarEventoActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun llenarFormulario(evento: Evento) {
        etTitulo.setText(evento.titulo)
        etDescripcion.setText(evento.descripcion)
        etUbicacionNombre.setText(evento.ubicacionNombre)
        etCupo.setText(evento.cupoMaximo.toString())
        etTags.setText(evento.tags?.joinToString(", ") ?: "")

        organizadoresOriginales = evento.organizadores.joinToString(",") { it.matricula }

        latitudSeleccionada = evento.ubicacionObj?.latitud
        longitudSeleccionada = evento.ubicacionObj?.longitud
        tvCoordenadas.text = "Ubicación actual: $latitudSeleccionada, $longitudSeleccionada"

        val catAdapter = spinnerCategoria.adapter as ArrayAdapter<String>
        val posicion = catAdapter.getPosition(evento.categoria)
        if (posicion >= 0) spinnerCategoria.setSelection(posicion)

        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'", Locale.getDefault())
            val date = inputFormat.parse(evento.fecha)
            if (date != null) {
                calendar.time = date
                etFecha.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date))
                etHora.setText(SimpleDateFormat("HH:mm", Locale.getDefault()).format(date))
            }
        } catch (e: Exception) {
            etFecha.setText(evento.fecha)
        }

        if (!evento.urlFoto.isNullOrEmpty()) {
            val fullUrl = "${BuildConfig.API_BASE_URL}/${evento.urlFoto}"
            Glide.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.ic_image)
                .into(ivPreview)
            ivPreview.setPadding(0, 0, 0, 0)
            ivPreview.imageTintList = null
        }
    }

    private fun guardarCambios() {
        val titulo = etTitulo.text.toString().trim()
        val desc = etDescripcion.text.toString().trim()
        val ubiNombre = etUbicacionNombre.text.toString().trim()
        val cupo = etCupo.text.toString().trim()
        val tags = etTags.text.toString().trim()
        val cat = spinnerCategoria.selectedItem.toString()
        val fechaStr = etFecha.text.toString()
        val horaStr = etHora.text.toString()

        if (titulo.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Título y descripción requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        val fechaFinal = formatearFechaMySQL(fechaStr, horaStr)
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null) ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convertir campos a RequestBody
                val pTitulo = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
                val pDesc = desc.toRequestBody("text/plain".toMediaTypeOrNull())
                val pFecha = fechaFinal.toRequestBody("text/plain".toMediaTypeOrNull())
                val pLat = (latitudSeleccionada ?: 0.0).toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val pLng = (longitudSeleccionada ?: 0.0).toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val pOrgs = organizadoresOriginales.toRequestBody("text/plain".toMediaTypeOrNull())
                val pCupo = (cupo.toIntOrNull() ?: 50).toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val pCat = cat.toRequestBody("text/plain".toMediaTypeOrNull())
                val pUbiNom = ubiNombre.toRequestBody("text/plain".toMediaTypeOrNull())
                val pTags = tags.toRequestBody("text/plain".toMediaTypeOrNull())

                // Foto opcional
                var pFoto: MultipartBody.Part? = null
                uriNuevaFoto?.let { uri ->
                    val file = uriToFile(uri, "evento_update")
                    val reqFile = file.readBytes().toRequestBody("image/jpeg".toMediaTypeOrNull())
                    pFoto = MultipartBody.Part.createFormData("foto", file.name, reqFile)
                }

                // Llamada con Multipart
                val response = RetrofitClient.eventosService.actualizarEvento(
                    eventoId,
                    "Bearer $token",
                    pTitulo, pDesc, pFecha, pLat, pLng, pOrgs, pCupo, pCat, pUbiNom, pTags, pFoto
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditarEventoActivity, "Evento actualizado", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(this@EditarEventoActivity, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditarEventoActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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
        DatePickerDialog(
            this, dateSetListener,
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun mostrarTimePicker() {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            etHora.setText(format.format(calendar.time))
        }
        TimePickerDialog(
            this, timeSetListener,
            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
        ).show()
    }

    private fun formatearFechaMySQL(fecha: String, hora: String): String {
        return try {
            val parts = fecha.split("/")
            "${parts[2]}-${parts[1]}-${parts[0]} $hora:00"
        } catch (e: Exception) {
            fecha
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