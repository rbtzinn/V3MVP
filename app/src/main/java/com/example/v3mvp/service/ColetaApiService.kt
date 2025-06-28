package com.example.v3mvp.service

import com.example.v3mvp.model.Coleta
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class PayloadColeta(
    val coletas: List<Coleta>
)

interface ColetaApiService {
    @POST("/coleta/sync")
    suspend fun enviarColetas(@Body payload: PayloadColeta): retrofit2.Response<ResponseBody>
}

object RetrofitInstance {
    val api: ColetaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://192.168.0.12:8080/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ColetaApiService::class.java)
    }
}
