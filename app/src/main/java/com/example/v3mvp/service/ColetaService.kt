package com.example.v3mvp.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.hardware.*
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.example.v3mvp.FotoActivity
import com.example.v3mvp.data.AppDatabase
import com.example.v3mvp.model.Coleta
import com.example.v3mvp.remote.ColetaRemoteDataSource
import com.example.v3mvp.util.FotoHelper
import com.example.v3mvp.data.repository.ColetaRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class ColetaService : Service(), SensorEventListener {

    companion object {
        const val ACTION_COLETAR_AGORA = "ACTION_COLETAR_AGORA"
        const val ACTION_UPDATE_INTERVAL = "ACTION_UPDATE_INTERVAL"
        const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var sensorManager: SensorManager
    private var lastGyro: FloatArray? = null
    private var intervalo: Long = 10_000L
    private var coletaJob: Job? = null

    private val repository by lazy {
        ColetaRepository(AppDatabase.getInstance(applicationContext).coletaDao())
    }

    override fun onCreate() {
        super.onCreate()
        criarNotificacaoForeground()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI)

        scope.launch {
            delay(1000)
            iniciarLoopAutomatico()
        }

        tentarReenviarPendentes()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_COLETAR_AGORA -> {
                val fotoPath = intent.getStringExtra("fotoPath")
                scope.launch { salvarColeta(fotoPath) }
            }
            ACTION_UPDATE_INTERVAL -> {
                intervalo = intent.getLongExtra(EXTRA_INTERVAL, 10_000L)
                reiniciarLoopAutomatico()
            }
        }
        return START_STICKY
    }

    private fun criarNotificacaoForeground() {
        val canalId = "canal_coleta"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "Coleta de Dados", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(canal)
        }

        val notificacao = Notification.Builder(this, canalId)
            .setContentTitle("Coleta de dados ativa")
            .setContentText("O app está coletando dados em segundo plano.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notificacao, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notificacao)
        }
    }

    private fun iniciarLoopAutomatico() {
        coletaJob?.cancel()
        coletaJob = scope.launch {
            while (isActive) {
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@ColetaService, FotoActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                delay(intervalo)
            }
        }
    }

    private fun reiniciarLoopAutomatico() {
        coletaJob?.cancel()
        iniciarLoopAutomatico()
    }

    private suspend fun salvarColeta(fotoPath: String?) {
        try {
            val fused = LocationServices.getFusedLocationProviderClient(this)
            val location = withContext(Dispatchers.Main) { fused.lastLocation.await() }

            if (location == null || (location.latitude == 0.0 && location.longitude == 0.0)) {
                emitirErro("Localização inválida")
                return
            }

            if (lastGyro == null) {
                emitirErro("Giroscópio não inicializado")
                return
            }

            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

            val coleta = Coleta(
                timestamp = System.currentTimeMillis(),
                latitude = location.latitude,
                longitude = location.longitude,
                gyroX = lastGyro?.getOrNull(0) ?: 0f,
                gyroY = lastGyro?.getOrNull(1) ?: 0f,
                gyroZ = lastGyro?.getOrNull(2) ?: 0f,
                deviceId = deviceId,
                fotoPath = fotoPath,
                enviado = false
            )

            repository.inserir(coleta)
            enviarColetaParaApi(coleta)

        } catch (e: Exception) {
            emitirErro("Erro inesperado ao salvar coleta: ${e.message}")
            Log.e("ColetaService", "Erro ao salvar: ${e.message}", e)
        }
    }

    private fun enviarColetaParaApi(coleta: Coleta) {
        val fotoBase64 = FotoHelper.toBase64(coleta.fotoPath)

        ColetaRemoteDataSource.enviar(coleta, fotoBase64,
            onSuccess = {
                scope.launch { repository.marcarComoEnviado(coleta.id) }
            },
            onError = { msg ->
                notificarUsuario("Erro ao enviar coleta: $msg")
            }
        )
    }

    private fun tentarReenviarPendentes() {
        scope.launch {
            repository.listarNaoEnviados().forEach { enviarColetaParaApi(it) }
        }
    }

    private fun emitirErro(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun notificarUsuario(msg: String) {
        val canalId = "erros_coleta"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "Erros de Coleta", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(canal)
        }
        val notificacao = Notification.Builder(this, canalId)
            .setContentTitle("Erro na Coleta")
            .setContentText(msg)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notificacao)
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
