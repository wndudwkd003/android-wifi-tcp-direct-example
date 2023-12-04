package com.wnzyan.wifi_direct_p2p_example.device_list

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.wnzyan.wifi_direct_p2p_example.R

class DeviceListAdapter(
    context: Context,
    private val resource: Int,
    private val items: List<WifiP2pDevice>
) : ArrayAdapter<WifiP2pDevice>(context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater = LayoutInflater.from(context)
        val view = convertView ?: layoutInflater.inflate(resource, parent, false)

        val deviceName = view.findViewById<TextView>(R.id.tv_device_name)
        val deviceMacAddress = view.findViewById<TextView>(R.id.tv_device_mac_address)
        val deviceIpAddress = view.findViewById<TextView>(R.id.tv_device_ip_address)

        val device = items[position]
        deviceName.text = device.deviceName
        deviceMacAddress.text = device.deviceAddress
        deviceIpAddress.text = "test"


        return view
    }


}
