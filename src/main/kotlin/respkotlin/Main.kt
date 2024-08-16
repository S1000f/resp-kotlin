package respkotlin

import respkotlin.core.BulkStringType
import respkotlin.core.MapType
import java.io.*
import java.net.Socket

fun sendCommand(output: OutputStream, command: ByteArray) {
    output.write(command)
    output.flush()
}

fun readResponse(input: InputStream): ByteArray {
    val buffer = ByteArray(1024)
    input.read(buffer)

    return buffer
}

fun convertInputStreamToByteArray(inputStream: InputStream): ByteArray {
    val buffer = ByteArray(60) // Buffer size, e.g., 1KB
    val outputStream = ByteArrayOutputStream()
    var bytesRead: Int

    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
        outputStream.write(buffer, 0, bytesRead)
    }

    return outputStream.toByteArray()
}

fun main() {
    val host = "127.0.0.1"
    val port = 6379

    Socket(host, port).use { socket ->
        val output: OutputStream = socket.getOutputStream()
        val input: InputStream = socket.getInputStream()

        val hello = BulkStringType.serialize("HELLO")
        val proto = BulkStringType.serialize("3")
        val comm = "*2\r\n".toByteArray() + hello + proto

        sendCommand(output, comm)

        val response = convertInputStreamToByteArray(input)
        val deserialize = MapType.deserialize(response)
        println(deserialize)
    }

}

