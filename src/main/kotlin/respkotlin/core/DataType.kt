package respkotlin.core

import java.math.BigInteger

const val TERMINATOR = "\r\n"
const val TERMINATOR_FIRST_BYTE = '\r'.code.toByte()

fun interface Serializer<in S> {
    fun serialize(data: S): ByteArray
}

fun interface Deserializer<out D> {
    fun deserialize(data: ByteArray): D
}

sealed interface DataCategory {
    val firstByte: Char
    val length: (ByteArray) -> Int
}

sealed interface DataType<S, D> : Serializer<S>, Deserializer<D>, DataCategory

sealed interface SimpleType<S, D> : DataType<S, D> {
    override val length: (ByteArray) -> Int
        get() = { it.lengthUntilTerminator() }
}

sealed interface AggregateType<S, D> : DataType<S, D> {
    override val length: (ByteArray) -> Int
        get() = { it.length() }
}

sealed interface ContainerType<S, D> : AggregateType<S, D>

sealed interface ErrorType : Deserializer<Error>

sealed interface Error {
    val prefix: String
    val message: String
}

data class SimpleError(
    override val prefix: String,
    override val message: String
) : Error {
    init {
        require(!message.contains('\r') && !message.contains('\n')) {
            "Error message cannot contain '\\r' or '\\n'"
        }
    }
}

data class BulkError(
    override val prefix: String,
    override val message: String
) : Error

data class VerbatimString(
    val encoding: String,
    val data: String
) {
    init {
        require(encoding.length == 3) { "Encoding must be 3 characters long" }
    }
}

object SimpleStringType : SimpleType<String, String> {
    override fun serialize(data: String) = "$firstByte$data$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = String(data, 1, length(data) - 1)
    override val firstByte get() = '+'
}

object SimpleErrorType : SimpleType<SimpleError, Error>, ErrorType {
    override fun serialize(data: SimpleError) = "$firstByte${data.prefix} ${data.message}$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = SimpleStringType.deserialize(data).let {
        val split = it.split(" ", limit = 2)
        SimpleError(split[0], split[1])
    }

    override val firstByte get() = '-'
}

object IntegerType : SimpleType<Long, Long> {
    override fun serialize(data: Long) = "$firstByte${data}$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = String(data, 1, length(data) - 1).toLong()
    override val firstByte get() = ':'
}

object BulkStringType : AggregateType<String, String> {
    override fun serialize(data: String) = "$firstByte${data.length}$TERMINATOR$data$TERMINATOR".toByteArray()

    override fun deserialize(data: ByteArray) =
        String(data, data.lengthUntilTerminator() + TERMINATOR.length, length(data))

    override val firstByte get() = '$'
}

object ArrayType : ContainerType<List<Any>, List<Any>> {
    override fun serialize(data: List<Any>) = serializeContainer(data)
    override fun deserialize(data: ByteArray) = deserializeArray(data).first
    override val firstByte get() = '*'
}

object NullType : SimpleType<Unit, Unit> {
    override fun serialize(data: Unit) = "$firstByte$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = Unit
    override val firstByte get() = '_'
}

object BooleanType : SimpleType<Boolean, Boolean> {
    override fun serialize(data: Boolean) = "$firstByte${if (data) "t" else "f"}$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = data[1].toInt() == 't'.code
    override val firstByte get() = '#'
}

object DoubleType : SimpleType<Double, Double> {
    override fun serialize(data: Double) = when {
        data.isNaN() -> "nan"
        data == Double.POSITIVE_INFINITY -> "inf"
        data == Double.NEGATIVE_INFINITY -> "-inf"
        else -> data.toString()
    }.let { "$firstByte$it$TERMINATOR".toByteArray() }

    override fun deserialize(data: ByteArray) = when (data.elementAt(2)) {
        'n'.code.toByte() -> Double.POSITIVE_INFINITY
        'i'.code.toByte() -> Double.NEGATIVE_INFINITY
        'a'.code.toByte() -> Double.NaN
        else -> String(data, 1, length(data) - 1).toDouble()
    }

    override val firstByte get() = ','
}

object BigNumberType : SimpleType<BigInteger, BigInteger> {
    override fun serialize(data: BigInteger) = "$firstByte$data$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = String(data, 1, length(data) - 1).toBigInteger()
    override val firstByte get() = '('
}

object BulkErrorType : AggregateType<BulkError, Error>, ErrorType {
    override fun serialize(data: BulkError) =
        "${data.prefix} ${data.message}".let { "$firstByte${it.length}$TERMINATOR$it$TERMINATOR".toByteArray() }

    override fun deserialize(data: ByteArray) = BulkStringType.deserialize(data).let {
        val split = it.split(" ", limit = 2)
        BulkError(split[0], split[1])
    }

    override val firstByte get() = '!'
}

object VerbatimStringType : AggregateType<VerbatimString, VerbatimString> {
    override fun serialize(data: VerbatimString) = "${data.encoding}:${data.data}".let {
        "$firstByte${it.length}$TERMINATOR$it$TERMINATOR".toByteArray()
    }

    override fun deserialize(data: ByteArray) = BulkStringType.deserialize(data).let {
        val split = it.split(':', limit = 2)
        VerbatimString(split[0], split[1])
    }

    override val firstByte get() = '='
}

object MapType : ContainerType<Map<*, *>, Map<Any, Any>> {
    override fun serialize(data: Map<*, *>) = serializeContainer(data)
    override fun deserialize(data: ByteArray) = deserializeMap(data).first
    override val firstByte get() = '%'
}

private fun serializeContainer(data: Any): ByteArray = when (data) {
    is String -> when (data.contains('\r') || data.contains('\n')) {
        true -> BulkStringType.serialize(data)
        false -> SimpleStringType.serialize(data)
    }

    is SimpleError -> SimpleErrorType.serialize(data)
    is Int -> IntegerType.serialize(data.toLong())
    is Long -> IntegerType.serialize(data)
    is List<*> -> when (data.isEmpty()) {
        true -> "${ArrayType.firstByte}0$TERMINATOR".toByteArray()
        false -> {
            val collect = data.map { serializeContainer(it!!) }
            collect.fold("${ArrayType.firstByte}${collect.size}$TERMINATOR".toByteArray(), ByteArray::plus)
        }
    }

    is Unit -> NullType.serialize(Unit)
    is Boolean -> BooleanType.serialize(data)
    is Double -> DoubleType.serialize(data)
    is BigInteger -> BigNumberType.serialize(data)
    is BulkError -> BulkErrorType.serialize(data)
    is VerbatimString -> VerbatimStringType.serialize(data)
    is Map<*, *> -> when (data.isEmpty()) {
        true -> "${MapType.firstByte}0$TERMINATOR".toByteArray()
        false -> {
            val collect = data.map { (k, v) -> serializeContainer(k!!) + serializeContainer(v!!) }
            collect.fold("${MapType.firstByte}${collect.size}$TERMINATOR".toByteArray(), ByteArray::plus)
        }
    }

    else -> throw IllegalArgumentException("Unknown data type: $data")
}

private fun deserializeArray(data: ByteArray): Pair<List<Any>, Int> {
    val numOfElements = data.length()
    val list = mutableListOf<Any>()
    val prefix = data.lengthUntilTerminator() + TERMINATOR.length

    var round = data.sliceArray(prefix..<data.size)
    var count = 0
    var totalLength = prefix

    while (count < numOfElements) {
        count++

        val (element, len) = deserializeElement(round)

        list.add(element)
        totalLength += len

        if (count == numOfElements) {
            break
        }

        round = round.sliceArray(len..<round.size)
    }

    return list.toList() to totalLength
}

private fun deserializeMap(data: ByteArray): Pair<Map<Any, Any>, Int> {
    val numOfElements = data.length()
    val map = mutableMapOf<Any, Any>()
    val prefix = data.lengthUntilTerminator() + TERMINATOR.length

    var round = data.sliceArray(prefix..<data.size)
    var count = 0
    var totalLength = prefix

    while (count < numOfElements) {
        count++

        val (key, keyLen) = deserializeElement(round)
        round = round.sliceArray(keyLen..<round.size)
        val (value, valueLen) = deserializeElement(round)

        map[key] = value
        totalLength += (keyLen + valueLen)

        if (count == numOfElements) {
            break
        }

        round = round.sliceArray(valueLen..<round.size)
    }

    return map.toMap() to totalLength
}

private fun deserializeElement(data: ByteArray) = when (val dataType = data.toDataType()) {
    is SimpleType -> {
        val len = dataType.length(data) + TERMINATOR.length
        dataType.deserialize(data.sliceArray(0..<len)) to len
    }

    is AggregateType -> when (dataType) {
        is ContainerType -> when (dataType) {
            is ArrayType -> deserializeArray(data)
            is MapType -> deserializeMap(data)
        }

        else -> {
            val len = data.lengthUntilTerminator() + dataType.length(data) + TERMINATOR.length * 2
            dataType.deserialize(data.sliceArray(0..<len)) to len
        }
    }
}

private val dataTypeMap = mutableMapOf(
    SimpleStringType.firstByte.code to SimpleStringType,
    SimpleErrorType.firstByte.code to SimpleErrorType,
    IntegerType.firstByte.code to IntegerType,
    BulkStringType.firstByte.code to BulkStringType,
    ArrayType.firstByte.code to ArrayType,
    NullType.firstByte.code to NullType,
    BooleanType.firstByte.code to BooleanType,
    DoubleType.firstByte.code to DoubleType,
    BigNumberType.firstByte.code to BigNumberType,
    BulkErrorType.firstByte.code to BulkErrorType,
    VerbatimStringType.firstByte.code to VerbatimStringType,
    MapType.firstByte.code to MapType
)

internal fun ByteArray.toDataType(): DataType<out Any, out Any> {
    val firstByte = this[0].toInt()
    val dataType = dataTypeMap[firstByte] ?: throw IllegalArgumentException("Unknown data type: $firstByte")

    return dataType
}

fun ByteArray.lengthUntilTerminator() = this.indexOf(TERMINATOR_FIRST_BYTE)

private fun ByteArray.length(): Int {
    var len = 0
    var i = 1

    while (this[i] != TERMINATOR_FIRST_BYTE) {
        len = len * 10 + (this[i] - '0'.code)
        i++
    }

    return len
}
