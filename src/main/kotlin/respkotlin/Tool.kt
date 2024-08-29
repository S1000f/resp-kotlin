package respkotlin

import java.io.InputStream
import kotlin.reflect.KClass

/**
 * Register a custom data type for serialization and deserialization.
 *
 * Once registered, the custom data type can be used in the container types of the `RESP` protocol,
 * such as [ArrayType], [SetType] or [MapType].
 *
 * You can use [SimpleType] or [BulkType] to implement your custom [DataType].
 *
 * @param kClass The class of the custom data type.
 * @param dataType The custom data type to register.
 */
fun <S : Any, D : Any> registerDataType(kClass: KClass<S>, dataType: DataType<S, D>) =
    putCustomDataType(kClass, dataType)

/**
 * Create a command from a list of strings.
 *
 * The byte array returned is in the form of an array of bulk strings.
 */
fun createCommand(command: List<String>) =
    command.map { BulkStringType.serialize(it) }
        .fold("*${command.size}\r\n".toByteArray(), ByteArray::plus)

/**
 * Create a command from a list of strings.
 *
 * The byte array returned is in the form of an array of bulk strings.
 */
fun createCommand(vararg command: String) = createCommand(command.toList())

/**
 * Create a command from a list of strings.
 *
 * The byte array returned is in the form of an array of bulk strings.
 */
fun List<String>.toCommand() = createCommand(this)

/**
 * Create the `HELLO` command.
 *
 * In order to use `RESP3`, the client must first send a `HELLO` command to the server.
 */
fun helloCommand(proto: Long) = createCommand("HELLO", proto.toString())

/**
 * Create the `HELLO` command.
 *
 * In order to use `RESP3`, the client must first send a `HELLO` command to the server.
 */
fun helloCommand(proto: Long, vararg args: String) = createCommand("HELLO", proto.toString(), *args)

/**
 * It represents the response of the `HELLO` command.
 *
 * [server], [version] and [proto] are mandatory fields.
 */
interface HelloResponse {
    /**
     * The server name.
     */
    val server: String
    /**
     * The server version.
     */
    val version: String
    /**
     * The protocol version.
     */
    val proto: Long
    /**
     * The connection's ID(client ID).
     */
    val id: Long?
    /**
     * The connection's mode.
     */
    val mode: String?
    /**
     * The connection's role.
     */
    val role: String?
    /**
     * The list of loaded modules.
     */
    val modules: List<String>?
    /**
     * Get an attribute by key.
     */
    fun getAttribute(key: String): Any?

    private data class HelloResponseDefault(
        override val server: String,
        override val version: String,
        override val proto: Long,
        override val id: Long?,
        override val mode: String?,
        override val role: String?,
        override val modules: List<String>?
    ) : HelloResponse {
        private val attributes = mutableMapOf<String, Any?>()

        fun addAttribute(key: String, value: Any?) {
            attributes[key] = value
        }

        override fun getAttribute(key: String) = attributes[key]
    }

    companion object {
        /**
         * Create a [HelloResponse] from a map.
         */
        fun create(data: Map<Any, Any?>): HelloResponse {
            val server = data["server"] as String
            val version = data["version"] as String
            val proto = data["proto"] as Long
            val id = data["id"] as Long?
            val mode = data["mode"] as String?
            val role = data["role"] as String?

            @Suppress("UNCHECKED_CAST")
            val modules = data["modules"] as? List<String> ?: emptyList()

            val response = HelloResponseDefault(server, version, proto, id, mode, role, modules)
            data.forEach { (key, value) -> response.addAttribute(key.toString(), value) }

            return response
        }

        /**
         * Create a [HelloResponse] from a byte array.
         */
        fun create(data: ByteArray) = create(MapType.deserialize(data))
    }
}

/**
 * Read all the `RESP` data from the input stream.
 *
 * The function reads all bytes that consist of a one valid `RESP` data type.
 */
fun readResponse(input: InputStream, bufferSize: Int = 1024): ByteArray {
    require(bufferSize > 0) { "Buffer size must be greater than 0" }

    val buffer = ByteArray(bufferSize)
    val read = input.read(buffer)

    if (read < bufferSize) {
        return buffer.copyOf(read)
    }

    return when (buffer.toDataType()) {
        is SimpleType -> readSimpleType(input, buffer)
        is BulkType -> readBulkType(input, buffer)
        is AggregateType -> readAggregateType(input, buffer)
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

private fun readBulkType(input: InputStream, preRead: ByteArray): ByteArray {
    val normalized = readUntilTerminator(input, preRead)
    val length = normalized.toDataType().length(normalized) + TERMINATOR.length
    val prefix = normalized.lengthUntilTerminator() + TERMINATOR.length

    return when {
        (normalized.size >= prefix + length) -> normalized.sliceArray(0..<prefix + length)
        else -> normalized + input.readNBytes(length - normalized.size + prefix)
    }
}

private fun readAggregateType(input: InputStream, preRead: ByteArray): ByteArray {
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
        val bytes = when (round.toDataType()) {
            is SimpleType -> readSimpleType(input, round)
            is BulkType -> readBulkType(input, round)
            is AggregateType -> readAggregateType(input, round)
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
