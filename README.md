# RESP-Kotlin

This is a Kotlin implementation of RESP3 (Redis Serialization Protocol).
RESP is a simple and easy-to-use protocol for serializing data and communicating between clients and servers.
See the [official documentation](https://redis.io/docs/latest/develop/reference/protocol-spec/) for more information.

## 1. Quick Start

You can use a function `DataType::serialize` to serialize data. It could be a command for a server or just a data
structure itself.

Once you receive a response from a server, use a function `DataType::deserialize` to deserialize it.
You can check the type of the response using the extension function `ByteArray.toDataType`.

```kotlin
fun get(key: String): String? {
    val host = "127.0.0.1"
    val port = 6379

    Socket(host, port).use { socket ->
        val output = socket.getOutputStream()
        val command = ArrayType.serialize(listOf("GET", key))
        output.write(command)
        output.flush()

        val response = readResponse(socket.getInputStream())

        return when (val dataType = response.toDataType()) {
            is BulkStringType -> dataType.deserialize(response)
            is NullType -> null
            is ErrorType -> {
                val error: Error = dataType.deserialize(response)
                throw IllegalStateException("${error.prefix} ${error.message}")
            }
            else -> throw IllegalStateException("Unexpected data type")
        }
    }
}
```

<br/>

## 2. Codec

The library provides a codec for encoding and decoding RESP data.
The data types are categorized into three groups: Simple, Bulk and Aggregate.

- Simple type
    - simple types are used to represent literal values like strings, integers, booleans, etc.
    - \<identifier>\<data>\r\n
    - for example, `+OK\r\n`
- Bulk type
    - bulk types has a metadata that indicates the length of the data in bytes.
    - \<identifier>\r\n\<length>\r\n\<data>\r\n
    - for example, `$13\r\nHello, world!\r\n`
- Aggregate type
    - aggregate types are used to represent containers like arrays, maps, sets, etc.
    - \<identifier>\r\n\<num-of-elements>\r\n\<data...>\r\n
    - for example, `*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n`

The following table shows the correspondence between RESP data types and Kotlin types.

| Data Type        | Category  | Identifier | Corresponding Kotlin Type                   |
|------------------|-----------|------------|---------------------------------------------|
| Simple strings   | Simple    | +          | String                                      |
| Simple errors    | Simple    | -          | *Error (presented by the library)*          |
| Integers         | Simple    | :          | Long                                        |
| Bulk strings     | Bulk      | $          | String                                      |
| Arrays           | Aggregate | *          | List<Any?>                                  |
| Nulls            | Simple    | _          | null (Nothing?)                             |
| Booleans         | Simple    | #          | Boolean                                     |
| Doubles          | Simple    | ,          | Double                                      |
| Big numbers      | Simple    | (          | BigInteger (java.math)                      |
| Bulk errors      | Bulk      | !          | *Error (presented by the library)*          |
| Verbatim strings | Bulk      | =          | *VerbatimString (presented by the library)* |
| Maps             | Aggregate | %          | Map<Any, Any?>                              |
| Sets             | Aggregate | ~          | Set<Any?>                                   |
| Pushes           | Aggregate | \>         | List<Any?>                                  |


### 2.1 Serialization

You can serialize data using the `DataType::serialize` function.

```kotlin
val data = "Hello, world!"
val serialized = BulkStringType.serialize(data)

assert(serialized.contentEquals("\$13\r\nHello, world!\r\n".toByteArray()))
```

### 2.2 Deserialization

You can deserialize data using the `DataType::deserialize` function.

```kotlin
val data = "\$13\r\nHello, world!\r\n".toByteArray()
val deserialized = BulkStringType.deserialize(data)

assert(deserialized == "Hello, world!")
```

Note that the `DataType::serialize` and `DataType::deserialize` is a type of `(T) -> ByteArray`
and `(ByteArray) -> T` respectively.

You can pass the function as an argument to another function.

```kotlin
inline fun <reified T> exchange(command: ByteArray, deserializer: (ByteArray) -> T): T {
    Socket("127.0.0.1", 6379).use { socket ->
        val output = socket.getOutputStream()
        output.write(command)
        output.flush()

        val response = readResponse(socket.getInputStream())

        return deserializer(response)
    }
}

fun main() {
    val exchange: String = 
        exchange(listOf("SET", "key", "val").toCommand(), SimpleStringType::deserialize)
}
```

### 2.3 Type Checking

You can check the type of the response using the extension function `ByteArray.toDataType`.

```kotlin
val data = "\$13\r\nHello, world!\r\n".toByteArray()
val dataType = data.toDataType()

when (dataType) {
    is BulkStringType -> println("Bulk string")
    is ErrorType -> println("Error")
    else -> println("Unexpected data type")
}
```

<br/>

## 3. Tools

### 3.1 Registering Custom Data Types

Besides using RESP as the protocol for communication with *redis-like* servers, you can use it just like a binary-safe
serialization protocol for your data structures in any use case.

Now, consider you want to serialize a `LocalDateTime` instance (even though you can still serialize it to String manually).
You can create a custom data type for it.

```kotlin
object LocalDateTimeType : SimpleType<LocalDateTime, LocalDateTime> {
    override fun serialize(data: LocalDateTime) = "$firstByte$data$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = LocalDateTime.parse(String(data, 1, data.size - 3))
    override val firstByte: Char get() = 't'
}
```

The new data type must be SimpleType or BulkType. And the `firstByte` property must be unique among all data types
including built-in types.

Once you register the new data type, the library will be able to serialize and deserialize `LocalDateTime` with any
Aggregate type of RESP.

```kotlin
registerDataType(LocalDateTime::class, LocalDateTimeType)

val time = LocalDateTime.parse("2024-08-24T21:38:12")
val serialize = MapType.serialize(mapOf("timestamp" to time))
assert(serialize.contentEquals("%1\r\n$9\r\ntimestamp\r\nt2024-08-24T21:38:12\r\n".toByteArray()))
```

### 3.2 configuring USE_BULK_STRING

When this flag is set to true, the library will use only `BulkStringType` for serializing string elements in a container
such as `List<String>`, `Set<String>` etc.

Otherwise, the library will choose `SimpleStringType`, if the string does not contain `\r` and `\n` characters.

### 3.3 create command

You can create a command for a *redis-like* server using one of the following functions.

```kotlin
val comm = createCommand(listOf("SET", "key", "value"))
val comm1 = createCommand("SET", "key", "value")
val comm2 = listOf("SET", "key", "value").toCommand()
```

### 3.4 read response

the server may use TCP-KEEPALIVE to keep the connection alive. So, you may need to read the response without receiving the
end of the stream.

the function `readResponse` reads all bytes from the input stream so that it can make a proper byte array of RESP type.

