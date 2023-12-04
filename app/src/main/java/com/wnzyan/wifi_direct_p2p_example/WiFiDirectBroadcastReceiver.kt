package com.wnzyan.wifi_direct_p2p_example

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Build
import android.util.Log


class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: Channel,
    private val connectActivity: ConnectActivity,
    private val peerListListener: PeerListListener
): BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent) {
        when(intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Wi-Fi P2P가 활성화 되었는지 또는 비활성화 되었는지
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                connectActivity.isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // 사용가능한 피어 목록이 변경되었을 때
                manager.requestPeers(channel, peerListListener)

            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {



            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing

            }
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                connectActivity.logtv("Peer discovery started")
            }

            override fun onFailure(p0: Int) {
                connectActivity.logtv("Peer discovery initiation failed: $p0")
            }
        })
    }
}