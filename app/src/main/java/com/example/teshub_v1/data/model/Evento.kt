package com.example.teshub_v1.data.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class Evento(
    @Json(name = "id_evento") val id: Int,
    @Json(name = "titulo") val titulo: String,
    @Json(name = "fecha") val fecha: String,
    @Json(name = "descripcion") val descripcion: String,

    @Json(name = "url_foto") val urlFoto: String?, // Backend manda 'url_foto'

    val categoria: String? = "General",

    @Json(name = "ubicacion") val ubicacionObj: Ubicacion?,
    @Json(name = "ubicacion_nombre") val ubicacionNombre: String? = null,

    val tags: List<String>? = emptyList(),

    @Json(name = "organizadores") val organizadores: List<Organizador>,

    @Json(name = "cupo_maximo") val cupoMaximo: Int = 50, // Backend manda 'cupo_maximo'
    @Json(name = "asistentes_registrados") val asistentesRegistrados: Int = 0,
    @Json(name = "usuario_registrado") val usuarioRegistrado: Boolean = false,

    // Campos calculados o extras
    @Json(name = "inscrito") val inscrito: Boolean = false,

    // Banderas de rol (vienen de listarEventos)
    @Json(name = "es_organizador") val esOrganizador: Boolean = false,
    @Json(name = "es_asistente") val esAsistente: Boolean = false

) : Parcelable {

    val hayLugaresDisponibles: Boolean
        get() = asistentesRegistrados < cupoMaximo
}

@Parcelize
@JsonClass(generateAdapter = true)
data class Ubicacion(
    val latitud: Double,
    val longitud: Double
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class Organizador(
    val matricula: String,
    val nombre: String,
    val apellido: String?,
    val imagen: String?
) : Parcelable