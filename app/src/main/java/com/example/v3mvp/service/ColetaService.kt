package com.example.v3mvp.service

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.Build
import android.provider.Settings
import android.content.Context
import android.util.Log
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Environment
import com.example.v3mvp.data.AppDatabase
import com.example.v3mvp.model.Coleta
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.gms.location.LocationServices

class ColetaService : Service(), SensorEventListener {

    companion object {
        const val ACTION_COLETAR_AGORA = "ACTION_COLETAR_AGORA"
        const val ACTION_UPDATE_INTERVAL = "ACTION_UPDATE_INTERVAL"
        const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var sensorManager: SensorManager
    private var lastGyro: FloatArray? = null
    private var intervalo: Long = 10_000L // padrão: 10s
    private var coletaJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        criarNotificacaoForeground()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL)
        iniciarLoopAutomatico()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_COLETAR_AGORA -> {
                val fotoPath = intent.getStringExtra("fotoPath")
                scope.launch { acionarColetaComFoto(fotoPath) }
            }
            ACTION_UPDATE_INTERVAL -> {
                val novoIntervalo = intent.getLongExtra(EXTRA_INTERVAL, 10_000L)
                intervalo = novoIntervalo
                reiniciarLoopAutomatico()
            }
            else -> {}
        }
        return START_STICKY
    }

    private fun criarNotificacaoForeground() {
        val canalId = "canal_coleta"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "Coleta de Dados", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(canal)
        }
        val notificacao = Notification.Builder(this, canalId)
            .setContentTitle("Coleta de dados ativa")
            .setContentText("O app está coletando dados em segundo plano.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
        startForeground(1, notificacao)
    }

    private fun iniciarLoopAutomatico() {
        coletaJob?.cancel()
        coletaJob = scope.launch {
            while (isActive) {
                acionarColetaSemFoto() // coleta automática sem foto
                delay(intervalo)
            }
        }
    }

    private fun reiniciarLoopAutomatico() {
        coletaJob?.cancel()
        iniciarLoopAutomatico()
    }

    // Coleta automática (de 10 em 10s, sem foto)
    private suspend fun acionarColetaSemFoto() {
        salvarColeta(null)
    }

    // Coleta manual (com foto)
    private suspend fun acionarColetaComFoto(fotoPath: String?) {
        salvarColeta(fotoPath)
    }

    // Lógica de coleta (chamada por ambos)
    private suspend fun salvarColeta(fotoPath: String?) {
        val fused = LocationServices.getFusedLocationProviderClient(this)
        try {
            val location = withContext(Dispatchers.Main) { fused.lastLocation.await() }
            if (location == null || location.latitude == 0.0 || location.longitude == 0.0) {
                Log.d("ColetaService", "Localização inválida, coleta descartada.")
                return
            }
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val coleta = Coleta(
                timestamp = System.currentTimeMillis(),
                latitude = location.latitude,
                longitude = location.longitude,
                gyroX = lastGyro?.getOrNull(0),
                gyroY = lastGyro?.getOrNull(1),
                gyroZ = lastGyro?.getOrNull(2),
                deviceId = deviceId,
                fotoPath = fotoPath
            )
            val db = AppDatabase.getInstance(applicationContext)
            val coletaDao = db.coletaDao()
            coletaDao.inserir(coleta)
            Log.d("ColetaService", "Coleta salva: $coleta")
        } catch (e: Exception) {
            Log.e("ColetaService", "Erro ao salvar coleta", e)
        }
    }

    override fun onDestroy() {
        coletaJob?.cancel()
        scope.cancel()
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
            lastGyro = event.values.clone()
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
