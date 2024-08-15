package respkotlin

import respkotlin.core.*
import kotlin.reflect.KClass

fun <S : Any, D: Any> registerDataType(kClass: KClass<S>, dataType: DataType<S, D>) {
    putCustomDataType(kClass, dataType)
}

fun <T> exchange(command: ByteArray, deserializer: Deserializer<T>): T {
    println("Command: ${String(command)}")
    val byteArray = ByteArray(1024)

    return deserializer.deserialize(byteArray)
}

fun exchange(command: ByteArray): Pair<Deserializer<Any>, ByteArray> {
    return SimpleErrorType to "-ERR unknown error\r\n".toByteArray()
}

fun test() {

    val comm = ArrayType.serialize(listOf("GET", "key"))
    val (dataType, response) = exchange(comm)
    when (dataType) {
        is SimpleStringType -> dataType.deserialize(response)
        is ErrorType -> {
            val error = dataType.deserialize(response)
            println("Error: ${error.prefix} ${error.message}")
        }
    }
}