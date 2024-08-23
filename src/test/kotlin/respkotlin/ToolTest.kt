package respkotlin

import org.junit.jupiter.api.TestInstance
import respkotlin.core.*
import respkotlin.core.toDataType
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
    fun `test readResponse with simple type`() {
        // given
        val data = "+This is the test simple string\r\n".toByteArray()
        val inputStream = data.inputStream()
        // when
        val readResponse = readResponse(inputStream)
        // then
        assert(readResponse.contentEquals(data))

        // given
        val data1 = "-This is the test error string\r\n".toByteArray()
        val inputStream1 = data1.inputStream()
        // when
        val readResponse1 = readResponse(inputStream1, 8)
        // then
        assert(readResponse1.contentEquals(data1))
    }

    @Test
    fun `test readResponse with aggregate type`() {
        // given
        val data = "$23\r\nthis is the test string\r\n".toByteArray()
        val inputStream = data.inputStream()
        // when
        val readResponse = readResponse(inputStream)
        // then
        assert(readResponse.contentEquals(data))

        // given
        val data1 = "$37\r\nthis is the test string\nhello, world!\r\n".toByteArray()
        val inputStream1 = data1.inputStream()
        // when
        val readResponse1 = readResponse(inputStream1, 8)
        // then
        assert(readResponse1.contentEquals(data1))
    }

    @Test
    fun `test readResponse with array container type`() {
        // given
        val data = "*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n".toByteArray()
        val inputStream = data.inputStream()
        // when
        val readResponse = readResponse(inputStream)
        // then
        assert(readResponse.contentEquals(data))

        // given
        val data1 = "*3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n:42\r\n".toByteArray()
        val inputStream1 = data1.inputStream()
        // when
        val readResponse1 = readResponse(inputStream1, 8)
        // then
        assert(readResponse1.contentEquals(data1))

        // given
        val data2 = "~3\r\n+ok\r\n+hello world\r\n+kotlin\r\n".toByteArray()
        val inputStream2 = data2.inputStream()
        // when
        val readResponse2 = readResponse(inputStream2)
        // then
        assert(readResponse2.contentEquals(data2))
    }

    @Test
    fun `test readResponse with nested array`() {
        // given
        val data = "*2\r\n$3\r\nfoo\r\n*2\r\n$3\r\nbar\r\n:42\r\n".toByteArray()
        val inputStream = data.inputStream()
        // when
        val readResponse = readResponse(inputStream)
        // then
        assert(readResponse.contentEquals(data))

        // given
        val data1 = "*2\r\n~1\r\n+foo\r\n*2\r\n+bar\r\n:42\r\n".toByteArray()
        val inputStream1 = data1.inputStream()
        // when
        val readResponse1 = readResponse(inputStream1, 8)
        // then
        assert(readResponse1.contentEquals(data1))

        // given
        val data2 = "*2\r\n%1\r\n+foo\r\n:42\r\n*2\r\n$3\r\nbaz\r\n:3\r\n".toByteArray()
        val inputStream2 = data2.inputStream()
        // when
        val readResponse2 = readResponse(inputStream2, 8)
        // then
        assert(readResponse2.contentEquals(data2))
    }

    @Test
    fun `test readResponse with map container type`() {
        // given
        val data = "%2\r\n+key\r\n$3\r\nfoo\r\n+key2\r\n:42\r\n".toByteArray()
        val inputStream = data.inputStream()
        // when
        val readResponse = readResponse(inputStream)
        // then
        assert(readResponse.contentEquals(data))

        // given
        val data1 = "%2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$2\r\nbz\r\n:42\r\n".toByteArray()
        val inputStream1 = data1.inputStream()
        // when
        val readResponse1 = readResponse(inputStream1, 8)
        // then
        assert(readResponse1.contentEquals(data1))
    }

    @Test
    fun `test readResponse with nested map container`() {
        // given
        val data = "%2\r\n$3\r\nkey\r\n*2\r\n+foo\r\n:42\r\n$3\r\nbar\r\n$3\r\nbaz\r\n".toByteArray()
        val inputStream = data.inputStream()
        // when
        val readResponse = readResponse(inputStream, 8)
        // then
        assert(readResponse.contentEquals(data))

        // given
        val data1 = "%2\r\n*1\r\n+key\r\n%1\r\n+foo\r\n:42\r\n*2\r\n+bar\r\n$3\r\nbaz\r\n:1000\r\n".toByteArray()
        val inputStream1 = data1.inputStream()
        // when
        val readResponse1 = readResponse(inputStream1, 8)
        // then
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
}
