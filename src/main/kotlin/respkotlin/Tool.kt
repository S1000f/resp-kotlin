package respkotlin

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
    require(bufferSize > 0) { "Buffer size must be greater than 0" }

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
    val normalized = readUntilTerminator(input, preRead)
    val indexOfFirst = normalized.indexOfFirst { it == '\n'.code.toByte() }

    return when {
        (indexOfFirst < normalized.size - 1) -> normalized.sliceArray(0..indexOfFirst)
        else -> normalized
    }
}

private fun readAggregateType(input: InputStream, preRead: ByteArray): ByteArray {
    val normalized = readUntilTerminator(input, preRead)
    val length = normalized.toDataType().length(normalized) + TERMINATOR.length
    val prefix = normalized.lengthUntilTerminator() + TERMINATOR.length

    return when {
        (normalized.size >= prefix + length) -> normalized.sliceArray(0..<prefix + length)
        else -> normalized + input.readNBytes(length - normalized.size + prefix)
    }
}

private fun readContainerType(input: InputStream, preRead: ByteArray): ByteArray {
    val normalized = readUntilTerminator(input, preRead)

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
            round.size > bytes.size -> round.sliceArray(bytes.size..<round.size)
            else -> ByteArray(1) { input.read().toByte() }
        }
    }

    val data = when {
        collect.isEmpty() -> ByteArray(0)
        else -> collect.reduce(ByteArray::plus)
    }

    return normalized.sliceArray(0..<prefix) + data
}

private fun readUntilTerminator(input: InputStream, preRead: ByteArray = ByteArray(0)) = when {
    !preRead.contains('\n'.code.toByte()) -> {
        val collect = mutableListOf<Byte>()

        while (true) {
            val read = input.read()
            if (read == -1) break
            collect.add(read.toByte())
            if (read == '\n'.code) break
        }

        if (preRead.isNotEmpty()) preRead + collect.toByteArray() else collect.toByteArray()
    }

    else -> preRead
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