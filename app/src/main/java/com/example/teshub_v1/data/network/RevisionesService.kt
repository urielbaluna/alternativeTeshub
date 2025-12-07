package com.example.teshub_v1.data.network

import com.example.teshub_v1.data.model.GeneralResponse
import com.example.teshub_v1.data.model.HistorialResponse
import com.example.teshub_v1.data.model.PendientesResponse
import com.example.teshub_v1.data.model.RevisionRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface RevisionesService {

    // (Asesor) Ver qué tesis tengo que revisar
    @GET("api/revisiones/pendientes")
    suspend fun obtenerPendientes(
        @Header("Authorization") token: String
    ): Response<PendientesResponse>

    // (Asesor) Enviar veredicto
    @POST("api/revisiones/revisar")
    suspend fun revisarPublicacion(
        @Header("Authorization") token: String,
        @Body body: RevisionRequest
    ): Response<GeneralResponse>

    // (Ambos) Ver historial de cambios
    @GET("api/revisiones/historial/{id_publi}")
    suspend fun obtenerHistorial(
        @Header("Authorization") token: String,
        @Path("id_publi") idPubli: Int
    ): Response<HistorialResponse>
}