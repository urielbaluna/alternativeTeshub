package com.example.teshub_v1.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.teshub_v1.R
import com.example.teshub_v1.data.network.RetrofitClient
import com.example.teshub_v1.ui.home.HomeContainerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContentView(R.layout.activity_main)

        val token = getSharedPreferences("sesion", MODE_PRIVATE).getString("token", null)
        if (token != null) {
            irAlHome()
            return
        }

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvRegistrarse = findViewById<TextView>(R.id.tvRegistrarse)

        btnLogin.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (usuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            CoroutineScope(Dispatchers.IO).launch {
                loginUsuario(usuario, password, btnLogin)
            }
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotpasswordActivity::class.java))
        }

        tvRegistrarse.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private suspend fun loginUsuario(usuario: String, password: String, btnLogin: Button) {
        try {
            val loginRequest = mapOf("correo" to usuario, "contrasena" to password)
            val response = RetrofitClient.usuariosService.login(loginRequest)

            withContext(Dispatchers.Main) {
                btnLogin.isEnabled = true

                // --- CORRECCIÓN: Guardar también la matrícula ---
                getSharedPreferences("sesion", MODE_PRIVATE).edit()
                    .putString("token", response.token)
                    .putString("matricula", response.matricula) // <-- LÍNEA AÑADIDA
                    .apply()

                Toast.makeText(this@MainActivity, "Bienvenido ${response.nombre}", Toast.LENGTH_SHORT).show()

                irAlHome()
            }

        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorMessage = try {
                JSONObject(errorBody).optString("mensaje", "Credenciales incorrectas")
            } catch (jsonE: Exception) {
                "Error de servidor: ${e.code()}"
            }

            withContext(Dispatchers.Main) {
                btnLogin.isEnabled = true
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                Log.e("LoginError", "HTTP ${e.code()}: $errorMessage")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                btnLogin.isEnabled = true
                Toast.makeText(this@MainActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("LoginError", e.stackTraceToString())
            }
        }
    }

    private fun irAlHome() {
        val intent = Intent(this, HomeContainerActivity::class.java)
        startActivity(intent)
        finish()
    }
}
