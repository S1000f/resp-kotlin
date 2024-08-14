package respkotlin

import respkotlin.core.*

fun <T> exchange(command: ByteArray, deserializer: Deserializer<T>): T {
    println("Command: ${String(command)}")
    val byteArray = ByteArray(1024)

    return deserializer.deserialize(byteArray)
}

fun exchange(command: ByteArray): Pair<Deserializer<Any>, ByteArray> {
    return SimpleStringType to command
}

fun findDataType(data: ByteArray): Deserializer<Any> {
    return data.toDataType()
}
