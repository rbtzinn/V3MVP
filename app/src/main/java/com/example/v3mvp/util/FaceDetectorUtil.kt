package com.example.v3mvp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import kotlinx.coroutines.tasks.await

object FaceDetectorUtil {
    fun lerBitmap(context: Context, fotoPath: String?): Bitmap? {
        if (fotoPath.isNullOrEmpty()) return null
        return BitmapFactory.decodeFile(fotoPath)
    }

    suspend fun temRosto(context: Context, bitmap: Bitmap?): Boolean {
        if (bitmap == null) return false
        val detector = FaceDetection.getClient()
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = detector.process(image).await()
        return faces.isNotEmpty()
    }
}
