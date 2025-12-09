package com.example.teshub_v1.ui.usuarios

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.ActualizarInteresesRequest
import com.example.teshub_v1.data.model.Interes
import com.example.teshub_v1.data.network.RetrofitClient
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ActualizarUsuarioActivity : AppCompatActivity() {

    // Vistas de Perfil
    private lateinit var ivPerfil: CircleImageView
    private lateinit var etNombre: TextInputEditText
    private lateinit var etApellido: TextInputEditText
    private lateinit var etCorreo: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etCarrera: TextInputEditText
    private lateinit var etSemestre: TextInputEditText
    private lateinit var etBiografia: TextInputEditText
    private lateinit var etUbicacion: TextInputEditText

    // Vistas de Intereses (NUEVO)
    private lateinit var chipGroupIntereses: ChipGroup

    private lateinit var btnGuardar: Button
    private var selectedImageUri: Uri? = null
    private var emailOriginal: String = ""

    // Selector de Imagen
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ivPerfil.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actualizarusuario)

        initViews()
        cargarDatosCompletos() // Ahora carga perfil + intereses

        ivPerfil.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnGuardar.setOnClickListener {
            guardarTodo()
        }
    }

    private fun initViews() {
        ivPerfil = findViewById(R.id.ivPerfilActualizar)
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)
        etCarrera = findViewById(R.id.etCarrera)
        etSemestre = findViewById(R.id.etSemestre)
        etBiografia = findViewById(R.id.etBiografia)
        etUbicacion = findViewById(R.id.etUbicacion)
        chipGroupIntereses = findViewById(R.id.chipGroupIntereses) // Asegúrate que este ID exista en tu XML
        btnGuardar = findViewById(R.id.btnGuardar)
    }

    private fun cargarDatosCompletos() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null)
        if (token == null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Lanzamos las dos peticiones en paralelo (más rápido)
                val perfilDeferred = async { RetrofitClient.usuariosService.getPerfil("Bearer $token") }
                val catalogoDeferred = async { RetrofitClient.usuariosService.getCatalogoIntereses("Bearer $token") }

                val responsePerfil = perfilDeferred.await()
                val responseCatalogo = catalogoDeferred.await()

                withContext(Dispatchers.Main) {
                    // 2. Procesar PERFIL
                    if (responsePerfil.isSuccessful && responsePerfil.body() != null) {
                        val perfil = responsePerfil.body()!!

                        etNombre.setText(perfil.nombre)
                        etApellido.setText(perfil.apellido)
                        etCorreo.setText(perfil.correo)
                        emailOriginal = perfil.correo
                        etCarrera.setText(perfil.carrera ?: "")
                        etSemestre.setText(perfil.semestre ?: "")
                        etBiografia.setText(perfil.biografia ?: "")
                        etUbicacion.setText(perfil.ubicacion ?: "")

                        if (!perfil.imagen.isNullOrEmpty()) {
                            val baseUrl = BuildConfig.API_BASE_URL.removeSuffix("/") + "/"
                            Glide.with(this@ActualizarUsuarioActivity)
                                .load(baseUrl + perfil.imagen)
                                .placeholder(R.drawable.ic_profile)
                                .into(ivPerfil)
                        }

                        // 3. Procesar INTERESES (Solo si el catálogo cargó bien)
                        if (responseCatalogo.isSuccessful && responseCatalogo.body() != null) {
                            val catalogo = responseCatalogo.body()!!
                            // Extraemos los IDs que el usuario YA tiene
                            val misInteresesIds = perfil.intereses.map { it.id_interes }

                            poblarChipGroup(catalogo, misInteresesIds)
                        }
                    } else {
                        Toast.makeText(this@ActualizarUsuarioActivity, "Error al cargar perfil", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ActualizarUsuarioActivity, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun poblarChipGroup(catalogo: List<Interes>, misInteresesIds: List<Int>) {
        chipGroupIntereses.removeAllViews()

        for (item in catalogo) {
            val chip = Chip(this)
            chip.text = item.nombre
            chip.tag = item.id_interes
            chip.isCheckable = true
            chip.isClickable = true
            chip.isChecked = misInteresesIds.contains(item.id_interes)
            chip.setChipBackgroundColorResource(R.color.chip_background_selector)

            chip.setTextColor(getColorStateList(R.color.selector_chip_texto))
            chip.chipStrokeWidth = 0f
            chip.setChipStrokeColorResource(android.R.color.darker_gray)

            chipGroupIntereses.addView(chip)
        }
    }

    private fun guardarTodo() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null) ?: return
        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

        // --- A. Preparar Datos Perfil ---
        val nombrePart = etNombre.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val apellidoPart = etApellido.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val correoActual = etCorreo.text.toString().trim()
        val correoPart = if (correoActual != emailOriginal) {
            correoActual.toRequestBody("text/plain".toMediaTypeOrNull())
        } else null

        val carreraPart = etCarrera.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val semestrePart = etSemestre.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val bioPart = etBiografia.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val ubiPart = etUbicacion.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val passText = etPassword.text.toString()
        val passwordPart = if (passText.isNotEmpty()) passText.toRequestBody("text/plain".toMediaTypeOrNull()) else null

        var imagenPart: MultipartBody.Part? = null
        selectedImageUri?.let { uri ->
            val file = uriToFile(uri)
            val requestFile = file.readBytes().toRequestBody("image/jpeg".toMediaTypeOrNull())
            imagenPart = MultipartBody.Part.createFormData("imagen", file.name, requestFile)
        }

        // --- B. Preparar Datos Intereses ---
        val selectedIds = mutableListOf<Int>()
        for (i in 0 until chipGroupIntereses.childCount) {
            val chip = chipGroupIntereses.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedIds.add(chip.tag as Int)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Actualizar Datos Personales
                val responsePerfil = RetrofitClient.usuariosService.actualizarUsuario(
                    token = "Bearer $token",
                    nombre = nombrePart,
                    apellido = apellidoPart,
                    correo = correoPart,
                    contrasena = passwordPart,
                    carrera = carreraPart,
                    semestre = semestrePart,
                    biografia = bioPart,
                    ubicacion = ubiPart,
                    imagen = imagenPart
                )

                // 2. Actualizar Intereses
                val requestIntereses = ActualizarInteresesRequest(selectedIds)
                val responseIntereses = RetrofitClient.usuariosService.actualizarMisIntereses("Bearer $token", requestIntereses)

                withContext(Dispatchers.Main) {
                    // Verificamos si al menos una de las dos cosas funcionó o ambas
                    if (responseIntereses.isSuccessful) {
                        Toast.makeText(this@ActualizarUsuarioActivity, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ActualizarUsuarioActivity, "Datos guardados, pero hubo error en intereses", Toast.LENGTH_LONG).show()
                        finish() // Cerramos igual porque los datos principales se guardaron (asumiendo que responsePerfil no lanza excepción)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = if (correoPart != null) "Error: Cambiar correo puede requerir verificación" else "Error: ${e.message}"
                    Toast.makeText(this@ActualizarUsuarioActivity, msg, Toast.LENGTH_LONG).show()
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar Cambios"
                }
            }
        }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)!!
        val tempFile = File(cacheDir, "temp_profile_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        return tempFile
    }
}