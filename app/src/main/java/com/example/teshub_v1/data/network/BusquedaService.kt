package com.example.teshub_v1.data.network

import com.example.teshub_v1.data.model.BusquedaGeneralResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query


interface BusquedaService {
    @GET("api/buscar")
    suspend fun buscarGeneral(
        @Header("Authorization") token: String,
        @Query("palabra") palabra: String
    ): BusquedaGeneralResponse
}