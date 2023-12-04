package com.wnzyan.wifi_direct_p2p_example

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.wnzyan.wifi_direct_p2p_example.databinding.ActivityConnectBinding
import com.wnzyan.wifi_direct_p2p_example.device_list.DeviceListAdapter

class ConnectActivity : AppCompatActivity() {

    private val intentFilter = IntentFilter()

    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var receiver: WiFiDirectBroadcastReceiver

    private val peers = mutableListOf<WifiP2pDevice>()

    var isWifiP2pEnabled = false
    var isHotspotEnabled = false

    private lateinit var connectType: String

    private var hostAddress: String? = null
    private var hostIpAddress: String? = null


    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        logtv("original peers")
        logtv(refreshedPeers.toString())
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)
        }

        logtv("refreshed peers")
        logtv(refreshedPeers.toString())

        if (peers.isEmpty()) {
            logtv("No devices found")
        }
    }


    private lateinit var viewBinding: ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // 권한 설정
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.NEARBY_WIFI_DEVICES), PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION
                )
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION)

            }
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }


        // 매니저, 채널 초기화
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        // 인텐트 필터 설정
        setupIndicates()


        // 핫스팟 연결 설정 기능 아직 추가 x
        intent.getStringExtra("CONNECT_TYPE")?.let { connectType = it }
        if (connectType == "HOST") {
            viewBinding.tvSsid.visibility = View.GONE
            viewBinding.etSsid.visibility = View.GONE
            viewBinding.tvPw.visibility = View.GONE
            viewBinding.etPw.visibility = View.GONE


            viewBinding.btnConnectStop.setOnClickListener {
                manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        isHotspotEnabled = false
                        logtv("connection closed")
                    }

                    override fun onFailure(p0: Int) {
                        logtv("connection close error")
                    }

                })
            }

            viewBinding.btnInput.setOnClickListener {
                val input = viewBinding.etInput.text.toString()

            }



        } else {
            viewBinding.tvSsid.visibility = View.GONE
            viewBinding.etSsid.visibility = View.GONE
            viewBinding.tvPw.visibility = View.GONE
            viewBinding.etPw.visibility = View.GONE
        }


        // 연결 버튼, 누르면 연결 가능한 피어(디바이스)들의 목록을 표시
        viewBinding.btnConnectStart.setOnClickListener {
            if (peers.isNotEmpty()) {
                showDevicesDialog(this, peers)
            } else {
                logtv("No peers found. Please refresh.")
            }
        }

        // 정지 버튼을 누르면 모든 연결 종료
        viewBinding.btnConnectStop.setOnClickListener {
            manager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    logtv("Disconnect success!")
                }

                override fun onFailure(reason: Int) {
                    logtv("Disconnect failure")
                }

            })

        }

        // 새로고침 버튼 (탐색 기능)
        viewBinding.btnRefresh.setOnClickListener {
            receiver.discoverPeers()
        }
    }






    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Fine location permission is not granted!")
                finish()
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        receiver = WiFiDirectBroadcastReceiver(manager, channel, this, peerListListener)
        registerReceiver(receiver, intentFilter)
        logtv("receiver register")
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }



    private fun setupIndicates() {
        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }




    fun logtv(message: String) {
        Log.d(TAG, message)
        viewBinding.tvLog.text = buildString {
            append(viewBinding.tvLog.text)
            append(message)
            append("\n")
        }
    }


    fun updateChat(message: String) {
        // UI 스레드에서 채팅 메시지를 표시
        logtv(message)
    }

    private fun showDevicesDialog(context: Context, devices: List<WifiP2pDevice>) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_devices_list)

        val listView = dialog.findViewById<ListView>(R.id.lv_device)
        val adapter = DeviceListAdapter(context, R.layout.cv_select_device, devices)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = devices[position]
            connectToDevice(device)
            dialog.dismiss()
        }

        dialog.show()
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                logtv("Connecting success to device!!: ${device.deviceAddress}")



            }

            override fun onFailure(reason: Int) {
                logtv("Failed to connect to device: ${device.deviceAddress}")
            }
        })
    }




    companion object {
        const val TAG = "TEST_LOG_CAT"
        const val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001
    }
}