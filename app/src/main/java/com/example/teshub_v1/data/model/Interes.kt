package com.example.teshub_v1.data.model
import com.squareup.moshi.Json

data class Interes(
    @Json(name = "id_interes") val id_interes: Int, // O usa 'id' si prefieres, pero debe coincidir en todo tu código
    val nombre: String
)