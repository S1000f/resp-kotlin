package core

import org.junit.jupiter.api.TestInstance
import respkotlin.core.*
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BulkErrorTypeTest {

    @Test
    fun `test bulk error deserializer`() {
        // given
        val data = "!44\r\nERR this is an error message\nanother message\r\n".toByteArray()
        // when
        val error = BulkErrorType.deserialize(data)
        // then
        assert(error.prefix == "ERR")
        assert(error.message == "this is an error message\nanother message")

        // given
        val data2 = "!21\r\nSYNTAX invalid syntax\r\n".toByteArray()
        // when
        val error2 = BulkErrorType.deserialize(data2)
        // then
        assert(error2.prefix == "SYNTAX")
        assert(error2.message == "invalid syntax")
    }

    @Test
    fun `test bulk error serializer`() {
        // given
        val error = BulkError("ERR", "this is an error message\nanother message")
        // when
        val data = BulkErrorType.serialize(error)
        // then
        assert(data.contentEquals("!44\r\nERR this is an error message\nanother message\r\n".toByteArray()))

        // given
        val error2 = BulkError("SYNTAX", "invalid syntax")
        // when
        val data2 = BulkErrorType.serialize(error2)
        // then
        assert(data2.contentEquals("!21\r\nSYNTAX invalid syntax\r\n".toByteArray()))
    }
}