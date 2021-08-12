package com.example.bluetooth3streamer

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceListAdapter(
    private var list: ArrayList<BluetoothDevice>,
    private val onItemClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        var textView: TextView? = view.findViewById(R.id.deviceNameTv)

        fun bind(device: BluetoothDevice) {
            textView?.text = device.name
        }
    }

    fun submitList(newList: ArrayList<BluetoothDevice>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.apply {
            bind(list[position])
            view.setOnClickListener { onItemClick.invoke(list[position]) }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}
