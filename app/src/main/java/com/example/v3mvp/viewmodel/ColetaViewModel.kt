package com.example.v3mvp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.v3mvp.data.AppDatabase
import com.example.v3mvp.model.Coleta
import kotlinx.coroutines.launch

class ColetaViewModel(app: Application) : AndroidViewModel(app) {

    private val coletaDao = AppDatabase.getInstance(app).coletaDao()

    val coletas: LiveData<List<Coleta>> = coletaDao.observarTodas()

    fun limparColetas() {
        viewModelScope.launch {
            coletaDao.deletarTodas()
        }
    }

    fun inserirColetasTeste() {
        viewModelScope.launch {
            val teste = listOf(
                Coleta(
                    timestamp = System.currentTimeMillis(),
                    latitude = -8.06,
                    longitude = -34.89,
                    gyroX = 1.1f,
                    gyroY = 2.2f,
                    gyroZ = 3.3f,
                    deviceId = "TEST-DEVICE-001",
                    fotoPath = null
                ),
                Coleta(
                    timestamp = System.currentTimeMillis(),
                    latitude = -8.07,
                    longitude = -34.88,
                    gyroX = 1.2f,
                    gyroY = 2.3f,
                    gyroZ = 3.4f,
                    deviceId = "TEST-DEVICE-001",
                    fotoPath = null
                ),
            )
            teste.forEach { coletaDao.inserir(it) }
            // também não precisa chamar carregarColetas()
        }
    }
}

