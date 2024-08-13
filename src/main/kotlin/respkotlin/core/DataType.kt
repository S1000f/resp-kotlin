package respkotlin.core

const val TERMINATOR = "\r\n"
const val TERMINATOR_FIRST_BYTE = '\r'.code

fun interface Deserializer<out T> {
    fun deserialize(data: ByteArray): T
}

fun interface Serializer<in T> {
    fun serialize(data: T): ByteArray
}

sealed interface DataCategory {
    val length: (ByteArray) -> Int
}

sealed interface DataType<T> : Deserializer<T>, Serializer<T>, DataCategory

sealed interface SimpleType<T> : DataType<T> {
    override val length: (ByteArray) -> Int
        get() = { it.lengthUntilTerminator() }
}

sealed interface AggregateType<T> : DataType<T> {
    override val length: (ByteArray) -> Int
        get() = { it.length() }
}

sealed interface ContainerType<T> : AggregateType<T>

object SimpleStringType : SimpleType<String> {
    override fun serialize(data: String): ByteArray {
        return "+$data$TERMINATOR".toByteArray()
    }

    override fun deserialize(data: ByteArray) = String(data, 1, length(data) - 1)
}

object IntegerType : SimpleType<Long> {
    override fun serialize(data: Long): ByteArray {
        TODO("Not yet implemented")
    }

    override fun deserialize(data: ByteArray): Long {
        val (isPositive, index) = if (data[1].toInt() == '-'.code || data[1].toInt() == '+'.code) {
            (data[1].toInt() == '+'.code) to 2
        } else {
            true to 1
        }

        val long = String(data, index, data.lengthUntilTerminator(index) - index).toLong()

        return if (isPositive) long else -long
    }
}

object BulkStringType : AggregateType<String> {
    override fun serialize(data: String): ByteArray {
        TODO("Not yet implemented")
    }

    override fun deserialize(data: ByteArray): String {
        val length = length(data)
        return if (length == 0) "" else String(data, data.lengthUntilTerminator() + TERMINATOR.length, length)
    }
}

object ArrayType : ContainerType<List<Any>> {
    override fun serialize(data: List<Any>): ByteArray {
        TODO("Not yet implemented")
    }

    override fun deserialize(data: ByteArray): List<Any> {
        return splitElements(data).first
    }
}

private fun splitElements(data: ByteArray): Pair<List<Any>, Int> {
    val numOfElements = data.length()
    val list = mutableListOf<Any>()
    val prefix = data.lengthUntilTerminator() + TERMINATOR.length

    var round = data.sliceArray(prefix..<data.size)
    var count = 0
    var totalLength = prefix

    while (count < numOfElements) {
        count++

        val (element, len) = when (val dataType = round.toDataType()) {
            is SimpleType -> {
                val len = dataType.length(round) + TERMINATOR.length
                dataType.deserialize(round.sliceArray(0..<len)) to len
            }

            is AggregateType -> when (dataType) {
                is ContainerType -> splitElements(round)
                else -> {
                    val len = round.lengthUntilTerminator() + dataType.length(round) + TERMINATOR.length * 2
                    dataType.deserialize(round.sliceArray(0..<len)) to len
                }
            }
        }

        list.add(element)
        totalLength += len

        if (count == numOfElements) {
            break
        }

        round = round.sliceArray(len..<round.size)
    }

    return list.toList() to totalLength
}

private val dataTypeMap = mutableMapOf(
    '+'.code to SimpleStringType,
    ':'.code to IntegerType,
    '$'.code to BulkStringType,
    '*'.code to ArrayType
)

private fun ByteArray.toDataType(): DataType<out Any> {
    val firstByte = this[0].toInt()
    val dataType = dataTypeMap[firstByte] ?: throw IllegalArgumentException("Unknown data type: $firstByte")

    return dataType
}

private fun ByteArray.lengthUntilTerminator(offset: Int = 0): Int {
    var len = offset

    while (this[len].toInt() != TERMINATOR_FIRST_BYTE) {
        len++
    }

    return len
}

private fun ByteArray.length(): Int {
    var len = 0
    var i = 1

    while (this[i].toInt() != TERMINATOR_FIRST_BYTE) {
        len = len * 10 + (this[i] - '0'.code)
        i++
    }

    return len
}