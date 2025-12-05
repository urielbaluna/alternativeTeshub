package com.example.teshub_v1.data.network

import com.example.teshub_v1.data.model.LoginResponse
import com.example.teshub_v1.data.model.PerfilResponse
import com.example.teshub_v1.data.model.GeneralResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface UsuariosService {
    @POST("api/usuarios/login")
    suspend fun login(
        @Body loginRequest: Map<String, String>
    ): LoginResponse

    @GET("api/usuarios/ver-info")
    suspend fun getPerfil(
        @Header("Authorization") token: String
    ): PerfilResponse

    @POST("api/usuarios/codigo-contrasena")
    suspend fun solicitarCodigoContrasena(
        @Body body: Map<String, String>
    ): GeneralResponse

    @PUT("api/usuarios/actualizar-contrasena")
    suspend fun actualizarContrasena(
        @Body body: Map<String, String>
    ): GeneralResponse

    @Multipart
    @PUT("api/usuarios/actualizar")
    suspend fun actualizarUsuario(
        @Header("Authorization") token: String,
        @Part("nombre") nombre: RequestBody?,
        @Part("apellido") apellido: RequestBody?,
        @Part("correo") correo: RequestBody?,
        @Part("contrasena") contrasena: RequestBody? = null,
        @Part imagen: MultipartBody.Part? = null
    ): GeneralResponse

    @POST("api/usuarios/eliminar")
    suspend fun eliminarCuenta(
        @Header("Authorization") token: String,
        @Body body: Map<String, String> = emptyMap()
    ): GeneralResponse

}