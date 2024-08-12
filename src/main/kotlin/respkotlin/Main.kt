package respkotlin

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
        println("GET Response: ${readResponse(input)}")
    }

    val aggregate = "*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n"
    val data = aggregate.toByteArray()

}

