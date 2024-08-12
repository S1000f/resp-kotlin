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

sealed interface SimpleDataType<T> : DataType<T> {
    override val length: (ByteArray) -> Int
        get() = { it.lengthUntilTerminator() }
}

sealed interface AggregateDataType<T> : DataType<T> {
    override val length: (ByteArray) -> Int
        get() = { it.length() }
}

object SimpleStringType : SimpleDataType<String> {

    override fun serialize(data: String): ByteArray {
        return "+$data$TERMINATOR".toByteArray()
    }

    override fun deserialize(data: ByteArray) = String(data, 1, data.lengthUntilTerminator() - 1)
}

object IntegerType : SimpleDataType<Long> {

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

object BulkStringType : AggregateDataType<String> {

    override fun serialize(data: String): ByteArray {
        TODO("Not yet implemented")
    }

    override fun deserialize(data: ByteArray): String {
        val length = data.length()
        return if (length == 0) "" else String(data, data.lengthUntilTerminator() + TERMINATOR.length, length)
    }
}

object ArrayType : AggregateDataType<List<Any>> {

    override fun serialize(data: List<Any>): ByteArray {
        TODO("Not yet implemented")
    }

    override fun deserialize(data: ByteArray): List<Any> {
        val numOfElements = data.length()
        val list = mutableListOf<Any>()
        var round = data.sliceArray(data.lengthUntilTerminator() + TERMINATOR.length..<data.size)
        var roundType = round.toDataType()

        var count = 0
        var offset: Int

        while (count < numOfElements) {
            count++
            val value = roundType.deserialize(round)
            list.add(value)

            if (count == numOfElements) {
                break
            }

            offset = when (roundType) {
                is SimpleDataType -> roundType.length(round) + TERMINATOR.length
                is AggregateDataType -> round.lengthUntilTerminator() + roundType.length(round) + TERMINATOR.length * 2
            }

            round = round.sliceArray(offset..<round.size)
            roundType = round.toDataType()
        }

        return list.toList()
    }
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