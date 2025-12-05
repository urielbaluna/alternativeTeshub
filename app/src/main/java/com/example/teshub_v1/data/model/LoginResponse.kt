package com.example.teshub_v1.data.model

data class LoginResponse(
    val token: String,
    val matricula: String,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val rol: String
)

