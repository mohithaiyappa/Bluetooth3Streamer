package com.example.bluetooth3streamer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private var hasPermissions: Boolean = false
    private var selectedDevice: BluetoothDevice? = null

    private var selectedDeviceTextView: TextView? = null
    private var devicesRecyclerView: RecyclerView? = null
    private var adapter: DeviceListAdapter? = null
    private var displayBtn: Button? = null
    private var streamBtn: Button? = null

    // updates the current selected device.
    private val onDeviceSelected: (BluetoothDevice) -> Unit = { bluetoothDevice ->
        selectedDevice = bluetoothDevice
        val text = "Selected Device : " + bluetoothDevice.name
        selectedDeviceTextView?.text = text
    }
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
        this::onPermissionResult
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        // bind views
        selectedDeviceTextView = findViewById(R.id.deviceText)
        devicesRecyclerView = findViewById(R.id.recyclerView)
        displayBtn = findViewById(R.id.displayBtn)
        streamBtn = findViewById(R.id.streamBtn)

        // click listeners
        displayBtn?.setOnClickListener(this::displayBtnClicked)
        streamBtn?.setOnClickListener(this::streamBtnClicked)

        // setup recycler view
        adapter = DeviceListAdapter(
            list = ArrayList(),
            onItemClick = onDeviceSelected
        )
        devicesRecyclerView?.layoutManager = LinearLayoutManager(this)
        devicesRecyclerView?.adapter = adapter
        devicesRecyclerView?.setHasFixedSize(true)
    }

    override fun onResume() {
        super.onResume()

        // update adapter
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter?.bondedDevices ?: emptySet()
        val devices: ArrayList<BluetoothDevice> = ArrayList(pairedDevices)
        adapter?.submitList(newList = devices)
    }

    /**
     * returns true if a condition is not met
     */
    private fun hasNotMetConditions(): Boolean {
        // checks for permission
        if (!hasPermissions) {
            Toast.makeText(this, "Needs Permissions", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "hasNotMetConditions: Needs Permissions")
            return true
        }

        // must select a device
        if (selectedDevice == null) {
            Toast.makeText(this, "Select a Device", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "hasNotMetConditions: No device selected")
            return true
        }

        // return false as all conditions have been met
        return false
    }

    /**
     * check if conditions have met and takes to display activity
     */
    private fun displayBtnClicked(view: View) {

        if (hasNotMetConditions())
            return

        startActivity(
            DisplayActivity.getIntent(this, selectedDevice!!)
        )
    }

    /**
     * check if conditions have met and takes to stream activity
     */
    private fun streamBtnClicked(view: View) {

        if (hasNotMetConditions())
            return

        startActivity(
            StreamActivity.getIntent(this, selectedDevice!!)
        )
    }

    private fun onPermissionResult(results: Map<String, Boolean>) {
        val locationPermission = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val cameraPermission = results[Manifest.permission.CAMERA] ?: false
        hasPermissions = locationPermission && cameraPermission
    }
}
