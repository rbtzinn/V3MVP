package com.example.v3mvp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.v3mvp.data.AppDatabase
import com.example.v3mvp.model.Coleta
import kotlinx.coroutines.launch

class ColetaViewModel(app: Application) : AndroidViewModel(app) {

    private val coletaDao = AppDatabase.getInstance(app).coletaDao()
    private val _coletas = MutableLiveData<List<Coleta>>()
    val coletas: LiveData<List<Coleta>> = _coletas

    fun carregarColetas() {
        viewModelScope.launch {
            _coletas.value = coletaDao.buscarTodas()
        }
    }
    fun limparColetas() {
        viewModelScope.launch {
            coletaDao.deletarTodas()
            carregarColetas()
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
                    deviceId = "TEST-DEVICE-001"
                ),
                Coleta(
                    timestamp = System.currentTimeMillis(),
                    latitude = -8.07,
                    longitude = -34.88,
                    gyroX = 1.2f,
                    gyroY = 2.3f,
                    gyroZ = 3.4f,
                    deviceId = "TEST-DEVICE-001"
                ),
            )
            teste.forEach { AppDatabase.getInstance(getApplication()).coletaDao().inserir(it) }
            carregarColetas()
        }
    }



}
