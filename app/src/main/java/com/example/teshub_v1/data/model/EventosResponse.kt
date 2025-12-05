package com.example.teshub_v1.data.model

import com.squareup.moshi.Json

data class EventosResponse(
    @Json(name = "eventos")
    val eventos: List<Evento>
)
