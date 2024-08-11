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

fun interface Deserializer<out T> {
    fun deserialize(data: ByteArray): T
}

fun interface Serializer<T> {
    fun serialize(data: T): ByteArray
}

object SimpleString : Deserializer<String>, Serializer<String> {
    override fun deserialize(data: ByteArray): String {
        TODO("Not yet implemented")
    }

    override fun serialize(data: String): ByteArray {
        TODO("Not yet implemented")
    }
}

val simpleStringDeserializer = Deserializer { data ->
    String(data, 1, lengthOfData(data))
}

val integerDeserializer = Deserializer { data ->
    val (isPositive, index) = if (data[1].toInt() == '-'.code || data[1].toInt() == '+'.code) {
        (data[1].toInt() == '+'.code) to 2
    } else {
        true to 1
    }

    val long = String(data, index, lengthOfData(data, index)).toLong()

    if (isPositive) long else -long
}

val arrayDeserializer = Deserializer { data ->
    val numOfElements = numberOfElements(data)
    val list = mutableListOf<Any>()

    var index = 1
    while (data[index].toInt() != '\n'.code) {
        index++
    }

    index++
    var count = 0

    while (count < numOfElements) {
        val element = data.sliceArray(index..data.size)

        val dataType = matchDataType(element)
        val value = dataType.deserialize(element)
        list.add(value)

        val lengthOfElement = lengthOfData(data, index)

        index += lengthOfElement + 2
        count++
    }

    list.toList()
}

private val dataTypeMap = mutableMapOf(
    '+'.code to simpleStringDeserializer,
    '$'.code to integerDeserializer,
    '*'.code to arrayDeserializer
)

fun matchDataType(data: ByteArray): Deserializer<Any> {
    val firstByte = data[0].toInt()
    return dataTypeMap[firstByte] ?: throw IllegalArgumentException("Unknown data type: $firstByte")
}

fun lengthOfData(data: ByteArray, offset: Int = 1): Int {
    var len = 0
    var i = offset

    while (data[i].toInt() != '\r'.code) {
        len++
        i++
    }

    return len
}

fun numberOfElements(data: ByteArray): Int {
    var len = 0
    var i = 1

    while (data[i].toInt() != '\r'.code) {
        len = len * 10 + (data[i] - '0'.code)
        i++
    }

    return len
}