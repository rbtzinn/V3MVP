// FotoActivity.kt
package com.example.v3mvp

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.v3mvp.util.FaceDetectorUtil
import kotlinx.coroutines.launch
import java.io.File

class FotoActivity : AppCompatActivity() {

    private var fotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        abrirCamera()
    }

    private fun abrirCamera() {
        val nomeArquivo = "FOTO_${System.currentTimeMillis()}.jpg"
        val arquivo = File(getExternalFilesDir(null), nomeArquivo)
        fotoPath = arquivo.absolutePath

        val fotoUri = FileProvider.getUriForFile(
            this, "${packageName}.provider", arquivo
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri)
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val bitmap = FaceDetectorUtil.lerBitmap(this, fotoPath)
            lifecycleScope.launch {
                // **VALIDAÇÃO DE ROSTO**:
                val temRosto = FaceDetectorUtil.temRosto(this@FotoActivity, bitmap)
                if (temRosto) {
                    Toast.makeText(this@FotoActivity, "Rosto detectado!", Toast.LENGTH_SHORT).show()
                    // Aqui você pode acionar o Service para coletar os dados,
                    // enviando o caminho da foto junto via Intent extra
                } else {
                    Toast.makeText(this@FotoActivity, "Sem rosto!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
