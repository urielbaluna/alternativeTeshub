package com.example.teshub_v1.data.network

import com.example.teshub_v1.data.model.AsesoradosResponse
import com.example.teshub_v1.data.model.GeneralResponse
import com.example.teshub_v1.data.model.MiAsesorResponse
import com.example.teshub_v1.data.model.ResponderSolicitudRequest
import com.example.teshub_v1.data.model.SolicitudesResponse
import retrofit2.Response
import retrofit2.http.*

interface AsesoriasService {

    // --- ESTUDIANTE ---

    @POST("api/asesorias/solicitar")
    suspend fun solicitarAsesoria(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<GeneralResponse>

    @GET("api/asesorias/mi-asesor")
    suspend fun obtenerMiAsesor(
        @Header("Authorization") token: String
    ): Response<MiAsesorResponse>

    // --- ASESOR ---

    @PUT("api/asesorias/responder")
    suspend fun responderSolicitud(
        @Header("Authorization") token: String,
        @Body body: ResponderSolicitudRequest
    ): Response<GeneralResponse>

    @GET("api/asesorias/pendientes")
    suspend fun listarSolicitudesPendientes(
        @Header("Authorization") token: String
    ): Response<SolicitudesResponse>

    @GET("api/asesorias/mis-alumnos")
    suspend fun listarMisAsesorados(
        @Header("Authorization") token: String
    ): Response<AsesoradosResponse>
}