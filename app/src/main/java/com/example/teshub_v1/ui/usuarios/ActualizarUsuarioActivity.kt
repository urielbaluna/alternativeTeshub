package com.example.teshub_v1.ui.usuarios

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ActualizarUsuarioActivity : AppCompatActivity() {

    private lateinit var ivPerfil: CircleImageView
    private lateinit var etNombre: TextInputEditText
    private lateinit var etApellido: TextInputEditText
    private lateinit var etCorreo: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    // Campos nuevos
    private lateinit var etCarrera: TextInputEditText
    private lateinit var etSemestre: TextInputEditText
    private lateinit var etBiografia: TextInputEditText
    private lateinit var etUbicacion: TextInputEditText

    private lateinit var btnGuardar: Button
    private var selectedImageUri: Uri? = null

    // Guardamos el correo original para comparar
    private var emailOriginal: String = ""

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ivPerfil.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actualizarusuario)

        // Referencias
        ivPerfil = findViewById(R.id.ivPerfilActualizar)
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCorreo = findViewById(R.id.etCorreo)
        etPassword = findViewById(R.id.etPassword)

        etCarrera = findViewById(R.id.etCarrera)
        etSemestre = findViewById(R.id.etSemestre)
        etBiografia = findViewById(R.id.etBiografia)
        etUbicacion = findViewById(R.id.etUbicacion)

        btnGuardar = findViewById(R.id.btnGuardar)

        cargarDatosActuales()

        ivPerfil.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    private fun cargarDatosActuales() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null)
        if (token == null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val perfil = RetrofitClient.usuariosService.getPerfil("Bearer $token")
                withContext(Dispatchers.Main) {
                    etNombre.setText(perfil.nombre)
                    etApellido.setText(perfil.apellido)
                    etCorreo.setText(perfil.correo)
                    emailOriginal = perfil.correo // Guardamos para validar después

                    // Cargar nuevos campos
                    etCarrera.setText(perfil.carrera)
                    etSemestre.setText(perfil.semestre)
                    etBiografia.setText(perfil.biografia)
                    etUbicacion.setText(perfil.ubicacion)

                    if (!perfil.imagen.isNullOrEmpty()) {
                        val baseUrl = if (BuildConfig.API_BASE_URL.endsWith("/")) BuildConfig.API_BASE_URL else "${BuildConfig.API_BASE_URL}/"
                        Glide.with(this@ActualizarUsuarioActivity)
                            .load(baseUrl + perfil.imagen)
                            .placeholder(R.drawable.ic_profile)
                            .into(ivPerfil)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ActualizarUsuarioActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun guardarCambios() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null) ?: return

        // Preparar partes de texto (RequestBody)
        val nombrePart = etNombre.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val apellidoPart = etApellido.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        // Lógica de correo: Si no cambió, mandamos null para evitar validación de seguridad (Error 400)
        val correoActual = etCorreo.text.toString().trim()
        val correoPart = if (correoActual != emailOriginal) {
            correoActual.toRequestBody("text/plain".toMediaTypeOrNull())
        } else {
            null
        }

        val carreraPart = etCarrera.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val semestrePart = etSemestre.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val bioPart = etBiografia.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val ubiPart = etUbicacion.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        // Password es opcional
        val passText = etPassword.text.toString()
        val passwordPart = if (passText.isNotEmpty()) {
            passText.toRequestBody("text/plain".toMediaTypeOrNull())
        } else null

        // Imagen es opcional
        var imagenPart: MultipartBody.Part? = null
        selectedImageUri?.let { uri ->
            val file = uriToFile(uri)
            val requestFile = file.readBytes().toRequestBody("image/jpeg".toMediaTypeOrNull())
            imagenPart = MultipartBody.Part.createFormData("imagen", file.name, requestFile)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // SOLUCIÓN DEL ERROR: Usamos argumentos nombrados
                val response = RetrofitClient.usuariosService.actualizarUsuario(
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

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ActualizarUsuarioActivity, response.mensaje, Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = if (correoPart != null)
                        "Error: Cambiar correo requiere verificación (no implementada aquí)"
                    else
                        "Error: ${e.message}"
                    Toast.makeText(this@ActualizarUsuarioActivity, errorMsg, Toast.LENGTH_LONG).show()
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