package respkotlin

import org.junit.jupiter.api.TestInstance
import respkotlin.core.*
import respkotlin.core.toDataType
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDateTime
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToolTest {

    @Test
    fun test() {
        val now = LocalDateTime.now()
        val serialize = LocalDateTimeType.serialize(now)
        println(String(serialize))

        val deserialize = LocalDateTimeType.deserialize(serialize)
        println(deserialize)
    }

    @Test
    fun test1() {
        registerDataType(LocalDateTime::class, LocalDateTimeType)

        val now = LocalDateTime.now()
        val serialize = LocalDateTimeType.serialize(now)

        when (val dataType = serialize.toDataType()) {
            is LocalDateTimeType -> {
                val deserialize = dataType.deserialize(serialize)
                println(deserialize)
            }

            else -> println("error")
        }
    }

    @Test
    fun test2() {
        registerDataType(LocalDateTime::class, LocalDateTimeType)

        val now = LocalDateTime.now()
        val list = listOf(now, 42L)
        val serialize = ArrayType.serialize(list)

        println(String(serialize))
    }

    @Test
    fun test3() {
        val data = "$24\r\nthis is the test strings\r\n".toByteArray()
        val inputStream = data.inputStream()

        val readResponse = readResponse(inputStream)
        println("test3: ${String(readResponse)}")
    }
}

fun readResponse(input: InputStream): ByteArray {
    val bufferSize = 8
    val buffer = ByteArray(bufferSize)
    val read = input.read(buffer)

    if (read < bufferSize) {
        return buffer
    }

    val dataType = buffer.toDataType()

    var countElement = when (dataType) {
        is SimpleType -> 1
        is AggregateType -> when (dataType) {
            is ContainerType -> dataType.length(buffer)
            else -> 1
        }
    }

    if (dataType is SimpleType) {
        return readSimpleType(input, buffer)
    }

    if (dataType is AggregateType && dataType !is ContainerType) {
        return readAggregateType(input, buffer)
    }

    while (countElement > 0) {
        countElement--
    }

    return byteArrayOf()
}

fun readSimpleType(input: InputStream, preRead: ByteArray): ByteArray {
    val buffer = ByteArray(8)
    val outputStream = ByteArrayOutputStream()
    var readBytes: Int

    while (input.read(buffer).also { readBytes = it } != -1) {
        outputStream.write(buffer, 0, readBytes)

        if (buffer.last() == '\n'.code.toByte()) {
            break
        }
    }

    return preRead + outputStream.toByteArray()
}

fun readAggregateType(input: InputStream, preRead: ByteArray): ByteArray {
    val buffer = ByteArray(8)
    val outputStream = ByteArrayOutputStream()

    var length = preRead.toDataType().length(preRead) + TERMINATOR.length
    var readBytes: Int

    while (input.read(buffer).also { readBytes = it } != -1) {
        outputStream.write(buffer, 0, readBytes)
        length -= readBytes

        if (length == 0) {
            break
        }
    }

    return preRead + outputStream.toByteArray()
}

fun readContainerType(input: InputStream, preRead: ByteArray): ByteArray {
    val dataType = preRead.toDataType()
    val countElement = dataType.length(preRead)
    val outputStream = ByteArrayOutputStream()
    val buffer = ByteArray(8)

    while (countElement > 0) {
        var readBytes: Int

        while (input.read(buffer).also { readBytes = it } != -1) {
            outputStream.write(buffer, 0, readBytes)

        }
    }


    return ByteArray(0)
}

object LocalDateTimeType : SimpleType<LocalDateTime, LocalDateTime> {
    override fun serialize(data: LocalDateTime): ByteArray {
        return "$firstByte$data$TERMINATOR".toByteArray()
    }

    override fun deserialize(data: ByteArray): LocalDateTime {
        return LocalDateTime.parse(String(data, 1, data.size - 3))
    }

    override val firstByte: Char
        get() = 't'

    override val length: (ByteArray) -> Int
        get() = { it.size - TERMINATOR.length }
}