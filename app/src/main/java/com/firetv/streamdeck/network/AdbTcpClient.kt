package com.firetv.streamdeck.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AdbTcpClient(
    private val host: String,
    private val port: Int = 5555,
    private val timeoutMs: Int = 3000
) {
    suspend fun testConnection(): Result<Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            val latency = System.currentTimeMillis() - startTime
            Result.success(latency)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    suspend fun executeShellCommand(shellCommand: String): Result<String> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.soTimeout = timeoutMs
            socket.connect(InetSocketAddress(host, port), timeoutMs)

            val outputStream: OutputStream = socket.getOutputStream()
            val inputStream: InputStream = socket.getInputStream()

            val systemId = "host::FireTvDeck\u0000".toByteArray(Charsets.US_ASCII)
            val cnxnPacket = createAdbPacket("CNXN", 0x01000000, 4096, systemId)
            outputStream.write(cnxnPacket)
            outputStream.flush()

            val headerBuffer = ByteArray(24)
            readFully(inputStream, headerBuffer)

            val cmdPayload = "shell:\u0000".toByteArray(Charsets.US_ASCII)
            val openPacket = createAdbPacket("OPEN", 1, 0, cmdPayload)
            outputStream.write(openPacket)
            outputStream.flush()

            val responseBuilder = StringBuilder()
            val dataBuffer = ByteArray(1024)
            var bytesRead: Int

            try {
                while (inputStream.read(dataBuffer).also { bytesRead = it } != -1) {
                    if (bytesRead >= 24) {
                        val cmdTag = String(dataBuffer, 0, 4, Charsets.US_ASCII)
                        if (cmdTag == "WRTE" && bytesRead > 24) {
                            responseBuilder.append(String(dataBuffer, 24, bytesRead - 24, Charsets.UTF_8))
                            val okayPacket = createAdbPacket("OKAY", 1, 1, ByteArray(0))
                            outputStream.write(okayPacket)
                            outputStream.flush()
                        } else if (cmdTag == "CLSE") {
                            break
                        }
                    }
                }
            } catch (_: Exception) {}

            Result.success(responseBuilder.toString().ifBlank { "Comando inviato con successo" })
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private fun createAdbPacket(cmd: String, arg0: Int, arg1: Int, payload: ByteArray): ByteArray {
        val cmdBytes = cmd.toByteArray(Charsets.US_ASCII)
        var crc = 0L
        for (b in payload) {
            crc = (crc + (b.toInt() and 0xFF)) and 0xFFFFFFFFL
        }

        val magic = ByteBuffer.wrap(cmdBytes).order(ByteOrder.LITTLE_ENDIAN).int xor -1

        val buffer = ByteBuffer.allocate(24 + payload.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(cmdBytes)
        buffer.putInt(arg0)
        buffer.putInt(arg1)
        buffer.putInt(payload.size)
        buffer.putInt(crc.toInt())
        buffer.putInt(magic)
        buffer.put(payload)

        return buffer.array()
    }

    private fun readFully(inputStream: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val count = inputStream.read(buffer, offset, buffer.size - offset)
            if (count == -1) break
            offset += count
        }
    }
}
