package com.example.bluetooth3streamer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class StreamActivity : AppCompatActivity() {

    private var clientSocket: BluetoothSocket? = null

    private var outputStream: OutputStream? = null

    private var buildSocketJob: Job? = null
    private var writeJob: Job? = null

    private var imageView: ImageView? = null

    private val executor: Executor = Executors.newFixedThreadPool(1)

    private val subject: PublishSubject<ByteArray> = PublishSubject.create()
    private val bitmapSubject: PublishSubject<Bitmap> = PublishSubject.create()

    private val compositeDisposable = CompositeDisposable()

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream)

        // setup camera
        startImageStreamer()

        imageView = findViewById(R.id.imageViewStream)

        // build client socket
        buildSocketJob = buildSocket()

        val d1 = subject
            .debounce(50, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { writeBitmapByteArray(it) },
                { Log.e(TAG, "onCreate: $it", it) }
            )
        compositeDisposable.add(d1)
        val d2 = bitmapSubject
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { imageView?.setImageBitmap(it) },
                { Log.e(TAG, "onCreate: $it", it) }
            )
        compositeDisposable.add(d2)
    }

    /**
     *  tries to create a client socket and connect to it.
     */
    private fun buildSocket() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            // build secure socket
            clientSocket = selectedDevice!!.createRfcommSocketToServiceRecord(
                UUID.fromString(UUID_STRING)
            )
        } catch (e: Exception) {
            Log.e(TAG, "buildSocket: building secure socket failed $e", e)
            try {
                // build insecure socket
                clientSocket = selectedDevice!!.createInsecureRfcommSocketToServiceRecord(
                    UUID.fromString(UUID_STRING)
                )
            } catch (e: Exception) {
                Log.e(TAG, "buildSocket: build insecure socket failed $e", e)
            }
        }

        try {
            // connect client socket
            clientSocket!!.connect()

            outputStream = clientSocket!!.outputStream
        } catch (e: Exception) {
            Log.e(TAG, "buildSocket: connecting client socket failed $e", e)
        }
    }

    /**
     * writes ByteArray to OutputStream
     */
    private fun writeBitmapByteArray(byteArray: ByteArray) {
        outputStream?.let {
            try {
                // write to out stream and flush
                Log.d(TAG, "writeBitmapByteArray: writing")
                val stream = DataOutputStream(it)
                val data = byteArray
                stream.writeInt(data.size)
                stream.write(data)
                stream.flush()
                Log.d(TAG, "writeBitmapByteArray: writing done")
            } catch (e: Exception) {
                Log.e(TAG, "writeBitmapByteArray: $e", e)
            }
        }
    }

    /**
     * camera setup.
     */
    private fun startImageStreamer() {

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(720, 1280))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(executor, ImageAnalyser(subject, bitmapSubject))

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val cameraProvider = ProcessCameraProvider.getInstance(this).get()

        val camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageAnalysis
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
        buildSocketJob?.cancel()
        outputStream?.close()
        clientSocket?.takeIf { it.isConnected }?.apply {
            close()
        }
    }

    companion object {
        private var selectedDevice: BluetoothDevice? = null

        fun getIntent(context: Context, device: BluetoothDevice): Intent {
            selectedDevice = device
            Log.d(TAG, "getIntent: device ${device.name}")
            Log.d(TAG, "getIntent: selected device ${selectedDevice?.name}")

            return Intent(context, StreamActivity::class.java)
        }
    }
}
