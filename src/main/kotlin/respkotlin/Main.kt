package respkotlin

import java.io.*
import java.net.Socket

fun sendCommand(output: OutputStream, command: ByteArray) {
    output.write(command)
    output.flush()
}

fun main() {
    val host = "127.0.0.1"
    val port = 6379

    val socket = Socket(host, port)
    val output = socket.getOutputStream()
    val input = socket.getInputStream()

    val command = createCommand("HELLO", "3")

    sendCommand(output, command)

    val response = readResponse(input)
    val deserialize = MapType.deserialize(response)
    println("deserialized: $deserialize")


    val command1 = createCommand("HELLO", "3")

    sendCommand(output, command1)

    val response1 = readResponse(input)
    val create = HelloResponse.create(response1)
    println("response: $create")

    socket.close()
}

