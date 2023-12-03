package com.wnzyan.wifi_direct_p2p_example

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.appcompat.app.AppCompatActivity
import com.wnzyan.wifi_direct_p2p_example.databinding.ActivityConnectBinding

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
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)
        }

        if (peers.isEmpty()) {
            logtv("No devices found")
        }
    }


    private lateinit var viewBinding: ActivityConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        intent.getStringExtra("CONNECT_TYPE")?.let {
            connectType = it
        }



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


        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        // 인텐트 필터 설정
        setupIndicates()

        if (!initP2p()) {
            finish()
        }




        if (connectType == "HOST") {
            viewBinding.btnConnectStart.setOnClickListener {
                if (!isWifiP2pEnabled) {
                    return@setOnClickListener
                }

                val ssid = viewBinding.etSsid.text.toString()
                val pw = viewBinding.etPw.text.toString()
                val band = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiP2pConfig.GROUP_OWNER_BAND_2GHZ
                } else {
                    -1
                }

                val config: WifiP2pConfig

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    config = WifiP2pConfig.Builder()
                        .setNetworkName("DIRECT-hs-$ssid")
                        .setPassphrase(pw)
                        .enablePersistentMode(false)
                        .setGroupOperatingBand(band)
                        .build()

                    manager.createGroup(channel, config, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            isHotspotEnabled = true
                            logtv("hot spot start")
                            logtv("SSID : $ssid")
                            logtv("PW : $pw")
                            logtv("BAND : $band")
                        }

                        override fun onFailure(p0: Int) {
                            logtv("hot spot failed")
                        }

                    })
                } else {

                }

                startServerSocket()

            }

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

            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    logtv("Peer discovery started")
                }

                override fun onFailure(p0: Int) {
                    logtv("Peer discovery initiation failed: $p0")
                }

            })

            viewBinding.btnConnectStart.setOnClickListener {
                if (peers.isNotEmpty()) {
                    // 첫 번째 피어(호스트)를 선택. 실제 앱에서는 사용자가 피어를 선택할 수 있어야 합니다.
                    val device = peers[0]

                    val config = WifiP2pConfig().apply {
                        deviceAddress = device.deviceAddress
                        wps.setup = WpsInfo.PBC
                    }

                    manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            // 연결 시도 성공
                            logtv("Connection to host initiated")
                            logtv("host address ${device.deviceAddress}")
                            hostAddress = device.deviceAddress
                        }

                        override fun onFailure(reason: Int) {
                            // 연결 시도 실패
                            logtv("Connection to host failed: $reason")
                        }
                    })




                } else {
                    logtv("No peers found")
                }





            }

            viewBinding.btnInput.setOnClickListener {
                manager.requestConnectionInfo(channel, object : WifiP2pManager.ConnectionInfoListener {
                    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
                        if (info.groupFormed && info.isGroupOwner) {
                            logtv("you are host owner")
                            // 호스트로서의 로직
                        } else if (info.groupFormed) {
                            logtv("you are client")
                            // 클라이언트로서의 로직
                            val serverIp = info.groupOwnerAddress.hostAddress
                            hostIpAddress = serverIp
                            hostIpAddress?.let {
                                logtv(it)
                            }
                        }
                    }
                })



                val input = viewBinding.etInput.text.toString()
                hostIpAddress?.let {
                    startClientSocket(it, input)
                    logtv("host $hostAddress , $hostIpAddress to send $input")
                }

            }
        }


        refresh()

        viewBinding.btnRefresh.setOnClickListener {
            refresh()
        }


    }

    @SuppressLint("MissingPermission")
    private fun refresh() {
        manager.requestPeers(channel, peerListListener)
    }



    private fun startServerSocket() {
        val serverThread = SocketServerThread(this)
        serverThread.start()

    }


    private fun startClientSocket(hostAddr: String, message: String) {
        val clientThread = SocketClientThread(hostAddr, message)
        clientThread.start()
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


    @SuppressLint("MissingPermission")
    fun updateConnectedDeviceList() {
        if (!isHotspotEnabled) {
            return
        }


        manager.requestGroupInfo(channel, object : WifiP2pManager.GroupInfoListener {
            override fun onGroupInfoAvailable(p0: WifiP2pGroup?) {
                val devices = p0?.clientList
                devices?.forEach {
                    logtv("device ${it.deviceAddress}")
                }
            }
        })
    }

    private fun initP2p(): Boolean { // Device capability definition check
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.")
            return false
        } // Hardware capability check
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        if (!wifiManager.isP2pSupported) {
            Log.e(TAG, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.")
            return false
        }
        manager = applicationContext.getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        return true
    }


    private fun logtv(message: String) {
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



    companion object {
        const val TAG = "TEST_LOG_CAT"
        const val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001
    }
}