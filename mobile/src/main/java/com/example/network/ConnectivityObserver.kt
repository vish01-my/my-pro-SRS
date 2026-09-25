package com.example.network

import kotlinx.coroutines.flow.Flow

/**
 * Interface for observing device network connectivity changes.
 * 
 * Essential for the e-PDS Fair Price Shop POS terminal to detect 2G/3G/4G/WiFi
 * restoration and initiate automatic synchronization of queued offline records.
 */
interface ConnectivityObserver {

    /**
     * Emits the current network reachability state.
     */
    val status: Flow<NetworkStatus>

    /**
     * Emits the active transport type (WiFi, Cellular, Ethernet, None).
     */
    val connectionType: Flow<ConnectionType>

    /**
     * Synchronous query for current network connectivity.
     */
    fun isConnected(): Boolean

    /**
     * Synchronous query for the active connection type.
     */
    fun getCurrentConnectionType(): ConnectionType

    enum class NetworkStatus {
        Available,
        Unavailable,
        Losing,
        Lost
    }

    enum class ConnectionType {
        WIFI,
        CELLULAR,
        ETHERNET,
        BLUETOOTH,
        VPN,
        NONE
    }
}
