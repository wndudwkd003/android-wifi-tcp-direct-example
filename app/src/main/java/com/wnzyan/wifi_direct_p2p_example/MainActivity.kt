package com.wnzyan.wifi_direct_p2p_example

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.wnzyan.wifi_direct_p2p_example.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.btnConnectHost.setOnClickListener {
            startActivity(Intent(this, ConnectActivity::class.java).putExtra("CONNECT_TYPE", "HOST"))
        }

        viewBinding.btnConnectClient.setOnClickListener {
            startActivity(Intent(this, ConnectActivity::class.java).putExtra("CONNECT_TYPE", "CLIENT"))
        }

    }
}