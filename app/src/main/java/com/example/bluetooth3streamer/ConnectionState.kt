package com.example.bluetooth3streamer

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connect : ConnectionState()
    object Connected : ConnectionState()
}
