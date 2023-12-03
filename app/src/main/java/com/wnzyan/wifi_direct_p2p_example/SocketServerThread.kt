package com.wnzyan.wifi_direct_p2p_example

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class SocketServerThread(private val activity: ConnectActivity) : Thread() {
    private val serverPort = 8888

    override fun run() {
        try {
            val serverSocket = ServerSocket(serverPort)
            while (!isInterrupted) {
                val socket = serverSocket.accept()
                handleClientSocket(socket)
            }
            serverSocket.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun handleClientSocket(socket: Socket) {
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))
        val output = PrintWriter(socket.getOutputStream(), true)

        var message: String?
        while (socket.isConnected) {
            message = input.readLine()
            if (message != null) {
                activity.runOnUiThread {
                    activity.updateChat(message)
                }
            }
        }
    }
}