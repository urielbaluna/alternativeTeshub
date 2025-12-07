package com.example.teshub_v1.ui.eventos

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.teshub_v1.BuildConfig
import com.example.teshub_v1.R
import com.example.teshub_v1.data.model.Evento
import com.example.teshub_v1.data.network.RetrofitClient
import com.example.teshub_v1.databinding.ActivityEventoDetalleBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class EventoDetalleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventoDetalleBinding
    private var eventoId: Int = 0
    private var eventoActual: Evento? = null
    private var esOrganizador = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventoDetalleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "" // Título vacío para que se vea la imagen

        eventoId = intent.getIntExtra("evento_id", 0)
        if (eventoId == 0) {
            Toast.makeText(this, "Error: Evento no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cargarDetalles()

        // Acción del Botón Principal (FAB)
        binding.fabAccionEvento.setOnClickListener {
            eventoActual?.let { evt ->
                if (esOrganizador) {
                    // Si es organizador, el FAB edita
                    val intent = Intent(this, EditarEventoActivity::class.java)
                    intent.putExtra("evento_id", evt.id)
                    startActivity(intent)
                } else {
                    // Si es usuario, registra o cancela
                    if (evt.usuarioRegistrado) cancelarRegistro() else registrarse()
                }
            }
        }

        // Acción Ver Mapa
        binding.btnVerMapa.setOnClickListener {
            eventoActual?.ubicacionObj?.let { ubi ->
                val uri = "geo:${ubi.latitud},${ubi.longitud}?q=${ubi.latitud},${ubi.longitud}(${eventoActual?.titulo})"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                intent.setPackage("com.google.android.apps.maps")
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) // Intento genérico
                }
            }
        }
    }

    private fun cargarDetalles() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null) ?: return
        val miMatricula = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("matricula", "")

        lifecycleScope.launch {
            try {
                // 1. La llamada ahora devuelve el wrapper
                val response = RetrofitClient.eventosService.getEvento(eventoId, "Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    // 2. Extraemos el objeto real usando .evento
                    val evento = response.body()!!.evento
                    eventoActual = evento

                    // Verificar si soy organizador
                    esOrganizador = evento.organizadores.any { it.matricula == miMatricula }
                    actualizarUI(evento)
                } else {
                    Toast.makeText(this@EventoDetalleActivity, "Error al cargar detalles", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                // Este Toast es el que veías antes
                Toast.makeText(this@EventoDetalleActivity, "Error de datos: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace() // Mira el Logcat para ver el error exacto si persiste
            }
        }
    }

    private fun actualizarUI(evento: Evento) {
        binding.tvDetalleTitulo.text = evento.titulo
        binding.tvDetalleDescripcion.text = evento.descripcion
        binding.tvDetalleUbicacion.text = evento.ubicacionNombre ?: "Ver ubicación en mapa"
        binding.tvDetalleCupo.text = "${evento.asistentesRegistrados}/${evento.cupoMaximo} Asistentes"

        // Organizadores
        val orgsNombres = evento.organizadores.joinToString("\n") { "• ${it.nombre} ${it.apellido ?: ""}" }
        binding.tvOrganizadoresLista.text = orgsNombres

        // Formato Fecha
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'", Locale.getDefault())
            val date = inputFormat.parse(evento.fecha)
            val outputFormat = SimpleDateFormat("EEEE, d 'de' MMMM • h:mm a", Locale.getDefault())
            binding.tvDetalleFecha.text = outputFormat.format(date ?: "")
        } catch (e: Exception) {
            binding.tvDetalleFecha.text = evento.fecha
        }

        // Imagen
        if (!evento.urlFoto.isNullOrEmpty()) {
            Glide.with(this)
                .load("${BuildConfig.API_BASE_URL}/${evento.urlFoto}")
                .placeholder(R.drawable.ic_image)
                .into(binding.ivDetalleImagen)
        }

        // Tags / Categoría
        binding.chipGroupTags.removeAllViews()
        // Agregar chip de categoría primero
        val chipCat = Chip(this).apply {
            text = evento.categoria ?: "Evento"
            setChipBackgroundColorResource(R.color.green_500) // Define en colors.xml o usa default
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
        }
        binding.chipGroupTags.addView(chipCat)

        // Agregar otros tags
        evento.tags?.forEach { tag ->
            val chip = Chip(this).apply { text = tag }
            binding.chipGroupTags.addView(chip)
        }

        // Configurar Botón FAB
        if (esOrganizador) {
            binding.fabAccionEvento.text = "Editar Evento"
            binding.fabAccionEvento.setIconResource(R.drawable.ic_edit)
            binding.fabAccionEvento.backgroundTintList = ContextCompat.getColorStateList(this, R.color.blue_500) // Define color azul
            invalidateOptionsMenu() // Mostrar menú borrar
        } else {
            if (evento.usuarioRegistrado) {
                binding.fabAccionEvento.text = "Cancelar Registro"
                binding.fabAccionEvento.setIconResource(R.drawable.ic_close) // Usa ic_close o ic_cancel
                binding.fabAccionEvento.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
            } else {
                if (evento.hayLugaresDisponibles) {
                    binding.fabAccionEvento.text = "Registrarme"
                    binding.fabAccionEvento.setIconResource(R.drawable.ic_add)
                    binding.fabAccionEvento.backgroundTintList = ContextCompat.getColorStateList(this, R.color.green_500)
                } else {
                    binding.fabAccionEvento.text = "Cupo Lleno"
                    binding.fabAccionEvento.isEnabled = false
                    binding.fabAccionEvento.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
                }
            }
        }
    }

    private fun registrarse() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null) ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.eventosService.registrarseEvento(eventoId, "Bearer $token")
                if (response.isSuccessful) {
                    Toast.makeText(this@EventoDetalleActivity, "¡Registrado con éxito!", Toast.LENGTH_SHORT).show()
                    cargarDetalles() // Recargar para actualizar botón
                } else {
                    Toast.makeText(this@EventoDetalleActivity, "Error al registrarse", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EventoDetalleActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelarRegistro() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null) ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.eventosService.cancelarRegistroEvento(eventoId, "Bearer $token")
                if (response.isSuccessful) {
                    Toast.makeText(this@EventoDetalleActivity, "Registro cancelado", Toast.LENGTH_SHORT).show()
                    cargarDetalles()
                } else {
                    Toast.makeText(this@EventoDetalleActivity, "Error al cancelar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EventoDetalleActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Menú superior (Solo para eliminar si es organizador)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (esOrganizador) {
            menu?.add(0, 1, 0, "Eliminar Evento")
                ?.setIcon(R.drawable.ic_delete)
                ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        if (item.itemId == 1) confirmarEliminar()
        return super.onOptionsItemSelected(item)
    }

    private fun confirmarEliminar() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Evento")
            .setMessage("¿Estás seguro? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> eliminarEvento() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarEvento() {
        val token = getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("token", null) ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.eventosService.eliminarEvento(eventoId, "Bearer $token")
                if (response.isSuccessful) {
                    Toast.makeText(this@EventoDetalleActivity, "Evento eliminado", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EventoDetalleActivity, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EventoDetalleActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        }
    }
}