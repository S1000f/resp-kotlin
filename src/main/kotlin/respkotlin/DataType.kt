package respkotlin

import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * The terminator used in RESP protocol.
 */
const val TERMINATOR = "\r\n"

/**
 * The first byte of the terminator.
 */
const val TERMINATOR_FIRST_BYTE = '\r'.code.toByte()

/**
 * This trait represents a feature that can serialize data to a byte array.
 *
 * @param S the type of the data to serialize
 */
fun interface Serializer<in S> {
    fun serialize(data: S): ByteArray
}

/**
 * This trait represents a feature that can deserialize data from a byte array.
 *
 * @param D the type of the data to deserialize
 */
fun interface Deserializer<out D> {
    fun deserialize(data: ByteArray): D
}

/**
 * It represents a category of data according to the `RESP`.
 * One of the following: Simple, Bulk or Aggregate.
 *
 * Simple and Bulk data types are used to represent a literal value.
 * Aggregate data types are used to represent a container type, such as List, Set or Dictionary.
 *
 * [length] has different meanings according to the data type:
 * - Simple type, it calculates the length until the terminator including a type identifier. For example, the following
 * data has a length of 3:
 * ```shell
 * +OK\r\n
 * ```
 *
 * - Bulk type, it returns the fixed length of the data. For example, the following data has a length of 5:
 * ```shell
 * $5\r\nhello\r\n
 * ```
 *
 * - Aggregate type, it returns the number of elements the data holds. For example, the following data has a length of 2:
 * ```shell
 * *2\r\n+foo\r\n+bar\r\n
 * ```
 */
sealed interface DataCategory {
    val length: (ByteArray) -> Int
}

/**
 * It represents a data type that is corresponding to the `RESP`.
 *
 * All data types in `RESP` have [firstByte] that identifies the type of the data.
 * This is literally the first byte of the serialized data.
 *
 * For example, you can tell the following data is a Simple String type because the first byte is `+`:
 * ```shell
 * +OK\r\n
 * ```
 *
 * @param S the type of the data to serialize
 * @param D the type of the data to deserialize
 */
sealed interface DataType<S, D> : Serializer<S>, Deserializer<D>, DataCategory {
    val firstByte: Char
}

/**
 * It represents a simple data type in `RESP`.
 *
 * @param S the type of the data to serialize
 * @param D the type of the data to deserialize
 * @see SimpleStringType
 * @see SimpleErrorType
 * @see IntegerType
 * @see NullType
 * @see BooleanType
 * @see DoubleType
 * @see BigNumberType
 */
interface SimpleType<S, D> : DataType<S, D> {
    override val length: (ByteArray) -> Int
        get() = { it.lengthUntilTerminator() }
}

/**
 * It represents a bulk data type in `RESP`.
 *
 * @param S the type of the data to serialize
 * @param D the type of the data to deserialize
 * @see BulkStringType
 * @see BulkErrorType
 * @see VerbatimStringType
 */
interface BulkType<S, D> : DataType<S, D> {
    override val length: (ByteArray) -> Int
        get() = { it.length() }
}

/**
 * It represents an aggregate data type in `RESP`.
 *
 * @param S the type of the data to serialize
 * @param D the type of the data to deserialize
 * @see ArrayType
 * @see MapType
 * @see SetType
 * @see PushType
 */
sealed interface AggregateType<S, D> : DataType<S, D> {
    override val length: (ByteArray) -> Int
        get() = { it.length() }
}

/**
 * It is a marker interface for error types.
 * In `RESP`, there are two types of error: Simple Error and Bulk Error.
 *
 * @see Error
 * @see SimpleErrorType
 * @see BulkErrorType
 */
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

object BulkStringType : BulkType<String, String> {
    override fun serialize(data: String) = "$firstByte${data.length}$TERMINATOR$data$TERMINATOR".toByteArray()

    override fun deserialize(data: ByteArray) =
        String(data, data.lengthUntilTerminator() + TERMINATOR.length, length(data))

    override val firstByte get() = '$'
}

object ArrayType : AggregateType<List<Any>, List<Any>> {
    override fun serialize(data: List<Any>) = serializeContainer(data)
    override fun deserialize(data: ByteArray) = deserializeArray(data, mutableListOf()).first.toList()
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

object BulkErrorType : BulkType<BulkError, Error>, ErrorType {
    override fun serialize(data: BulkError) =
        "${data.prefix} ${data.message}".let { "$firstByte${it.length}$TERMINATOR$it$TERMINATOR".toByteArray() }

    override fun deserialize(data: ByteArray) = BulkStringType.deserialize(data).let {
        val split = it.split(" ", limit = 2)
        BulkError(split[0], split[1])
    }

    override val firstByte get() = '!'
}

object VerbatimStringType : BulkType<VerbatimString, VerbatimString> {
    override fun serialize(data: VerbatimString) = "${data.encoding}:${data.data}".let {
        "$firstByte${it.length}$TERMINATOR$it$TERMINATOR".toByteArray()
    }

    override fun deserialize(data: ByteArray) = BulkStringType.deserialize(data).let {
        val split = it.split(':', limit = 2)
        VerbatimString(split[0], split[1])
    }

    override val firstByte get() = '='
}

object MapType : AggregateType<Map<*, *>, Map<Any, Any>> {
    override fun serialize(data: Map<*, *>) = serializeContainer(data)
    override fun deserialize(data: ByteArray) = deserializeMap(data).first
    override val firstByte get() = '%'
}

object SetType : AggregateType<Set<Any>, Set<Any>> {
    override fun serialize(data: Set<Any>) = serializeContainer(data)
    override fun deserialize(data: ByteArray) = deserializeArray(data, mutableSetOf()).first.toSet()
    override val firstByte get() = '~'
}

object PushType : AggregateType<List<Any>, List<Any>> {
    override fun serialize(data: List<Any>) = when (data.isEmpty()) {
        true -> "${firstByte}0$TERMINATOR".toByteArray()
        false -> {
            val list = data.map { serializeContainer(it) }
            list.fold("$firstByte${list.size}$TERMINATOR".toByteArray(), ByteArray::plus)
        }
    }

    override fun deserialize(data: ByteArray) = deserializeArray(data, mutableListOf()).first.toList()
    override val firstByte get() = '>'
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

    is Set<*> -> when (data.isEmpty()) {
        true -> "${SetType.firstByte}0$TERMINATOR".toByteArray()
        false -> {
            val collect = data.map { serializeContainer(it!!) }
            collect.fold("${SetType.firstByte}${collect.size}$TERMINATOR".toByteArray(), ByteArray::plus)
        }
    }

    else -> {
        val dataType = findCustomDataType(data::class) ?: throw IllegalArgumentException("Unknown data type: $data")
        dataType.serialize(data)
    }
}

private fun <T : MutableCollection<Any>> deserializeArray(data: ByteArray, container: T): Pair<T, Int> {
    val numOfElements = data.length()
    val prefix = data.lengthUntilTerminator() + TERMINATOR.length

    var round = data.sliceArray(prefix..<data.size)
    var count = 0
    var totalLength = prefix

    while (count < numOfElements) {
        count++

        val (element, len) = deserializeElement(round)

        container.add(element)
        totalLength += len

        if (count == numOfElements) {
            break
        }

        round = round.sliceArray(len..<round.size)
    }

    return container to totalLength
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

    is BulkType -> {
        val len = data.lengthUntilTerminator() + dataType.length(data) + TERMINATOR.length * 2
        dataType.deserialize(data.sliceArray(0..<len)) to len
    }

    is AggregateType -> when (dataType) {
        is ArrayType -> deserializeArray(data, mutableListOf())
        is MapType -> deserializeMap(data)
        is SetType -> deserializeArray(data, mutableSetOf())
        is PushType -> deserializeArray(data, mutableListOf())
    }

}

internal val dataTypeMap = ConcurrentHashMap(
    mutableMapOf(
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
        MapType.firstByte.code to MapType,
        SetType.firstByte.code to SetType,
        PushType.firstByte.code to PushType
    )
)

fun ByteArray.toDataType(): DataType<out Any, out Any> {
    val firstByte = this[0].toInt()
    val dataType = dataTypeMap[firstByte] ?: throw IllegalArgumentException("Unknown data type: $firstByte")

    return dataType
}

private val customDataTypeMap = ConcurrentHashMap<KClass<Any>, DataType<Any, Any>>()

@Suppress("UNCHECKED_CAST")
internal fun <S : Any, D : Any> putCustomDataType(kClass: KClass<S>, dataType: DataType<S, D>) {
    kClass as? KClass<Any> ?: throw IllegalArgumentException("Type must be a subclass of Any")
    dataType as? DataType<Any, Any> ?: throw IllegalArgumentException("Data type must be a subclass of DataType")

    customDataTypeMap[kClass] = dataType
    dataTypeMap[dataType.firstByte.code] = dataType
}

private fun findCustomDataType(serType: KClass<out Any>): DataType<Any, out Any>? {
    return customDataTypeMap[serType]
}

internal fun ByteArray.lengthUntilTerminator() = this.indexOf(TERMINATOR_FIRST_BYTE)

private fun ByteArray.length(): Int {
    var len = 0
    var i = 1

    while (this[i] != TERMINATOR_FIRST_BYTE) {
        len = len * 10 + (this[i] - '0'.code)
        i++
    }

    return len
}
