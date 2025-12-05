package com.example.teshub_v1.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.teshub_v1.R
import com.example.teshub_v1.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException

class ResetpasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val correo = intent.getStringExtra("correo") ?: ""

        val etCodigo = findViewById<EditText>(R.id.etCodigo)
        val etNueva = findViewById<EditText>(R.id.etNuevaContrasena)
        val etRepetir = findViewById<EditText>(R.id.etRepetirContrasena)
        val btnActualizar = findViewById<Button>(R.id.btnActualizar)

        btnActualizar.setOnClickListener {
            val codigo = etCodigo.text.toString().replace("\\s".toRegex(), "")
            val nueva = etNueva.text.toString()
            val repetir = etRepetir.text.toString()

            if (correo.isEmpty()) {
                Toast.makeText(this, "Falta el correo. Vuelve a solicitar el código.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (codigo.isEmpty() || nueva.isEmpty() || repetir.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (codigo.length != 6 || !codigo.all { it.isDigit() }) {
                Toast.makeText(this, "El código debe ser de 6 dígitos numéricos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (nueva != repetir) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!cumplePolitica(nueva)) {
                Toast.makeText(this, "La contraseña debe tener mayúscula, minúscula, número, caracter especial y mínimo 4 caracteres.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            btnActualizar.isEnabled = false

            actualizarContrasena(correo, codigo, nueva, btnActualizar)
        }
    }

    private fun actualizarContrasena(correo: String, codigo: String, nueva: String, btn: Button) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = mapOf(
                    "correo" to correo,
                    "codigo" to codigo,
                    "nuevaContrasena" to nueva
                )

                val response = RetrofitClient.usuariosService.actualizarContrasena(body)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ResetpasswordActivity, response.mensaje, Toast.LENGTH_LONG)
                        .show()

                    val intent = Intent(this@ResetpasswordActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }

            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val mensajeServidor = try {
                    JSONObject(errorBody).optString("mensaje", "")
                } catch (ex: Exception) { "" }

                withContext(Dispatchers.Main) {
                    btn.isEnabled = true

                    when (e.code()) {
                        400 -> {
                            if (mensajeServidor.contains("Código", true)) {
                                Toast.makeText(
                                    this@ResetpasswordActivity,
                                    "Código inválido o expirado.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@ResetpasswordActivity,
                                    mensajeServidor.ifEmpty { "Datos inválidos." },
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        404 -> Toast.makeText(
                            this@ResetpasswordActivity,
                            "Usuario no encontrado.",
                            Toast.LENGTH_LONG
                        ).show()

                        500 -> Toast.makeText(
                            this@ResetpasswordActivity,
                            "Error del servidor.",
                            Toast.LENGTH_LONG
                        ).show()

                        else -> Toast.makeText(
                            this@ResetpasswordActivity,
                            "Error (${e.code()}): $mensajeServidor",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btn.isEnabled = true
                    Toast.makeText(
                        this@ResetpasswordActivity,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun cumplePolitica(pwd: String): Boolean {
        val tieneMayus = pwd.any { it.isUpperCase() }
        val tieneMinus = pwd.any { it.isLowerCase() }
        val tieneNum = pwd.any { it.isDigit() }
        val tieneEspecial = pwd.any { !it.isLetterOrDigit() }
        return pwd.length >= 4 && tieneMayus && tieneMinus && tieneNum && tieneEspecial
    }
}