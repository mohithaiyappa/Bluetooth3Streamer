package com.example.bluetooth3streamer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import io.reactivex.subjects.PublishSubject
import java.io.ByteArrayOutputStream

class ImageAnalyser(
    private val subject: PublishSubject<ByteArray>,
    private val bitmapSubject: PublishSubject<Bitmap>
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        Log.d(TAG, "analyze: imageproxy")

        val bm = image.toBitmap()
        bitmapSubject.onNext(bm)
        val byteArray = bm.toByteArray()
        subject.onNext(byteArray)

        Log.d(TAG, "analyze: closing image")
        image.close()
    }

    private fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        // decide between png jpeg or webp compression format
        this.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        recycle()
        return byteArray
    }

    private fun ImageProxy.toBitmap(): Bitmap {

        val yBuffer = planes[0].buffer // Y
        val vuBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        val sourceBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val matrix = Matrix()
        matrix.postRotate(imageInfo.rotationDegrees.toFloat())
        return Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true)
    }

    private fun ImageProxy.toYuvImage(): YuvImage {

        val yBuffer = planes[0].buffer // Y
        val vuBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        return YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    }
}
