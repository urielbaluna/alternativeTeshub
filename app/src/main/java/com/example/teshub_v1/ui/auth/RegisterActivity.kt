package com.example.teshub_v1.ui.auth

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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

    // Nuevas variables para el Rol y Código
    private lateinit var spinnerRol: Spinner
    private lateinit var tilCodigoAcceso: TextInputLayout
    private lateinit var etCodigoAcceso: TextInputEditText

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

        // Inicializar vistas existentes
        ivFotoPerfil = findViewById(R.id.ivFotoPerfil)
        val btnSeleccionarFoto = findViewById<MaterialButton>(R.id.btnSeleccionarFoto)
        val etNombre = findViewById<TextInputEditText>(R.id.etNombre)
        val etApellido = findViewById<TextInputEditText>(R.id.etApellido)
        val etCorreo = findViewById<TextInputEditText>(R.id.etCorreoRegistro)
        val etMatricula = findViewById<TextInputEditText>(R.id.etMatricula)
        val etPassword = findViewById<TextInputEditText>(R.id.etPasswordRegistro)
        val btnRegistrar = findViewById<MaterialButton>(R.id.btnRegistrar)
        val tvYaTengoCuenta = findViewById<TextView>(R.id.tvYaTengoCuenta)

        // Inicializar nuevas vistas (Asegúrate de haber actualizado el XML antes)
        spinnerRol = findViewById(R.id.spinnerRol)
        tilCodigoAcceso = findViewById(R.id.tilCodigoAcceso)
        etCodigoAcceso = findViewById(R.id.etCodigoAcceso)

        // Configurar Spinner de Roles
        val roles = arrayOf("Estudiante", "Asesor")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRol.adapter = adapter

        // Listener para mostrar/ocultar el campo de código
        spinnerRol.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 1) { // 1 = Asesor
                    tilCodigoAcceso.visibility = View.VISIBLE
                } else {
                    tilCodigoAcceso.visibility = View.GONE
                    etCodigoAcceso.text?.clear()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

            // Lógica de Rol
            val selectedPosition = spinnerRol.selectedItemPosition
            val rolEnvio = if (selectedPosition == 1) "2" else "3" // 2=Asesor, 3=Estudiante
            val codigoAcceso = etCodigoAcceso.text.toString().trim()

            // Validaciones básicas
            if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() ||
                matricula.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validación específica para Asesor
            if (rolEnvio == "2" && codigoAcceso.isEmpty()) {
                tilCodigoAcceso.error = "El código de invitación es requerido"
                return@setOnClickListener
            } else {
                tilCodigoAcceso.error = null
            }

            if (!correo.contains("@")) {
                Toast.makeText(this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar contraseña
            if (!validarContrasena(password)) {
                Toast.makeText(
                    this,
                    "La contraseña debe tener al menos 4 caracteres, mayúscula, minúscula, número y símbolo",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Llamar al registro
            btnRegistrar.isEnabled = false // Evitar doble clic
            CoroutineScope(Dispatchers.IO).launch {
                registrarUsuario(nombre, apellido, correo, matricula, password, rolEnvio, codigoAcceso)
                withContext(Dispatchers.Main) {
                    btnRegistrar.isEnabled = true
                }
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
        password: String,
        rol: String,          // Nuevo parámetro
        codigoAcceso: String  // Nuevo parámetro
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

            // Agregar todos los campos básicos
            addFormField("nombre", nombre)
            addFormField("apellido", apellido)
            addFormField("correo", correo)
            addFormField("matricula", matricula)
            addFormField("contrasena", password)

            // Agregar campos de rol y código
            addFormField("rol", rol)
            if (rol == "2" && codigoAcceso.isNotEmpty()) {
                addFormField("codigo_acceso", codigoAcceso)
            }

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
                        val mensaje = jsonResponse.optString("mensaje", "Usuario registrado correctamente")

                        Toast.makeText(this@RegisterActivity, mensaje, Toast.LENGTH_LONG).show()

                        // Si es asesor, no hacemos login automático porque podría estar pendiente o porque es un flujo distinto
                        // O bien, si es asesor validado (activo), podrías intentar login.
                        // Para simplificar, mandamos al login para que ingrese sus credenciales.
                        finish()

                    } catch (e: Exception) {
                        Toast.makeText(this@RegisterActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
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
}