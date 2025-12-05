package com.example.teshub_v1.ui.auth

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class RegisterActivity : AppCompatActivity() {

    private var fotoPerfilUri: Uri? = null
    private lateinit var ivFotoPerfil: ImageView

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            fotoPerfilUri = it
            ivFotoPerfil.setImageURI(it)
            ivFotoPerfil.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        val btnSeleccionarFoto = findViewById<MaterialButton>(R.id.btnSeleccionarFoto)
        val etNombre = findViewById<TextInputEditText>(R.id.etNombre)
        val etApellido = findViewById<TextInputEditText>(R.id.etApellido)
        val etCorreo = findViewById<TextInputEditText>(R.id.etCorreoRegistro)
        val etMatricula = findViewById<TextInputEditText>(R.id.etMatricula)
        val etPassword = findViewById<TextInputEditText>(R.id.etPasswordRegistro)
        val btnRegistrar = findViewById<MaterialButton>(R.id.btnRegistrar)
        val tvYaTengoCuenta = findViewById<TextView>(R.id.tvYaTengoCuenta)

        // Botón Seleccionar Foto
        btnSeleccionarFoto.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Botón Registrar
        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val matricula = etMatricula.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validaciones
            if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() ||
                matricula.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!correo.contains("@")) {
                Toast.makeText(this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar contraseña
            if (!validarContrasena(password)) {
                Toast.makeText(
                    this,
                    "La contraseña no cumple los requisitos",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Llamar al registro
            CoroutineScope(Dispatchers.IO).launch {
                registrarUsuario(nombre, apellido, correo, matricula, password)
            }
        }

        // Link: Ya tengo cuenta
        tvYaTengoCuenta.setOnClickListener {
            finish()
        }
    }

    private fun validarContrasena(contrasena: String): Boolean {
        val tieneMayuscula = contrasena.any { it.isUpperCase() }
        val tieneMinuscula = contrasena.any { it.isLowerCase() }
        val tieneNumero = contrasena.any { it.isDigit() }
        val tieneEspecial = contrasena.any { !it.isLetterOrDigit() }
        val longitudMinima = contrasena.length >= 4

        return tieneMayuscula && tieneMinuscula && tieneNumero && tieneEspecial && longitudMinima
    }

    private suspend fun registrarUsuario(
        nombre: String,
        apellido: String,
        correo: String,
        matricula: String,
        password: String
    ) {
        try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val lineEnd = "\r\n"
            val twoHyphens = "--"

            val url = URL("${BuildConfig.API_BASE_URL}/api/usuarios/registrar")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.doOutput = true
            connection.doInput = true

            val outputStream = DataOutputStream(connection.outputStream)

            // Función helper para agregar campos de texto
            fun addFormField(fieldName: String, value: String) {
                outputStream.writeBytes(twoHyphens + boundary + lineEnd)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"$lineEnd")
                outputStream.writeBytes(lineEnd)
                outputStream.writeBytes(value)
                outputStream.writeBytes(lineEnd)
            }

            // Agregar todos los campos
            addFormField("nombre", nombre)
            addFormField("apellido", apellido)
            addFormField("correo", correo)
            addFormField("matricula", matricula)
            addFormField("contrasena", password)

            // Agregar foto si existe
            fotoPerfilUri?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val file = File(cacheDir, "temp_perfil_${System.currentTimeMillis()}.jpg")
                    val fileOutputStream = FileOutputStream(file)
                    inputStream?.copyTo(fileOutputStream)
                    inputStream?.close()
                    fileOutputStream.close()

                    val fileBytes = file.readBytes()
                    outputStream.writeBytes(twoHyphens + boundary + lineEnd)
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"imagen\"; filename=\"${file.name}\"$lineEnd")
                    outputStream.writeBytes("Content-Type: image/jpeg$lineEnd")
                    outputStream.writeBytes(lineEnd)
                    outputStream.write(fileBytes)
                    outputStream.writeBytes(lineEnd)

                    file.delete()
                } catch (e: Exception) {
                    Log.e("RegisterActivity", "Error al adjuntar foto: ${e.message}")
                }
            }

            // Cerrar multipart
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
            outputStream.flush()
            outputStream.close()

            val responseCode = connection.responseCode
            val responseText = try {
                val stream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                stream.bufferedReader().use(BufferedReader::readText)
            } catch (e: Exception) {
                ""
            }

            Log.d("RegisterDebug", "Código HTTP: $responseCode")
            Log.d("RegisterDebug", "Respuesta: $responseText")

            withContext(Dispatchers.Main) {
                if (responseCode in 200..299) {
                    try {
                        val jsonResponse = JSONObject(responseText)
                        val mensaje =
                            jsonResponse.optString("mensaje", "Usuario registrado correctamente")

                        Toast.makeText(this@RegisterActivity, mensaje, Toast.LENGTH_SHORT).show()

                        // Ahora debes hacer login para obtener el token
                        loginDespuesDeRegistro(correo, password)

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registro exitoso. Ahora inicia sesión.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                } else {
                    val mensajeError = try {
                        JSONObject(responseText).optString("mensaje", responseText)
                    } catch (e: Exception) {
                        "Error al registrar usuario"
                    }
                    Toast.makeText(this@RegisterActivity, mensajeError, Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e("RegisterException", e.stackTraceToString())
            }
        }
    }

    // Función para hacer login automático después del registro
    private suspend fun loginDespuesDeRegistro(correo: String, password: String) {
        Toast.makeText(this, "Registro exitoso. Ahora inicia sesión.", Toast.LENGTH_LONG).show()
        finish()
    }
}