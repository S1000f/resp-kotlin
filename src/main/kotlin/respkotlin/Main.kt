package respkotlin

import java.io.BufferedInputStream
import java.net.Socket
import java.io.OutputStream
import java.io.InputStream

fun sendCommand(output: OutputStream, command: String) {
    output.write(command.toByteArray())
    output.flush()
}

fun readResponse(input: InputStream): String {
    val buffer = ByteArray(1024)
    val bytesRead = input.read(buffer)
    return String(buffer, 0, bytesRead)
}

fun main() {
    val host = "127.0.0.1"
    val port = 6379

    Socket(host, port).use { socket ->
        val output: OutputStream = socket.getOutputStream()
        val input: InputStream = socket.getInputStream()

        val comm = "*4\r\n$4\r\nSADD\r\n$5\r\nmykey\r\n$3\r\nfoo\r\n$3\r\nbar\r\n"
        sendCommand(output, comm)
        println("SET Response: ${readResponse(input)}")

        val smembers = "*2\r\n$8\r\nSMEMBERS\r\n$5\r\nmykey\r\n"
        sendCommand(output, smembers)

        BufferedInputStream(input).use { bufferedInputStream ->
            val buffer = ByteArray(1024)
            val bytesRead = bufferedInputStream.read(buffer)

        }


        println("GET Response: ${readResponse(input)}")
    }

    val aggregate = "*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n"
    val aggreData = aggregate.toByteArray()
    length(aggreData)
    length(aggreData)
}

sealed interface Aggregate {
    val length: Int
}

fun length(data: ByteArray): Int {
    var len = 0
    var i = 1

    while (data[i].toInt() != '\r'.code) {
        len = len * 10 + (data[i] - '0'.code)
        i++
    }

    println("Length: $len")

    return len
}