package com.example.teshub_v1.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class Evento(
    @Json(name = "id") val id: Int,
    @Json(name = "titulo") val titulo: String,
    @Json(name = "fecha") val fecha: String,
    @Json(name = "descripcion") val descripcion: String,
    @Json(name = "urlFoto") val urlFoto: String?,
    @Json(name = "ubicacion") val ubicacion: Ubicacion,
    @Json(name = "organizadores") val organizadores: List<Organizador>,
    @Json(name = "cupoMaximo") val cupoMaximo: Int = 50,
    @Json(name = "asistentesRegistrados") val asistentesRegistrados: Int = 0,
    @Json(name = "usuarioRegistrado") val usuarioRegistrado: Boolean = false
) : Parcelable {
    /**
     * Función helper para mostrar los nombres completos de los organizadores
     * en un solo string, separados por comas.
     */
    fun organizadoresTexto(): String {
        return organizadores.joinToString(separator = ", ") { "${it.nombre} ${it.apellido}" }
    }

    /**
     * Calcula los lugares disponibles
     */
    val cupoDisponible: Int
        get() = cupoMaximo - asistentesRegistrados

    /**
     * Verifica si hay lugares disponibles
     */
    val hayLugaresDisponibles: Boolean
        get() = cupoDisponible > 0

    /**
     * Retorna un texto formateado para mostrar la asistencia
     */
    fun textoAsistencia(): String {
        return "$asistentesRegistrados/$cupoMaximo asistentes"
    }
}

@Parcelize
@JsonClass(generateAdapter = true)
data class Ubicacion(
    @Json(name = "latitud") val latitud: Double,
    @Json(name = "longitud") val longitud: Double
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class Organizador(
    @Json(name = "matricula") val matricula: String,
    @Json(name = "nombre") val nombre: String,
    // --- CAMPO AÑADIDO ---
    @Json(name = "apellido") val apellido: String? // Se marca como opcional para evitar crashes si la API no lo envía
) : Parcelable
