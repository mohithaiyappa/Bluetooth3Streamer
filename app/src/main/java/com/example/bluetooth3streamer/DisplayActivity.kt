package com.example.bluetooth3streamer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.DataInputStream
import java.io.InputStream
import java.lang.Exception
import java.util.UUID

class DisplayActivity : AppCompatActivity() {

    private val NAME: String = "BluetoothStreamer"

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mmServerSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var mInputStream: InputStream? = null

    private var keepAlive: Boolean = false
    private var buildServerJob: Job? = null
    private var listenJob: Job? = null

    private var imageView: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display)

        imageView = findViewById(R.id.imageViewDisplay)

        // build server socket
        buildServerJob = buildServerSocket()
        // listen to incoming data
        listenJob = listen()
    }

    /**
     * tries to build secure socket if failed tries to build insecure socket
     */
    private fun buildServerSocket() = lifecycleScope.launch(Dispatchers.IO) {
        // try building secure socket else try building insecure socket
        try {
            // build secure socket
            mmServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                NAME,
                UUID.fromString(UUID_STRING)
            )
            Log.d(TAG, " built secure socket successfully")
        } catch (e: Exception) {
            Log.e(TAG, "buildServerSocket: building secure socket failed $e", e)
            try {
                // build insecure socket
                mmServerSocket = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                    NAME,
                    UUID.fromString(UUID_STRING)
                )
                Log.d(TAG, "built insecure socket successfully")
            } catch (e: Exception) {
                Log.e(TAG, "buildServerSocket: building insecure socket failed $e", e)
            }
        }

        // build client socket
        while (true) {
            try {
                // accept is a blocking call.
                // build client socket and keep alive
                Log.d(TAG, "buildServerSocket: trying to connect to client")
                Thread.sleep(1000)
                clientSocket = mmServerSocket!!.accept(1000)
                Log.d(TAG, "buildServerSocket: successfully connected to client")

                // save input stream
                mInputStream = clientSocket?.inputStream
                break
            } catch (e: Exception) {
                Log.e(TAG, "buildServerSocket: building client socket failed$e", e)
            }
        }
    }

    /**
     * listens to the incoming data stream.
     */
    private fun listen() = lifecycleScope.launch(Dispatchers.IO) {
        // keep socket alive
        keepAlive = true
        while (keepAlive) {
            if (mInputStream == null) {
                Thread.sleep(100)
                continue
            }
            // log client socket connection
            clientSocket?.let {
                Log.d(TAG, "listen: client socket connection status ${it.isConnected}")
            }

            // try to read input stream
            try {
                // read input stream and update
                Log.d(TAG, "buildServerSocket: ready to read input stream")
                val inputStream = mInputStream
                // read
                val dataInputStream: DataInputStream = DataInputStream(inputStream)
                val length = dataInputStream.readInt()
                val data = ByteArray(length)
                dataInputStream.readFully(data)
                Log.d(TAG, "read input stream : updating ui")
                updateUi(data)
            } catch (e: Exception) {
                Log.i(TAG, "buildServerSocket: error while reading input stream")
                Log.e(TAG, "buildServerSocket: $e", e)
            }
        }
    }

    /**
     * converts the incoming byte array to bitmap and updates ImageView
     */
    private fun updateUi(byteArray: ByteArray) {
        var bitmap: Bitmap? = null
        if (byteArray.size < 4)
            return
        try {
            bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            Log.e(TAG, "byteArrayToBitmapFactory: $e", e)
        }
        runOnUiThread {
            imageView?.setImageBitmap(bitmap)
        }
    }

    // currently not being used decide if  needed in the future
    private fun rebuildServerSocket() {
        buildServerJob?.cancel()
        buildServerJob = buildServerSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        keepAlive = false
        buildServerJob?.cancel()
        listenJob?.cancel()
        clientSocket?.takeIf { it.isConnected }?.apply {
            close()
        }
        mmServerSocket?.apply {
            close()
        }
    }

    companion object {
        private var selectedDevice: BluetoothDevice? = null

        fun getIntent(context: Context, device: BluetoothDevice): Intent {
            selectedDevice = device
            Log.d(TAG, "getIntent: device ${device.name}")
            Log.d(TAG, "getIntent: selected device ${selectedDevice?.name}")

            return Intent(context, DisplayActivity::class.java)
        }
    }
}
