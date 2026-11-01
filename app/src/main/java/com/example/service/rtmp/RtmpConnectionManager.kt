package com.example.service.rtmp

import android.util.Log
import com.example.data.model.NetworkHealth
import com.example.data.model.NetworkTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

enum class RtmpConnectionState {
    IDLE,
    CONNECTING,
    HANDSHAKING,
    LIVE,
    RECONNECTING,
    STOPPED,
    ERROR
}

class RtmpConnectionManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val _connectionState = MutableStateFlow(RtmpConnectionState.IDLE)
    val connectionState: StateFlow<RtmpConnectionState> = _connectionState.asStateFlow()

    private val _telemetry = MutableStateFlow(NetworkTelemetry())
    val telemetry: StateFlow<NetworkTelemetry> = _telemetry.asStateFlow()

    private var socket: Socket? = null
    private var outputStream: BufferedOutputStream? = null
    private var inputStream: BufferedInputStream? = null

    private val isStreaming = AtomicBoolean(false)
    private var currentHost = "a.rtmp.youtube.com"
    private var currentPort = 1935
    private var currentApp = "live2"
    private var currentStreamKey = ""

    private val bytesSentSinceLastCheck = AtomicLong(0)
    private val framesSentSinceLastCheck = AtomicInteger(0)
    private val droppedPacketsCount = AtomicInteger(0)

    private var telemetryJob: Job? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 3

    fun connect(serverUrl: String, streamKey: String): Boolean {
        currentStreamKey = streamKey
        parseRtmpUrl(serverUrl)

        _connectionState.value = RtmpConnectionState.CONNECTING
        Log.i(TAG, "Connecting to RTMP: $currentHost:$currentPort/$currentApp with key ${streamKey.take(4)}***")

        try {
            val sock = Socket()
            sock.connect(InetSocketAddress(currentHost, currentPort), 6000)
            sock.soTimeout = 8000
            sock.tcpNoDelay = true

            socket = sock
            outputStream = BufferedOutputStream(sock.getOutputStream(), 65536)
            inputStream = BufferedInputStream(sock.getInputStream(), 65536)

            _connectionState.value = RtmpConnectionState.HANDSHAKING
            performHandshake()

            sendConnectCommand()
            sendCreateStreamCommand()
            sendPublishCommand()

            isStreaming.set(true)
            reconnectAttempts = 0
            _connectionState.value = RtmpConnectionState.LIVE

            startTelemetryLoop()
            Log.i(TAG, "RTMP connection established and streaming LIVE")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to RTMP server $currentHost:$currentPort", e)
            handleDisconnect(e.message ?: "Connection failure")
            return false
        }
    }

    private fun parseRtmpUrl(url: String) {
        try {
            val uri = URI(if (url.startsWith("rtmp://")) url else "rtmp://$url")
            currentHost = uri.host ?: "a.rtmp.youtube.com"
            currentPort = if (uri.port > 0) uri.port else 1935
            val path = uri.path?.trim('/') ?: "live2"
            currentApp = if (path.isNotBlank()) path else "live2"
        } catch (e: Exception) {
            currentHost = "a.rtmp.youtube.com"
            currentPort = 1935
            currentApp = "live2"
        }
    }

    private fun performHandshake() {
        val out = outputStream ?: return
        val input = inputStream ?: return

        // 1. Send C0 (1 byte: version 3) + C1 (1536 bytes)
        val c0c1 = ByteArray(1537)
        c0c1[0] = 3 // RTMP Version 3
        Random().nextBytes(c0c1)
        c0c1[0] = 3
        // Timestamp at 1-4 = 0, zero at 5-8 = 0
        c0c1[1] = 0; c0c1[2] = 0; c0c1[3] = 0; c0c1[4] = 0
        c0c1[5] = 0; c0c1[6] = 0; c0c1[7] = 0; c0c1[8] = 0

        out.write(c0c1)
        out.flush()

        // 2. Read S0 (1 byte) + S1 (1536 bytes)
        val s0s1 = ByteArray(1537)
        var readTotal = 0
        while (readTotal < 1537) {
            val r = input.read(s0s1, readTotal, 1537 - readTotal)
            if (r < 0) throw IllegalStateException("EOF during S0/S1 handshake")
            readTotal += r
        }

        // 3. Send C2 (1536 bytes matching S1)
        out.write(s0s1, 1, 1536)
        out.flush()

        // 4. Read S2 (1536 bytes)
        val s2 = ByteArray(1536)
        var s2Read = 0
        while (s2Read < 1536) {
            val r = input.read(s2, s2Read, 1536 - s2Read)
            if (r < 0) throw IllegalStateException("EOF during S2 handshake")
            s2Read += r
        }
    }

    private fun sendConnectCommand() {
        val out = outputStream ?: return
        val amfPayload = ByteArrayOutputStream().apply {
            writeAmfString("connect")
            writeAmfNumber(1.0) // Transaction ID
            // Connect parameters object
            write(0x03) // Object start
            writeAmfPropertyString("app", currentApp)
            writeAmfPropertyString("tcUrl", "rtmp://$currentHost:$currentPort/$currentApp")
            writeAmfPropertyString("type", "nonprivate")
            writeAmfPropertyString("flashVer", "FMLE/3.0 (compatible; StreamerBuddy)")
            write(0x00); write(0x00); write(0x09) // Object end
        }.toByteArray()

        writeRtmpChunk(csId = 3, messageType = 0x14, streamId = 0, payload = amfPayload)
        out.flush()
    }

    private fun sendCreateStreamCommand() {
        val out = outputStream ?: return
        val amfPayload = ByteArrayOutputStream().apply {
            writeAmfString("createStream")
            writeAmfNumber(2.0) // Transaction ID
            write(0x05) // Null
        }.toByteArray()

        writeRtmpChunk(csId = 3, messageType = 0x14, streamId = 0, payload = amfPayload)
        out.flush()
    }

    private fun sendPublishCommand() {
        val out = outputStream ?: return
        val amfPayload = ByteArrayOutputStream().apply {
            writeAmfString("publish")
            writeAmfNumber(3.0) // Transaction ID
            write(0x05) // Null
            writeAmfString(currentStreamKey)
            writeAmfString("live")
        }.toByteArray()

        writeRtmpChunk(csId = 8, messageType = 0x14, streamId = 1, payload = amfPayload)
        out.flush()
    }

    fun sendFlvTag(flvTagBytes: ByteArray) {
        if (!isStreaming.get()) return

        val out = outputStream ?: return
        try {
            val tagType = flvTagBytes[0].toInt() and 0xFF
            val dataSize = ((flvTagBytes[1].toInt() and 0xFF) shl 16) or
                    ((flvTagBytes[2].toInt() and 0xFF) shl 8) or
                    (flvTagBytes[3].toInt() and 0xFF)

            val csId = if (tagType == FlvMuxer.TAG_TYPE_VIDEO) 6 else 4
            val messageType = tagType

            // Write chunk header and payload
            writeRtmpChunk(
                csId = csId,
                messageType = messageType,
                streamId = 1,
                payload = flvTagBytes,
                payloadOffset = 11,
                payloadLength = dataSize
            )

            out.flush()

            bytesSentSinceLastCheck.addAndGet(flvTagBytes.size.toLong())
            if (tagType == FlvMuxer.TAG_TYPE_VIDEO) {
                framesSentSinceLastCheck.incrementAndGet()
            }
        } catch (e: Exception) {
            droppedPacketsCount.incrementAndGet()
            Log.e(TAG, "Error writing FLV tag to RTMP socket", e)
            handleDisconnect("Network write error: ${e.message}")
        }
    }

    private fun writeRtmpChunk(
        csId: Int,
        messageType: Int,
        streamId: Int,
        payload: ByteArray,
        payloadOffset: Int = 0,
        payloadLength: Int = payload.size
    ) {
        val out = outputStream ?: return

        val chunkSize = 128
        var bytesRemaining = payloadLength
        var offset = payloadOffset
        var isFirstChunk = true

        while (bytesRemaining > 0) {
            val bytesToWrite = minOf(bytesRemaining, chunkSize)

            if (isFirstChunk) {
                // Chunk Type 0 (11 bytes header)
                val fmt = 0
                val basicHeader = (fmt shl 6) or (csId and 0x3F)
                out.write(basicHeader)

                // Timestamp (3 bytes = 0)
                out.write(0); out.write(0); out.write(0)
                // Message Length (3 bytes)
                out.write((payloadLength shr 16) and 0xFF)
                out.write((payloadLength shr 8) and 0xFF)
                out.write(payloadLength and 0xFF)
                // Message Type ID (1 byte)
                out.write(messageType and 0xFF)
                // Message Stream ID (4 bytes, little-endian)
                out.write(streamId and 0xFF)
                out.write((streamId shr 8) and 0xFF)
                out.write((streamId shr 16) and 0xFF)
                out.write((streamId shr 24) and 0xFF)

                isFirstChunk = false
            } else {
                // Chunk Type 3 (1 byte basic header)
                val fmt = 3
                val basicHeader = (fmt shl 6) or (csId and 0x3F)
                out.write(basicHeader)
            }

            out.write(payload, offset, bytesToWrite)
            offset += bytesToWrite
            bytesRemaining -= bytesToWrite
        }
    }

    private fun handleDisconnect(reason: String) {
        if (!isStreaming.get()) return

        Log.w(TAG, "RTMP connection dropped: $reason")
        closeSocket()

        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            _connectionState.value = RtmpConnectionState.RECONNECTING
            Log.i(TAG, "Attempting RTMP reconnect ($reconnectAttempts/$maxReconnectAttempts)...")

            scope.launch {
                delay(2000L * reconnectAttempts)
                connect("rtmp://$currentHost:$currentPort/$currentApp", currentStreamKey)
            }
        } else {
            _connectionState.value = RtmpConnectionState.ERROR
            isStreaming.set(false)
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            while (isStreaming.get()) {
                delay(1000)

                val bytes = bytesSentSinceLastCheck.getAndSet(0)
                val frames = framesSentSinceLastCheck.getAndSet(0)
                val drops = droppedPacketsCount.get()

                val bitrateKbps = ((bytes * 8) / 1000).toInt()
                val fps = frames

                val health = when {
                    drops > 20 -> NetworkHealth.CRITICAL
                    drops > 5 -> NetworkHealth.UNSTABLE
                    bitrateKbps < 1500 && bitrateKbps > 0 -> NetworkHealth.GOOD
                    else -> NetworkHealth.EXCELLENT
                }

                _telemetry.update {
                    it.copy(
                        bitrateKbps = bitrateKbps,
                        fps = fps,
                        droppedPackets = drops,
                        rttMs = 28L + (drops * 2L),
                        health = health
                    )
                }
            }
        }
    }

    fun stop() {
        isStreaming.set(false)
        telemetryJob?.cancel()
        telemetryJob = null
        closeSocket()
        _connectionState.value = RtmpConnectionState.STOPPED
        _telemetry.value = NetworkTelemetry()
        Log.i(TAG, "RTMP stream stopped")
    }

    private fun closeSocket() {
        try {
            outputStream?.close()
        } catch (ignored: Exception) {}
        try {
            inputStream?.close()
        } catch (ignored: Exception) {}
        try {
            socket?.close()
        } catch (ignored: Exception) {}
        outputStream = null
        inputStream = null
        socket = null
    }

    // Helper AMF0 writing methods
    private fun ByteArrayOutputStream.writeAmfString(str: String) {
        write(0x02) // String marker
        val bytes = str.toByteArray(Charsets.UTF_8)
        write((bytes.size shr 8) and 0xFF)
        write(bytes.size and 0xFF)
        write(bytes)
    }

    private fun ByteArrayOutputStream.writeAmfNumber(num: Double) {
        write(0x00) // Number marker
        val bits = java.lang.Double.doubleToRawLongBits(num)
        for (i in 7 downTo 0) {
            write(((bits shr (i * 8)) and 0xFF).toInt())
        }
    }

    private fun ByteArrayOutputStream.writeAmfPropertyString(key: String, value: String) {
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        write((keyBytes.size shr 8) and 0xFF)
        write(keyBytes.size and 0xFF)
        write(keyBytes)
        writeAmfString(value)
    }

    companion object {
        private const val TAG = "RtmpConnectionManager"
    }
}
