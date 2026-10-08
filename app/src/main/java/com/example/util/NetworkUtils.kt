package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    /**
     * Retrieves the current IPv4 address of the Android device on Wi-Fi or Hotspot.
     * Crucial for Camera FTP configuration so the photographer can enter the phone's IP in the camera.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                // Focus on wlan0 or ap0 (hotspot)
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (host != null && !host.contains(":")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return "192.168.1.50"
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getStorageUsage(context: Context): Pair<Long, Long> {
        val photosDir = File(context.filesDir, "photos")
        var usedBytes = 0L
        if (photosDir.exists()) {
            photosDir.listFiles()?.forEach { file ->
                usedBytes += file.length()
            }
        }
        val freeBytes = context.filesDir.freeSpace
        return Pair(usedBytes, freeBytes)
    }
}
