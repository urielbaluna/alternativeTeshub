package com.example.teshub_v1.ui.eventos

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.teshub_v1.R
import com.example.teshub_v1.data.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CrearEventoActivity : AppCompatActivity() {

    private lateinit var etTitulo: TextInputEditText
    private lateinit var etOrganizadores: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var etCupoMaximo: TextInputEditText
    private lateinit var etFecha: TextInputEditText
    private lateinit var btnSeleccionarFoto: Button
    private lateinit var tvNombreFoto: TextView
    private lateinit var btnDeseleccionarFoto: ImageButton
    private lateinit var btnSeleccionarUbicacion: Button
    private lateinit var tvCoordenadas: TextView
    private lateinit var btnCrearEvento: Button

    private val fechaHoraSeleccionada = Calendar.getInstance()
    private var fotoUri: Uri? = null
    private var latitudSeleccionada: Double? = null
    private var longitudSeleccionada: Double? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            fotoUri = it
            val nombreArchivo = obtenerNombreArchivo(it)
            tvNombreFoto.text = nombreArchivo
            tvNombreFoto.visibility = View.VISIBLE
            btnDeseleccionarFoto.visibility = View.VISIBLE
            btnSeleccionarFoto.text = "Cambiar Foto"
        }
    }

    private val locationPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            latitudSeleccionada = data?.getDoubleExtra("latitud", 0.0)
            longitudSeleccionada = data?.getDoubleExtra("longitud", 0.0)
            tvCoordenadas.text = String.format("Lat: %.4f, Lng: %.4f", latitudSeleccionada, longitudSeleccionada)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_evento)

        etTitulo = findViewById(R.id.etTituloEvento)
        etOrganizadores = findViewById(R.id.etOrganizadoresEvento)
        etDescripcion = findViewById(R.id.etDescripcionEvento)
        etCupoMaximo = findViewById(R.id.etCupoMaximo)
        etFecha = findViewById(R.id.etFechaEvento)
        btnSeleccionarFoto = findViewById(R.id.btnSeleccionarFotoEvento)
        tvNombreFoto = findViewById(R.id.tvNombreFoto)
        btnDeseleccionarFoto = findViewById(R.id.btnDeseleccionarFoto)
        btnSeleccionarUbicacion = findViewById(R.id.btnSeleccionarUbicacion)
        tvCoordenadas = findViewById(R.id.tvCoordenadasSeleccionadas)
        btnCrearEvento = findViewById(R.id.btnCrearEvento)

        etFecha.setOnClickListener { mostrarDatePicker() }
        btnSeleccionarFoto.setOnClickListener { imagePickerLauncher.launch("image/*") }
        btnDeseleccionarFoto.setOnClickListener { deseleccionarFoto() }
        btnSeleccionarUbicacion.setOnClickListener { locationPickerLauncher.launch(Intent(this, SeleccionarUbicacionActivity::class.java)) }
        btnCrearEvento.setOnClickListener { crearEvento() }
    }

    private fun mostrarDatePicker() {
        val calendario = fechaHoraSeleccionada
        val datePickerDialog = DatePickerDialog(this, {
                _, year, month, dayOfMonth ->
            fechaHoraSeleccionada.set(Calendar.YEAR, year)
            fechaHoraSeleccionada.set(Calendar.MONTH, month)
            fechaHoraSeleccionada.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            mostrarTimePicker()
        }, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun mostrarTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Selecciona la hora del evento")
            .build()

        picker.addOnPositiveButtonClickListener {
            fechaHoraSeleccionada.set(Calendar.HOUR_OF_DAY, picker.hour)
            fechaHoraSeleccionada.set(Calendar.MINUTE, picker.minute)

            val formatoUsuario = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
            etFecha.setText(formatoUsuario.format(fechaHoraSeleccionada.time))
        }

        picker.show(supportFragmentManager, "TimePicker")
    }

    private fun obtenerNombreArchivo(uri: Uri): String {
        var nombre = "archivo_seleccionado"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                nombre = cursor.getString(nameIndex)
            }
        }
        return nombre
    }

    private fun deseleccionarFoto() {
        fotoUri = null
        tvNombreFoto.text = ""
        tvNombreFoto.visibility = View.GONE
        btnDeseleccionarFoto.visibility = View.GONE
        btnSeleccionarFoto.text = "Añadir Foto"
    }

    private fun crearEvento() {
        if (etTitulo.text.isNullOrEmpty() || etOrganizadores.text.isNullOrEmpty() || etDescripcion.text.isNullOrEmpty() || etFecha.text.isNullOrEmpty() || latitudSeleccionada == null) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("sesion", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", null) ?: return

        lifecycleScope.launch {
            try {
                val formatoISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                val fechaISO = formatoISO.format(fechaHoraSeleccionada.time)

                val tituloPart = etTitulo.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val organizadoresPart = etOrganizadores.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val descripcionPart = etDescripcion.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val cupoMaximoPart = (etCupoMaximo.text.toString().toIntOrNull() ?: 50).toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val fechaPart = fechaISO.toRequestBody("text/plain".toMediaTypeOrNull())
                val latitudPart = latitudSeleccionada.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val longitudPart = longitudSeleccionada.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                var fotoPart: MultipartBody.Part? = null
                fotoUri?.let {
                    val file = uriToFile(it)
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    fotoPart = MultipartBody.Part.createFormData("foto", file.name, requestFile)
                }

                val response = RetrofitClient.eventosService.crearEvento(
                    token = "Bearer $token",
                    titulo = tituloPart,
                    organizadores = organizadoresPart,
                    descripcion = descripcionPart,
                    fecha = fechaPart,
                    latitud = latitudPart,
                    longitud = longitudPart,
                    cupoMaximo = cupoMaximoPart,
                    foto = fotoPart
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@CrearEventoActivity, response.body()?.mensaje ?: "Evento creado con éxito", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("CrearEventoActivity", "Error: $errorBody")
                    Toast.makeText(this@CrearEventoActivity, "Error: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("CrearEventoActivity", "Excepción: ${e.message}")
                Toast.makeText(this@CrearEventoActivity, "Excepción: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        return file
    }
}