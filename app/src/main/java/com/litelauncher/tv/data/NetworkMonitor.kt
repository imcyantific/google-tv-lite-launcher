package com.litelauncher.tv.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper

enum class NetworkKind { Offline, Wifi, Ethernet, Other }

/** [level] is Wi-Fi strength from 1 (weak) to 3 (strong). */
data class NetworkStatus(val kind: NetworkKind = NetworkKind.Offline, val level: Int = 0)

class NetworkMonitor(context: Context, private val onChange: (NetworkStatus) -> Unit) {

    private val manager = context.getSystemService(ConnectivityManager::class.java)

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            onChange(read(capabilities))
        }

        override fun onLost(network: Network) {
            onChange(NetworkStatus())
        }
    }

    fun start() {
        runCatching { manager.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper())) }
    }

    fun stop() {
        runCatching { manager.unregisterNetworkCallback(callback) }
    }

    private fun read(capabilities: NetworkCapabilities): NetworkStatus = when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkStatus(NetworkKind.Ethernet)
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkStatus(NetworkKind.Wifi, wifiLevel(capabilities))
        else -> NetworkStatus(NetworkKind.Other)
    }

    private fun wifiLevel(capabilities: NetworkCapabilities): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 3
        val dbm = capabilities.signalStrength
        return when {
            dbm == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED -> 3
            dbm >= -60 -> 3
            dbm >= -72 -> 2
            else -> 1
        }
    }
}
