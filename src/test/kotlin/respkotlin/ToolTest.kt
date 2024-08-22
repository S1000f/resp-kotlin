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
        val data = "+This is the test simple string\r\n".toByteArray()
        val inputStream = data.inputStream()

        val readResponse = readResponse(inputStream)
        println("test3: ${String(readResponse)}")
    }

    @Test
    fun test4() {
        val data = "$23\r\nthis is the test string\r\n".toByteArray()
        val inputStream = data.inputStream()

        val readResponse = readResponse(inputStream)
        println("test4: ${String(readResponse)}")
    }

    @Test
    fun test5() {
        val data = "*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n".toByteArray()
        val inputStream = data.inputStream()

        val readResponse = readResponse(inputStream)

        assert(readResponse.contentEquals(data))
    }

    @Test
    fun test6() {
        val data1 = "%2\r\n+key\r\n$3\r\nfoo\r\n$3\r\nbar\r\n:42\r\n".toByteArray()
        val inputStream1 = data1.inputStream()

        val readResponse1 = readResponse(inputStream1)

        assert(readResponse1.contentEquals(data1))
    }
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