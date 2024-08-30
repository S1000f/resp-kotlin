package respkotlin

import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToolTest {

    @Test
    fun `test readResponse with simple type`() {
        for (size in 1..34) {
            // given
            val data = "+This is the test simple string\r\n".toByteArray()
            val inputStream = data.inputStream()
            // when
            val readResponse = readResponse(inputStream, size)
            // then
            assert(readResponse.contentEquals(data))
        }

        for (size in 1..6) {
            // given
            val data1 = ":42\r\n".toByteArray()
            val inputStream1 = data1.inputStream()
            // when
            val readResponse1 = readResponse(inputStream1, size)
            // then
            assert(readResponse1.contentEquals(data1))
        }

        for (size in 1..7) {
            // given
            val data2 = ":-42\r\n".toByteArray()
            val inputStream2 = data2.inputStream()
            // when
            val readResponse2 = readResponse(inputStream2, size)
            // then
            assert(readResponse2.contentEquals(data2))
        }

        for (size in 1..6) {
            // given
            val data2 = "-ERR this is an error message\r\n".toByteArray()
            val inputStream2 = data2.inputStream()
            // when
            val readResponse2 = readResponse(inputStream2, size)
            // then
            assert(readResponse2.contentEquals(data2))
        }
    }

    @Test
    fun `test readResponse with aggregate type`() {
        for (size in 1..31) {
            // given
            val data = "$23\r\nthis is the test string\r\n".toByteArray()
            val inputStream = data.inputStream()
            // when
            val readResponse = readResponse(inputStream, size)
            // then
            assert(readResponse.contentEquals(data))
        }

        for (size in 1..45) {
            // given
            val data1 = "$37\r\nthis is the test string\nhello, world!\r\n".toByteArray()
            val inputStream1 = data1.inputStream()
            // when
            val readResponse1 = readResponse(inputStream1, size)
            // then
            assert(readResponse1.contentEquals(data1))
        }

        for (size in 1..23) {
            // given
            val data1 = "=15\r\ntxt:Some string\r\n".toByteArray()
            val inputStream1 = data1.inputStream()
            // when
            val readResponse1 = readResponse(inputStream1, size)
            // then
            assert(readResponse1.contentEquals(data1))
        }
    }

    @Test
    fun `test readResponse with array container type`() {
        for (size in 1..19) {
            // given
            val data = "*2\r\n+okr\n$3\r\nbar\r\n".toByteArray()
            val inputStream = data.inputStream()
            // when
            val response = readResponse(inputStream, size)
            // then
            assert(response.contentEquals(data))
        }

        for (size in 1..27) {
            // given
            val data1 = "*3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n:-42\r\n".toByteArray()
            val inputStream1 = data1.inputStream()
            // when
            val readResponse1 = readResponse(inputStream1, size)
            // then
            assert(readResponse1.contentEquals(data1))
        }

        for (size in 1..32) {
            // given
            val data2 = "~3\r\n+ok\r\n+hello world\r\n+kotlin\r\n".toByteArray()
            val inputStream2 = data2.inputStream()
            // when
            val readResponse2 = readResponse(inputStream2, size)
            // then
            assert(readResponse2.contentEquals(data2))
        }
    }

    @Test
    fun `test readResponse with nested array`() {
        for (size in 1..32) {
            // given
            val data = "*2\r\n$3\r\nfoo\r\n*2\r\n$3\r\nbar\r\n:+42\r\n".toByteArray()
            val inputStream = data.inputStream()
            // when
            val readResponse = readResponse(inputStream, size)
            // then
            assert(readResponse.contentEquals(data))
        }

        for (size in 1..30) {
            // given
            val data1 = "*2\r\n~1\r\n+foo\r\n*2\r\n+bar\r\n:42\r\n".toByteArray()
            val inputStream1 = data1.inputStream()
            // when
            val readResponse1 = readResponse(inputStream1, size)
            // then
            assert(readResponse1.contentEquals(data1))
        }

        for (size in 1..37) {
            // given
            val data2 = "*2\r\n%1\r\n+foo\r\n:42\r\n*2\r\n$3\r\nbaz\r\n:3\r\n".toByteArray()
            val inputStream2 = data2.inputStream()
            // when
            val readResponse2 = readResponse(inputStream2, size)
            // then
            assert(readResponse2.contentEquals(data2))
        }
    }

    @Test
    fun `test readResponse with map container type`() {
        for (size in 1..33) {
            // given
            val data = "%2\r\n+key\r\n$3\r\nfoo\r\n+key2\r\n:-42\r\n".toByteArray()
            val inputStream = data.inputStream()
            // when
            val readResponse = readResponse(inputStream, size)
            // then
            assert(readResponse.contentEquals(data))
        }

        for (size in 1..36) {
            // given
            val data1 = "%2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$2\r\nbz\r\n:42\r\n".toByteArray()
            val inputStream1 = data1.inputStream()
            // when
            val readResponse1 = readResponse(inputStream1, size)
            // then
            assert(readResponse1.contentEquals(data1))
        }
    }

    @Test
    fun `test readResponse with nested map container`() {
        for (size in 1..47) {
            // given
            val data = "%2\r\n$3\r\nkey\r\n*2\r\n+foo\r\n:42\r\n$3\r\nbar\r\n$3\r\nbaz\r\n".toByteArray()
            val inputStream = data.inputStream()
            // when
            val readResponse = readResponse(inputStream, size)
            // then
            assert(readResponse.contentEquals(data))
        }

        for (size in 1..57) {
            // given
            val data1 = "%2\r\n*1\r\n+key\r\n%1\r\n+foo\r\n:-42\r\n*2\r\n+bar\r\n$3\r\nbaz\r\n:1000\r\n".toByteArray()
            val inputStream1 = data1.inputStream()
            // when
            val readResponse1 = readResponse(inputStream1, size)
            // then
            assert(readResponse1.contentEquals(data1))
        }
    }

    @Test
    fun `test customized data type`() {
        // given
        val now = LocalDateTime.now()
        //when
        val serialize = LocalDateTimeType.serialize(now)
        val deserialize = LocalDateTimeType.deserialize(serialize)
        // then
        assert(now == deserialize)

        // given
        val data = "t2024-08-24T21:38:00.000000\r\n".toByteArray()
        // when
        val deserialize1 = LocalDateTimeType.deserialize(data)
        // then
        assert(LocalDateTime.parse("2024-08-24T21:38:00.000000") == deserialize1)
    }

    @Test
    fun `test registerDataType method`() {
        // given
        registerDataType(LocalDateTime::class, LocalDateTimeType)
        val now = LocalDateTime.now()
        val serialize = LocalDateTimeType.serialize(now)
        // when
        val findDataType = serialize.toDataType()
        // then
        assert(findDataType is SimpleType<*, *>)
        assert(findDataType is LocalDateTimeType)
    }

    @Test
    fun `test custom data type with a container`() {
        // given
        registerDataType(LocalDateTime::class, LocalDateTimeType)
        val now = LocalDateTime.now()
        val list = listOf(now, 42L)
        // when
        val serialize = ArrayType.serialize(list)
        // then
        val expect = "*2\r\n".toByteArray() + LocalDateTimeType.serialize(now) + ":42\r\n".toByteArray()
        assert(serialize.contentEquals(expect))

        // given
        val data = "*2\r\n$3\r\nfoo\r\nt2024-08-24T21:38:00.000000\r\n".toByteArray()
        // when
        val deserialize = ArrayType.deserialize(data)
        // then
        assert(deserialize[0] == "foo")
        assert(deserialize[1] == LocalDateTime.parse("2024-08-24T21:38:00.000000"))
    }

    @Test
    fun `test createCommand method`() {
        // given
        val command = "SET"
        val key = "key"
        val value = "value"
        // when
        val createCommand = createCommand(command, key, value)
        // then
        assert(createCommand.contentEquals("*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n".toByteArray()))

        // given
        val list = listOf(command, key, value)
        // when
        val createCommand1 = createCommand(list)
        // then
        assert(createCommand1.contentEquals("*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n".toByteArray()))

        // when
        val toCommand = list.toCommand()
        // then
        assert(toCommand.contentEquals("*3\r\n$3\r\nSET\r\n$3\r\nkey\r\n$5\r\nvalue\r\n".toByteArray()))
    }

    @Test
    fun `test HelloResponse`() {
        // given
        val data = "%7\r\n$6\r\nserver\r\n$5\r\nredis\r\n$7\r\nversion\r\n$5\r\n7.0.9\r\n$5\r\nproto\r\n:3\r\n$2\r\n" +
                "id\r\n:175\r\n$4\r\nmode\r\n$10\r\nstandalone\r\n$4\r\nrole\r\n$6\r\nmaster\r\n$7\r\nmodules\r\n*0\r\n"

        // when
        val helloResponse = HelloResponse.create(data.toByteArray())
        // then
        assert(helloResponse.server == "redis")
        assert(helloResponse.version == "7.0.9")
        assert(helloResponse.proto == 3L)
        assert(helloResponse.id == 175L)
        assert(helloResponse.mode == "standalone")
        assert(helloResponse.role == "master")
        assert(helloResponse.modules?.isEmpty() == true)

        // when
        val deserialize = MapType.deserialize(data.toByteArray())
        val helloResponse1 = HelloResponse.create(deserialize)
        // then
        assert(helloResponse1.server == "redis")
        assert(helloResponse1.version == "7.0.9")
        assert(helloResponse1.proto == 3L)

        // given
        val data1 = "%3\r\n$6\r\nserver\r\n$5\r\nredis\r\n$7\r\nversion\r\n$5\r\n7.0.9\r\n$5\r\nproto\r\n:3\r\n"
        // when
        val helloResponse2 = HelloResponse.create(data1.toByteArray())
        // then
        assert(helloResponse2.server == "redis")
        assert(helloResponse2.version == "7.0.9")
        assert(helloResponse2.proto == 3L)
        assert(helloResponse2.id == null)
        assert(helloResponse2.mode == null)
        assert(helloResponse2.role == null)
    }

    @Test
    fun `test helloCommand`() {
        // given
        val proto = 3L
        // when
        val helloCommand = helloCommand(proto)
        // then
        assert(helloCommand.contentEquals("*2\r\n$5\r\nHELLO\r\n$1\r\n3\r\n".toByteArray()))

        // given
        val args = arrayOf("key", "value")
        // when
        val helloCommand1 = helloCommand(proto, *args)
        // then
        assert(helloCommand1.contentEquals("*4\r\n$5\r\nHELLO\r\n$1\r\n3\r\n$3\r\nkey\r\n$5\r\nvalue\r\n".toByteArray()))
    }

    @Test
    fun `test configureUseOnlyBulkString`() {
        // given
        val list = listOf("Hello", "World")
        // when
        configureUseBulkString()
        val serialize = ArrayType.serialize(list)
        // then
        assert(serialize.contentEquals("*2\r\n$5\r\nHello\r\n$5\r\nWorld\r\n".toByteArray()))

        // when
        configureUseBulkString(false)
        val serialize1 = ArrayType.serialize(list)
        // then
        assert(serialize1.contentEquals("*2\r\n+Hello\r\n+World\r\n".toByteArray()))
    }
}

object LocalDateTimeType : SimpleType<LocalDateTime, LocalDateTime> {
    override fun serialize(data: LocalDateTime) = "$firstByte$data$TERMINATOR".toByteArray()
    override fun deserialize(data: ByteArray) = LocalDateTime.parse(String(data, 1, data.size - 3))
    override val firstByte: Char get() = 't'
}
