package com.example.teshub_v1.data.network

import com.example.teshub_v1.data.model.GeneralResponse
import com.example.teshub_v1.data.model.Interes
import com.example.teshub_v1.data.model.LoginResponse
import com.example.teshub_v1.data.model.PerfilResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface UsuariosService {

    // --- AUTENTICACIÓN ---
    @POST("api/usuarios/login")
    suspend fun login(
        @Body loginRequest: Map<String, String>
    ): LoginResponse

    @POST("api/usuarios/codigo-contrasena")
    suspend fun solicitarCodigoContrasena(
        @Body body: Map<String, String>
    ): GeneralResponse

    @PUT("api/usuarios/actualizar-contrasena")
    suspend fun actualizarContrasena(
        @Body body: Map<String, String>
    ): GeneralResponse

    @POST("api/usuarios/eliminar")
    suspend fun eliminarCuenta(
        @Header("Authorization") token: String,
        @Body body: Map<String, String> = emptyMap()
    ): GeneralResponse

    // --- PERFIL Y USUARIO ---
    @GET("api/usuarios/ver-info")
    suspend fun getPerfil(
        @Header("Authorization") token: String
    ): PerfilResponse

    @Multipart
    @PUT("api/usuarios/actualizar")
    suspend fun actualizarUsuario(
        @Header("Authorization") token: String,
        @Part("nombre") nombre: RequestBody?,
        @Part("apellido") apellido: RequestBody?,
        @Part("correo") correo: RequestBody?,
        @Part("contrasena") contrasena: RequestBody? = null,
        // Agregamos los nuevos campos para que el perfil sea editable completo
        @Part("carrera") carrera: RequestBody? = null,
        @Part("semestre") semestre: RequestBody? = null,
        @Part("biografia") biografia: RequestBody? = null,
        @Part("ubicacion") ubicacion: RequestBody? = null,
        @Part imagen: MultipartBody.Part? = null
    ): GeneralResponse

    // --- INTERESES Y NETWORKING (NUEVO) ---

    // Obtener catálogo de intereses disponibles
    @GET("api/usuarios/intereses/catalogo")
    suspend fun getCatalogoIntereses(): List<Interes>

    // Guardar mis intereses seleccionados
    @POST("api/usuarios/intereses/actualizar")
    suspend fun actualizarMisIntereses(
        @Header("Authorization") token: String,
        @Body body: Map<String, List<Int>>
    ): GeneralResponse

    // Obtener sugerencias de conexión
    @GET("api/usuarios/sugerencias")
    suspend fun obtenerSugerencias(
        @Header("Authorization") token: String
    ): List<PerfilResponse>

    // Conectar (Seguir) a un usuario
    @POST("api/usuarios/conectar")
    suspend fun conectarUsuario(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): GeneralResponse

    @POST("api/usuarios/solicitar-codigo")
    suspend fun solicitarCodigoSesion(
        @Header("Authorization") token: String
    ): GeneralResponse

    @Multipart
    @POST("api/usuarios/registrar")
    suspend fun registrarUsuario(
        @Part("matricula") matricula: RequestBody,
        @Part("nombre") nombre: RequestBody,
        @Part("apellido") apellido: RequestBody,
        @Part("correo") correo: RequestBody,
        @Part("contrasena") contrasena: RequestBody,
        @Part("rol") rol: RequestBody,
        @Part("codigo_acceso") codigo: RequestBody?,
        @Part imagen: MultipartBody.Part?
    ): GeneralResponse

    @POST("api/usuarios/ver-info-publicaciones")
    suspend fun verPerfilUsuario(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): com.example.teshub_v1.data.model.PublicacionesUsuarioResponse

    @GET("api/usuarios/conexiones")
    suspend fun obtenerMisConexiones(
        @Header("Authorization") token: String
    ): List<PerfilResponse>
}