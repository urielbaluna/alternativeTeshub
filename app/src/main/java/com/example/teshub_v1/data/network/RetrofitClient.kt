package com.example.teshub_v1.data.network

import com.example.teshub_v1.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {

    private const val BASE_URL = BuildConfig.API_BASE_URL

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val usuariosService: UsuariosService by lazy {
        retrofit.create(UsuariosService::class.java)
    }

    val publicacionesService: PublicacionesService by lazy {
        retrofit.create(PublicacionesService::class.java)
    }

    val eventosService: EventosService by lazy {
        retrofit.create(EventosService::class.java)
    }
}