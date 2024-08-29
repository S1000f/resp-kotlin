package respkotlin

import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

/**
 * The terminator used in RESP protocol.
 */
const val TERMINATOR = "\r\n"

/**
 * The first byte of the terminator.
 */
const val TERMINATOR_FIRST_BYTE = '\r'.code.toByte()

internal var USE_BULK_STRING = AtomicBoolean(true)

/**
 * This trait represents a feature that can serialize data to a byte array.
 *
 * @param S the type of the data to serialize
 */
fun interface Serializer<in S> {
    /**
     * Serializes the data to a byte array.
     *
     * @param data the data to serialize
     */
    fun serialize(data: S): ByteArray
}

/**
 * This trait represents a feature that can deserialize data from a byte array.
 *
 * @param D the type of the data to deserialize
 */
fun interface Deserializer<out D> {
    /**
     * Deserializes the data from a byte array.
     *
     * @param data the byte array to deserialize
     */
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
 * $5\r\nHello\r\n
 * ```
 *
 * - Aggregate type, it returns the number of elements the data holds. For example, the following data has a length of 2:
 * ```shell
 * *2\r\n+foo\r\n+bar\r\n
 * ```
 */
sealed interface DataCategory {
    /**
     * Calculates the length of the serialized data.
     *
     * For Simple and Bulk types, the length is the number of bytes of the actual data.
     *
     * On the other hand, for Aggregate types, the length is the number of elements the data contains.
     */
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
 * @see SimpleType
 * @see BulkType
 * @see AggregateType
 */
sealed interface DataType<S, D> : Serializer<S>, Deserializer<D>, DataCategory {
    /**
     * The first byte of the serialized data.
     * It is used to identify the type of the data uniquely.
     */
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
 */
sealed interface ErrorType : Deserializer<Error>

/**
 * It represents an error in `RESP`.
 *
 * Error has a [prefix] and a [message]. [prefix] is a short string that describes the error usually in uppercase.
 * [message] is a detailed description of the error.
 *
 * Note that the contents of [prefix] is not a part of `RESP` protocol. It is defined on the server-side.
 *
 * @see SimpleError
 * @see BulkError
 */
sealed interface Error {
    /**
     * Error prefix.
     */
    val prefix: String

    /**
     * Error message.
     */
    val message: String
}

/**
 * This data class represents a simple error in `RESP`.
 *
 * This type of error is used to represent an error that can be described in a single line.
 *
 * @throws IllegalArgumentException if the message contains '\r' or '\n'
 * @see SimpleErrorType
 */
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

/**
 * This data class represents a bulk error in `RESP`.
 *
 * This type of error is used to represent an error that can be described in multiple lines.
 *
 * @see BulkErrorType
 */
data class BulkError(
    override val prefix: String,
    override val message: String
) : Error

/**
 * This data class represents a verbatim string in `RESP`.
 *
 * It has an [encoding] property that is a metadata about the data's encoding.
 *
 * @throws IllegalArgumentException if the encoding is not 3 characters long
 * @see VerbatimStringType
 */
data class VerbatimString(
    /**
     * The metadata about the data's encoding.
     * It must be 3 characters long.
     */
    val encoding: String,
    /**
     * The actual data that is encoded.
     */
    val data: String
) {
    init {
        require(encoding.length == 3) { "Encoding must be 3 characters long" }
    }
}

/**
 * Simple strings are used to represent a literal value. It never contains '\r' or '\n'.
 * The first byte of the serialized data is `+`.
 *
 * For example:
 * ```kotlin
 * val data = "+OK\r\n".toByteArray()
 * val result = SimpleStringType.deserialize(data)
 * assert(result == "OK")
 * ```
 *
 * [BulkStringType] is also converted to a [String], but it can contain '\r' or '\n'.
 * @see BulkStringType
 *
 */
object SimpleStringType : SimpleType<String, String> {
    override fun serialize(data: String) = "$firstByte$data$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = String(data, 1, length(data) - 1)
    override val firstByte get() = '+'
}

/**
 * Simple errors are used to represent an error that can be described in a single line.
 * The first byte of the serialized data is `-`.
 *
 * For example:
 * ```kotlin
 * val data = "-ERR Something went wrong\r\n".toByteArray()
 * val result = SimpleErrorType.deserialize(data)
 * assert(result.prefix == "ERR")
 * assert(result.message == "Something went wrong")
 * ```
 *
 * @see BulkErrorType
 */
object SimpleErrorType : SimpleType<SimpleError, Error>, ErrorType {
    override fun serialize(data: SimpleError) = "$firstByte${data.prefix} ${data.message}$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = SimpleStringType.deserialize(data).let {
        val split = it.split(" ", limit = 2)
        SimpleError(split[0], split[1])
    }

    override val firstByte get() = '-'
}

/**
 * Integers are used to represent a 64-bit signed integer.
 * The first byte of the serialized data is `:`.
 *
 * For example:
 * ```kotlin
 * val data = ":42\r\n".toByteArray()
 * val result = IntegerType.deserialize(data)
 * assert(result == 42L)
 * ```
 *
 * @see BigNumberType
 */
object IntegerType : SimpleType<Long, Long> {
    override fun serialize(data: Long) = "$firstByte${data}$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = String(data, 1, length(data) - 1).toLong()
    override val firstByte get() = ':'
}

/**
 * Bulk strings are used to represent a binary safe string.
 * The first byte of the serialized data is `$`.
 * It can contain '\r' or '\n'.
 *
 * For example:
 * ```kotlin
 * val data = "$5\r\nHello\r\n".toByteArray()
 * val result = BulkStringType.deserialize(data)
 * assert(result == "Hello")
 * ```
 */
object BulkStringType : BulkType<String, String> {
    override fun serialize(data: String) = "$firstByte${data.length}$TERMINATOR$data$TERMINATOR".toByteArray()

    override fun deserialize(data: ByteArray) =
        String(data, data.lengthUntilTerminator() + TERMINATOR.length, length(data))

    override val firstByte get() = '$'
}

/**
 * Arrays are used to represent a list of `RESP` values.
 * The first byte of the serialized data is `*`.
 *
 * For example:
 * ```kotlin
 * val data = "*2\r\n+foo\r\n+bar\r\n".toByteArray()
 * val result = ArrayType.deserialize(data)
 * assert(result == listOf("foo", "bar"))
 * ```
 *
 * @see SetType
 */
object ArrayType : AggregateType<List<Any?>, List<Any?>> {
    override fun serialize(data: List<Any?>) = serializeContainer(data)
    override fun deserialize(data: ByteArray) = deserializeArray(data, mutableListOf()).first.toList()
    override val firstByte get() = '*'
}

/**
 * Null type is used to represent a null value.
 * The first byte of the serialized data is `_`.
 *
 * For example:
 * ```kotlin
 * val data = "_\r\n".toByteArray()
 * val result = NullType.deserialize(data)
 * assertNull(result)
 * ```
 */
object NullType : SimpleType<Nothing?, Nothing?> {
    override fun serialize(data: Nothing?) = "$firstByte$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = null
    override val firstByte get() = '_'
}

/**
 * Boolean type is used to represent a boolean value.
 * The first byte of the serialized data is `#`.
 *
 * For example:
 * ```kotlin
 * val data = "#t\r\n".toByteArray()
 * val result = BooleanType.deserialize(data)
 * assert(result)
 * ```
 */
object BooleanType : SimpleType<Boolean, Boolean> {
    override fun serialize(data: Boolean) = "$firstByte${if (data) "t" else "f"}$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = data[1].toInt() == 't'.code
    override val firstByte get() = '#'
}

/**
 * Double type is used to represent a double value.
 * The first byte of the serialized data is `,`.
 *
 * For example:
 * ```kotlin
 * val data = ",3.14\r\n".toByteArray()
 * val result = DoubleType.deserialize(data)
 * assert(result == 3.14)
 * ```
 *
 * The positive infinity, negative infinity and NaN values are encoded as follows:
 * - Positive infinity: `,+inf\r\n`
 * - Negative infinity: `,-inf\r\n`
 * - NaN: `,nan\r\n`
 */
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

/**
 * Big numbers are used to represent a large integer value outside the range of a 64-bit signed integer.
 * The first byte of the serialized data is `(`.
 *
 * For example:
 * ```kotlin
 * val data = "(3492890328409238509324850943850943825024385\r\n".toByteArray()
 * val result = BigNumberType.deserialize(data)
 * assert(result == BigInteger("3492890328409238509324850943850943825024385"))
 * ```
 */
object BigNumberType : SimpleType<BigInteger, BigInteger> {
    override fun serialize(data: BigInteger) = "$firstByte$data$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = String(data, 1, length(data) - 1).toBigInteger()
    override val firstByte get() = '('
}

/**
 * Bulk errors are used to represent an error that can be described in multiple lines.
 * The first byte of the serialized data is `!`.
 *
 * For example:
 * ```kotlin
 * val data = "!ERR Something went wrong\r\n".toByteArray()
 * val result = BulkErrorType.deserialize(data)
 * assert(result.prefix == "ERR")
 * assert(result.message == "Something went wrong")
 * ```
 */
object BulkErrorType : BulkType<BulkError, Error>, ErrorType {
    override fun serialize(data: BulkError) =
        "${data.prefix} ${data.message}".let { "$firstByte${it.length}$TERMINATOR$it$TERMINATOR".toByteArray() }

    override fun deserialize(data: ByteArray) = BulkStringType.deserialize(data).let {
        val split = it.split(" ", limit = 2)
        BulkError(split[0], split[1])
    }

    override val firstByte get() = '!'
}

/**
 * Verbatim strings are used to represent a binary safe string with an encoding metadata.
 * The first byte of the serialized data is `=`.
 *
 * the encoding metadata is exactly three bytes.
 *
 * For example:
 * ```kotlin
 * val data = "=15\r\ntxt:Some string\r\n".toByteArray()
 * val result = VerbatimStringType.deserialize(data)
 * assert(result.encoding == "txt")
 * assert(result.data == "Some string")
 * ```
 */
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

/**
 * Maps are used to represent a dictionary of `RESP` values.
 * The first byte of the serialized data is `%`.
 *
 * For example:
 * ```kotlin
 * val data = "%2\r\n+first\r\n:1\r\n+second\r\n:2\r\n".toByteArray()
 * val result = MapType.deserialize(data)
 * assert(result == mapOf("first" to 1L, "second" to 2L))
 * ```
 */
object MapType : AggregateType<Map<*, *>, Map<Any, Any?>> {
    override fun serialize(data: Map<*, *>) = serializeContainer(data)
    override fun deserialize(data: ByteArray) = deserializeMap(data).first
    override val firstByte get() = '%'
}

/**
 * Sets are used to represent a collection of unordered and unique `RESP` values.
 * The first byte of the serialized data is `~`.
 *
 * For example:
 * ```kotlin
 * val data = "~2\r\n+foo\r\n+bar\r\n".toByteArray()
 * val result = SetType.deserialize(data)
 * assert(result == setOf("foo", "bar"))
 * ```
 */
object SetType : AggregateType<Set<Any?>, Set<Any?>> {
    override fun serialize(data: Set<Any?>) = serializeContainer(data)
    override fun deserialize(data: ByteArray) = deserializeArray(data, mutableSetOf()).first.toSet()
    override val firstByte get() = '~'
}

/**
 * It is used in Push mode for connections. Push events are similar to Array types, only differing in the first byte.
 * The first byte of the serialized data is `>`.
 *
 * For example:
 * ```kotlin
 * val data = ">2\r\n+foo\r\n+bar\r\n".toByteArray()
 * val result = PushType.deserialize(data)
 * assert(result == listOf("foo", "bar"))
 * ```
 */
object PushType : AggregateType<List<Any?>, List<Any?>> {
    override fun serialize(data: List<Any?>) = when (data.isEmpty()) {
        true -> "${firstByte}0$TERMINATOR".toByteArray()
        false -> {
            val list = data.map { serializeContainer(it) }
            list.fold("$firstByte${list.size}$TERMINATOR".toByteArray(), ByteArray::plus)
        }
    }

    override fun deserialize(data: ByteArray) = deserializeArray(data, mutableListOf()).first.toList()
    override val firstByte get() = '>'
}

/**
 * Serializes the data to a byte array.
 *
 * It serializes all elements of the container and collects them into a single byte array by calling [serializeContainer]
 * recursively.
 *
 * @param data the data to serialize
 * @return the serialized data
 */
private fun serializeContainer(data: Any?): ByteArray = when (data) {
    is String -> when (USE_BULK_STRING.get()) {
        true -> BulkStringType.serialize(data)
        false -> when (data.contains('\r') || data.contains('\n')) {
            true -> BulkStringType.serialize(data)
            false -> SimpleStringType.serialize(data)
        }
    }

    is SimpleError -> SimpleErrorType.serialize(data)
    is Int -> IntegerType.serialize(data.toLong())
    is Long -> IntegerType.serialize(data)
    is List<*> -> when (data.isEmpty()) {
        true -> "${ArrayType.firstByte}0$TERMINATOR".toByteArray()
        false -> {
            val collect = data.map { serializeContainer(it) }
            collect.fold("${ArrayType.firstByte}${collect.size}$TERMINATOR".toByteArray(), ByteArray::plus)
        }
    }

    is Boolean -> BooleanType.serialize(data)
    is Double -> DoubleType.serialize(data)
    is BigInteger -> BigNumberType.serialize(data)
    is BulkError -> BulkErrorType.serialize(data)
    is VerbatimString -> VerbatimStringType.serialize(data)
    is Map<*, *> -> when (data.isEmpty()) {
        true -> "${MapType.firstByte}0$TERMINATOR".toByteArray()
        false -> {
            val collect = data.map { (k, v) -> serializeContainer(k) + serializeContainer(v) }
            collect.fold("${MapType.firstByte}${collect.size}$TERMINATOR".toByteArray(), ByteArray::plus)
        }
    }

    is Set<*> -> when (data.isEmpty()) {
        true -> "${SetType.firstByte}0$TERMINATOR".toByteArray()
        false -> {
            val collect = data.map { serializeContainer(it) }
            collect.fold("${SetType.firstByte}${collect.size}$TERMINATOR".toByteArray(), ByteArray::plus)
        }
    }

    else -> when (data) {
        null -> NullType.serialize(null)
        else -> {
            val dataType = findCustomDataType(data::class) ?: throw IllegalArgumentException("Unknown data type: $data")
            dataType.serialize(data)
        }
    }
}

/**
 * Deserializes the collection type data from a byte array.
 * It returns a pair of the collection and the total length of the serialized data.
 *
 * @param T the type of the collection, such as List, Set
 */
private fun <T : MutableCollection<Any?>> deserializeArray(data: ByteArray, container: T): Pair<T, Int> {
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

/**
 * Deserializes the Map data from a byte array.
 * It returns a pair of the map and the total length of the serialized data.
 */
private fun deserializeMap(data: ByteArray): Pair<Map<Any, Any?>, Int> {
    val numOfElements = data.length()
    val map = mutableMapOf<Any, Any?>()
    val prefix = data.lengthUntilTerminator() + TERMINATOR.length

    var round = data.sliceArray(prefix..<data.size)
    var count = 0
    var totalLength = prefix

    while (count < numOfElements) {
        count++

        val (key, keyLen) = deserializeElement(round)
        round = round.sliceArray(keyLen..<round.size)
        val (value, valueLen) = deserializeElement(round)

        if (key != null) map[key] = value
        totalLength += (keyLen + valueLen)

        if (count == numOfElements) {
            break
        }

        round = round.sliceArray(valueLen..<round.size)
    }

    return map.toMap() to totalLength
}

/**
 * Deserializes the data from a byte array.
 * It returns a pair of the deserialized data and the total length of the serialized data.
 */
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
        is ArrayType, PushType -> deserializeArray(data, mutableListOf())
        is MapType -> deserializeMap(data)
        is SetType -> deserializeArray(data, mutableSetOf())
    }
}

/**
 * The map that contains all data types in `RESP`.
 * The key is the first byte of the serialized data.
 */
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

/**
 * Returns a [DataType] corresponding to the received byte array.
 *
 * @throws IllegalArgumentException if the first byte is not a known data type
 */
fun ByteArray.toDataType(): DataType<out Any?, out Any?> {
    val firstByte = this[0].toInt()
    val dataType = dataTypeMap[firstByte] ?: throw IllegalArgumentException("Unknown data type: $firstByte")

    return dataType
}

/**
 * The map that contains all custom data types.
 */
private val customDataTypeMap = ConcurrentHashMap<KClass<Any>, DataType<Any, Any>>()

/**
 * Registers a custom data type.
 *
 * @param S the type of the data to serialize
 * @param D the type of the data to deserialize
 */
@Suppress("UNCHECKED_CAST")
internal fun <S : Any, D : Any> putCustomDataType(kClass: KClass<S>, dataType: DataType<S, D>) {
    kClass as? KClass<Any> ?: throw IllegalArgumentException("Type must be a subclass of Any")
    dataType as? DataType<Any, Any> ?: throw IllegalArgumentException("Data type must be a subclass of DataType")

    customDataTypeMap[kClass] = dataType
    dataTypeMap[dataType.firstByte.code] = dataType
}

/**
 * Returns a custom data type corresponding to the received class.
 */
private fun findCustomDataType(serType: KClass<out Any>): DataType<Any, out Any>? {
    return customDataTypeMap[serType]
}

/**
 * Returns a length of the data until the terminator.
 */
internal fun ByteArray.lengthUntilTerminator() = this.indexOf(TERMINATOR_FIRST_BYTE)

/**
 * Returns a length of the data.
 *
 * For Bulk types, the length is the number of bytes of the actual data.
 * On the other hand, for Aggregate types, the length is the number of elements the data contains.
 */
private fun ByteArray.length(): Int {
    var len = 0
    var i = 1

    while (this[i] != TERMINATOR_FIRST_BYTE) {
        len = len * 10 + (this[i] - '0'.code)
        i++
    }

    return len
}
