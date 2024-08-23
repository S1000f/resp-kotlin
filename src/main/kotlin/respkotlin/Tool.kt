package respkotlin

import respkotlin.core.*
import java.io.InputStream
import kotlin.reflect.KClass

fun <S : Any, D : Any> registerDataType(kClass: KClass<S>, dataType: DataType<S, D>) {
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

fun readResponse(input: InputStream, bufferSize: Int = 1024): ByteArray {
    val buffer = ByteArray(bufferSize)
    val read = input.read(buffer)

    if (read < bufferSize) {
        return buffer.copyOf(read)
    }

    return when (val dataType = buffer.toDataType()) {
        is SimpleType -> readSimpleType(input, buffer)
        is AggregateType -> when (dataType) {
            is ContainerType -> readContainerType(input, buffer)
            else -> readAggregateType(input, buffer)
        }
    }
}

private fun readSimpleType(input: InputStream, preRead: ByteArray): ByteArray {
    val collect = preRead.takeWhile { it != '\n'.code.toByte() }.let {
        if (it.last() == '\r'.code.toByte()) return it.toByteArray() + '\n'.code.toByte()
        else it.toByteArray()
    }

    return collect + readUntilTerminator(input)
}

private fun readAggregateType(input: InputStream, preRead: ByteArray): ByteArray {
    val normalized = normalize(input, preRead)
    val length = normalized.toDataType().length(normalized) + TERMINATOR.length
    val prefix = normalized.lengthUntilTerminator() + TERMINATOR.length

    val collect = when {
        length <= (normalized.size - prefix) ->
            return normalized.sliceArray(0..<prefix + length + TERMINATOR.length)

        else -> input.readNBytes(length - normalized.size + prefix)
    }

    return normalized + collect
}

private fun readContainerType(input: InputStream, preRead: ByteArray): ByteArray {
    val normalized = normalize(input, preRead)

    val size = when (val dataType = normalized.toDataType()) {
        is MapType -> dataType.length(normalized) * 2
        else -> dataType.length(normalized)
    }

    if (size == 0) return normalized

    val collect = mutableListOf<ByteArray>()
    val prefix = normalized.lengthUntilTerminator() + TERMINATOR.length

    var countRead = 0
    var round = when {
        normalized.size > prefix -> normalized.sliceArray(prefix..<normalized.size)
        else -> readUntilTerminator(input)
    }

    while (size > countRead) {
        val bytes = when (val dataType = round.toDataType()) {
            is SimpleType -> readSimpleType(input, round)
            is AggregateType -> when (dataType) {
                is ContainerType -> readContainerType(input, round)
                else -> readAggregateType(input, round)
            }
        }

        countRead++
        collect.add(bytes)

        if (countRead == size) break

        round = when {
            round.size > bytes.size -> round.sliceArray(0..round.size)
            else -> ByteArray(1) { input.read().toByte() }
        }
    }

    val data = when {
        collect.isEmpty() -> ByteArray(0)
        else -> collect.reduce(ByteArray::plus)
    }

    return normalized.sliceArray(0..<prefix) + data
}

private fun normalize(input: InputStream, preRead: ByteArray) = when {
    !preRead.contains('\n'.code.toByte()) -> preRead + readUntilTerminator(input)
    else -> preRead
}

private fun readUntilTerminator(input: InputStream): ByteArray {
    val collect = mutableListOf<Byte>()

    while (true) {
        val read = input.read()
        if (read == -1) break
        collect.add(read.toByte())
        if (read == '\n'.code) break
    }

    return collect.toByteArray()
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