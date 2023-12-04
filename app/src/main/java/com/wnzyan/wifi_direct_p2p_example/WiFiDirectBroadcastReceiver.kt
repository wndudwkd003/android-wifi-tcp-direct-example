package com.wnzyan.wifi_direct_p2p_example

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.util.Log
import androidx.core.app.ActivityCompat

class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: Channel,
    private val connectActivity: ConnectActivity,
    private val peerListListener: PeerListListener,
    private val requestConnectionInfoListener: ConnectionInfoListener
): BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent) {
        when(intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Determine if Wifi P2P mode is enabled or not, alert
                // the Activity.
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                connectActivity.isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                connectActivity.updateConnectedDeviceList()
                manager.requestPeers(channel, peerListListener)



            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {

                val networkInfo: NetworkInfo? = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo?.isConnected == true) {
                    // 연결이 완료되었으므로 연결 정보를 요청

                    manager.requestConnectionInfo(channel,
                })
            }

            manager.requestPeers(channel, peerListListener)
                    Log.d(ConnectActivity.TAG, "P2P peers changed")
            // Connection state changed! We should probably do something about
            // that.

        }
        WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
            // Respond to this device's wifi state changing
        }
    }
}
}