package com.example.v3mvp.service

import android.app.Service
import android.content.Intent
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.v3mvp.data.AppDatabase
import com.example.v3mvp.model.Coleta
import kotlinx.coroutines.*
import android.provider.Settings
import androidx.annotation.RequiresApi


class ColetaService : Service(), SensorEventListener {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager

    private var lastGyro: FloatArray? = null

    override fun onCreate() {
        super.onCreate()

        criarNotificacaoForeground()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL)

        scope.launch {
            while (isActive) {
                coletarDados()
                delay(10_000)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun criarNotificacaoForeground() {
        val canalId = "canal_coleta"
        val nome = "Coleta de Dados"
        val descricao = "Serviço que coleta dados em segundo plano"

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = android.app.NotificationChannel(
                canalId,
                nome,
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = descricao
            }
            manager.createNotificationChannel(canal)
        }

        val notificacao = android.app.Notification.Builder(this, canalId)
            .setContentTitle("Coleta de dados ativa")
            .setContentText("O app está coletando dados em segundo plano.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notificacao)
    }


    private fun coletarDados() {
        try {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            val deviceId = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ANDROID_ID
            )

            val coleta = Coleta(
                timestamp = System.currentTimeMillis(),
                latitude = location?.latitude,
                longitude = location?.longitude,
                gyroX = lastGyro?.getOrNull(0),
                gyroY = lastGyro?.getOrNull(1),
                gyroZ = lastGyro?.getOrNull(2),
                deviceId = deviceId // 👈 novo dado incluído
            )

            val db = AppDatabase.getInstance(applicationContext)
            val coletaDao = db.coletaDao()

            scope.launch {
                coletaDao.inserir(coleta)
            }

            Log.d("ColetaService", "Coleta salva: $coleta")
        } catch (e: Exception) {
            Log.e("ColetaService", "Erro ao salvar coleta", e)
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            lastGyro = event.values.clone()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
