package core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import respkotlin.core.SimpleError
import respkotlin.core.SimpleErrorType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleErrorTypeTest {

    @Test
    fun `test simpleError data class`() {
        SimpleError("ERR", "unknown command 'foobar'").apply {
            assert(prefix == "ERR")
            assert(message == "unknown command 'foobar'")
        }

        assertThrows<IllegalArgumentException> { SimpleError("ERR", "unknown\ncommand") }
        assertThrows<IllegalArgumentException> { SimpleError("ERR", "\runknown command") }
        assertThrows<IllegalArgumentException> { SimpleError("ERR", "unknown\r\ncommand") }
    }

    @Test
    fun `test simple error deserializer`() {
        // given
        val data = "-ERR unknown command 'foobar'\r\n".toByteArray()
        // when
        val result = SimpleErrorType.deserialize(data)
        // then
        assert(result.prefix == "ERR")
        assert(result.message == "unknown command 'foobar'")

        // given
        val data1 = "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n".toByteArray()
        // when
        val result1 = SimpleErrorType.deserialize(data1)
        // then
        assert(result1.prefix == "WRONGTYPE")
        assert(result1.message == "Operation against a key holding the wrong kind of value")
    }

    @Test
    fun `test simple error serializer`() {
        // given
        val data = SimpleError("ERR", "unknown command 'foobar'")
        // when
        val result = SimpleErrorType.serialize(data)
        // then
        assert(result.contentEquals("-ERR unknown command 'foobar'\r\n".toByteArray()))

        // given
        val data1 = SimpleError("WRONGTYPE", "Operation against a key holding the wrong kind of value")
        // when
        val result1 = SimpleErrorType.serialize(data1)
        // then
        assert(result1.contentEquals("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n".toByteArray()))
    }
}
