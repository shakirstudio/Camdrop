package com.example.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.example.model.CameraBrand
import com.example.model.CameraDevice
import com.example.model.ConnectionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class RealWifiCameraDetector(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun isWifiConnected(): Boolean {
        val network = connManager?.activeNetwork ?: return false
        val caps = connManager.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getCurrentSsid(): String {
        return try {
            val info = wifiManager?.connectionInfo
            val ssid = info?.ssid?.replace("\"", "") ?: "Unknown Network"
            if (ssid == "<unknown ssid>") "Connected Wi-Fi" else ssid
        } catch (e: Exception) {
            "Connected Wi-Fi"
        }
    }

    suspend fun probeConnectedCameraSubnet(): List<CameraDevice> = withContext(Dispatchers.IO) {
        val detected = mutableListOf<CameraDevice>()
        if (!isWifiConnected()) return@withContext detected

        // Real socket probe of standard Camera Tethering / PTP / HTTP endpoints on default gateway & subnets
        val probeTargets = listOf(
            Triple("192.168.122.1", 8080, CameraBrand.SONY to "Sony Alpha (Direct Wi-Fi)"),
            Triple("192.168.1.1", 15740, CameraBrand.CANON to "Canon EOS (PTP/IP Port 15740)"),
            Triple("192.168.4.1", 15740, CameraBrand.NIKON to "Nikon Z (SnapBridge Direct)"),
            Triple("192.168.0.1", 8080, CameraBrand.GENERIC to "Network Camera HTTP"),
            Triple("192.168.1.100", 21, CameraBrand.GENERIC to "Studio FTP Endpoint")
        )

        for ((ip, port, brandInfo) in probeTargets) {
            val (brand, model) = brandInfo
            if (isEndpointOpen(ip, port, 400)) {
                detected.add(
                    CameraDevice(
                        id = "wifi-$ip-$port",
                        brand = brand,
                        model = model,
                        connectionType = ConnectionType.WIFI,
                        ipAddress = ip,
                        port = port,
                        isSupported = true,
                        supportNote = "Real open port verified on $ip:$port"
                    )
                )
            }
        }
        detected
    }

    private fun isEndpointOpen(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
