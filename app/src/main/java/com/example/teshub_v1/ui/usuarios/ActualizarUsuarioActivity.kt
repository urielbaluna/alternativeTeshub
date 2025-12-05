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
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ActualizarUsuarioActivity : AppCompatActivity() {

    private lateinit var ivPerfil: CircleImageView
    private lateinit var etNombre: TextInputEditText
    private lateinit var etApellido: TextInputEditText
    private lateinit var etCorreo: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnGuardar: Button

    private var selectedImageUri: Uri? = null

    // Lanzador para abrir la galería
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                // Mostrar la nueva imagen seleccionada
                Glide.with(this).load(uri).into(ivPerfil)
            }
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
        btnGuardar = findViewById(R.id.btnGuardar)

        // 1. Cargar datos actuales
        cargarDatosActuales()

        // 2. Configurar clic en la imagen para cambiarla
        ivPerfil.setOnClickListener {
            abrirGaleria()
        }

        // 3. Guardar cambios
        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
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

        val nombre = etNombre.text.toString()
        val apellido = etApellido.text.toString()
        val correo = etCorreo.text.toString()
        val password = etPassword.text.toString()

        // Convertir a RequestBody
        val nombrePart = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
        val apellidoPart = apellido.toRequestBody("text/plain".toMediaTypeOrNull())
        val correoPart = correo.toRequestBody("text/plain".toMediaTypeOrNull())

        // Contraseña es opcional
        val passwordPart = if (password.isNotEmpty()) {
            password.toRequestBody("text/plain".toMediaTypeOrNull())
        } else null

        // Imagen es opcional
        var imagenPart: MultipartBody.Part? = null
        selectedImageUri?.let { uri ->
            val file = getFileFromUri(uri)
            if (file != null) {
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                imagenPart = MultipartBody.Part.createFormData("imagen", file.name, requestFile)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.usuariosService.actualizarUsuario(
                    "Bearer $token",
                    nombrePart,
                    apellidoPart,
                    correoPart,
                    passwordPart,
                    imagenPart
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ActualizarUsuarioActivity, response.mensaje, Toast.LENGTH_LONG).show()
                    finish() // Cierra la actividad y vuelve al perfil
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ActualizarUsuarioActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Función auxiliar para obtener archivo real desde URI
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val contentResolver = applicationContext.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(applicationContext.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}