package com.example.teshub_v1.data.model

// --- CLASE PARA LA PETICIÓN DE EDICIÓN ---
data class EditarEventoRequest(
    val titulo: String,
    val descripcion: String,
    val fecha: String,
    val latitud: Double,
    val longitud: Double,
    val organizadores: String, // String de matrículas separadas por coma
    val cupo_maximo: Int? = null
)
