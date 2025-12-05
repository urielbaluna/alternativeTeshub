package com.example.teshub_v1.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.teshub_v1.R
import com.example.teshub_v1.ui.auth.ResetpasswordActivity
import com.example.teshub_v1.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class ForgotpasswordActivity : AppCompatActivity() {

    private var isRequestRunning = false
    private val PREFS = "teshub_prefs"
    private val KEY_LAST_REQUEST_TIME = "last_code_request_time"
    private val COOLDOWN_MS = 60 * 60 * 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val btnSend = findViewById<Button>(R.id.btnSendCode)

        btnSend.setOnClickListener {
            if (isRequestRunning) return@setOnClickListener

            val correo = etEmail.text.toString().trim()
            if (correo.isEmpty()) {
                Toast.makeText(this, "Ingresa tu correo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (estaEnCooldown(correo)) {
                Toast.makeText(this, "Ya se envió un código recientemente. Revisa tu correo.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            isRequestRunning = true

            solicitarCodigo(correo, btnSend)
        }
    }

    private fun solicitarCodigo(correo: String, btnSend: Button) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.usuariosService.solicitarCodigoContrasena(
                    mapOf("correo" to correo)
                )

                withContext(Dispatchers.Main) {
                    isRequestRunning = false
                    btnSend.isEnabled = true

                    guardarCooldown(correo)
                    Toast.makeText(
                        this@ForgotpasswordActivity,
                        response.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(
                        this@ForgotpasswordActivity,
                        ResetpasswordActivity::class.java
                    ).apply {
                        putExtra("correo", correo)
                    }
                    startActivity(intent)
                    finish()
                }

            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    isRequestRunning = false
                    btnSend.isEnabled = true

                    when (e.code()) {
                        400 -> Toast.makeText(
                            this@ForgotpasswordActivity,
                            "El correo es obligatorio o inválido.",
                            Toast.LENGTH_LONG
                        ).show()

                        404 -> Toast.makeText(
                            this@ForgotpasswordActivity,
                            "Correo no encontrado.",
                            Toast.LENGTH_LONG
                        ).show()

                        500 -> Toast.makeText(
                            this@ForgotpasswordActivity,
                            "Error del servidor.",
                            Toast.LENGTH_LONG
                        ).show()

                        else -> Toast.makeText(
                            this@ForgotpasswordActivity,
                            "Error (${e.code()}): Verifica tu conexión",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isRequestRunning = false
                    btnSend.isEnabled = true
                    Toast.makeText(
                        this@ForgotpasswordActivity,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun estaEnCooldown(correo: String): Boolean {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val lastTime = prefs.getLong(llaveCorreo(correo), 0L)
        return lastTime != 0L && (System.currentTimeMillis() - lastTime) < COOLDOWN_MS
    }

    private fun guardarCooldown(correo: String) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        prefs.edit()
            .putLong(llaveCorreo(correo), System.currentTimeMillis())
            .apply()
    }

    private fun llaveCorreo(correo: String) = "$KEY_LAST_REQUEST_TIME:$correo"
}