package respkotlin

import org.junit.jupiter.api.TestInstance
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerbatimStringTypeTest {

    @Test
    fun `test verbatim string deserializer`() {
        // given
        val data = "=15\r\ntxt:Some String\r\n".toByteArray()
        // when
        val result = VerbatimStringType.deserialize(data)
        // then
        assert(result.encoding == "txt")
        assert(result.data == "Some String")

        // given
        val data1 = "=15\r\ntxt:Some\nString\r\n".toByteArray()
        // when
        val result1 = VerbatimStringType.deserialize(data1)
        // then
        assert(result1.encoding == "txt")
        assert(result1.data == "Some\nString")
    }

    @Test
    fun `test verbatim string serializer`() {
        // given
        val data = VerbatimString("txt", "Some String")
        // when
        val result = VerbatimStringType.serialize(data)
        // then
        assert(result.contentEquals("=15\r\ntxt:Some String\r\n".toByteArray()))

        // given
        val data1 = VerbatimString("txt", "Some\nString")
        // when
        val result1 = VerbatimStringType.serialize(data1)
        // then
        assert(result1.contentEquals("=15\r\ntxt:Some\nString\r\n".toByteArray()))
    }
}