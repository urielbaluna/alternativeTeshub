package com.example.teshub_v1.data.model

import com.squareup.moshi.Json

data class BusquedaGeneralResponse(
    @Json(name = "eventos") val eventos: List<EventoBusquedaItem>,
    @Json(name = "publicaciones") val publicaciones: List<PublicacionBusquedaItem>,
    @Json(name = "perfiles") val perfiles: List<PerfilBusquedaItem>
)

data class PublicacionBusquedaItem(
    @Json(name = "id_publi") val idPubli: Int,
    val nombre: String,
    val descripcion: String,
    val autor: String,
    val fecha: String,
    @Json(name = "hace_cuanto") val haceCuanto: String,
    val tags: List<String>,
    val rating: String,
    val vistas: Int,
    @Json(name = "imagen_portada") val imagenPortada: String?
)

data class PerfilBusquedaItem(
    val matricula: String,
    val nombre: String,
    val apellido: String,
    val imagen: String?,
    val carrera: String?,
    val semestre: String?,
    val rol: String,
    val intereses: List<String>,
    val siguiendo: Boolean = false
)

data class EventoBusquedaItem(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val fecha: String,
    @Json(name = "hace_cuanto") val haceCuanto: String,
    val categoria: String,
    val imagen: String?,
    val inscrito: Boolean,
    val ubicacion: UbicacionEvento,
    val cupo: CupoEvento
)
data class UbicacionEvento(
    val nombre: String,
    val lat: String?,
    val lng: String?
)

data class CupoEvento(
    val maximo: Int,
    val registrados: Int
)