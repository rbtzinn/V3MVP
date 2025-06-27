package com.example.v3mvp.service

import com.example.v3mvp.model.Coleta
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class PayloadColeta(
    val coletas: List<Coleta>
)

interface ColetaApiService {
    @POST("/coleta/sync") // coloque seu endpoint real aqui
    suspend fun enviarColetas(@Body payload: PayloadColeta): retrofit2.Response<Void>
}

object RetrofitInstance {
    val api: ColetaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://SUA_API_AQUI.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ColetaApiService::class.java)
    }
}
