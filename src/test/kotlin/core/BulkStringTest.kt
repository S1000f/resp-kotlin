package core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import respkotlin.core.BulkStringType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BulkStringTest {

    @Test
    fun `test bulk string deserializer`() {
        // given
        val data = "$3\r\nfoo\r\n".toByteArray()
        // when
        val result = BulkStringType.deserialize(data)
        // then
        assert(result == "foo")

        // given
        val data1 = "$0\r\n\r\n".toByteArray()
        // when
        val result1 = BulkStringType.deserialize(data1)
        // then
        assert(result1 == "")

        // given
        val data2 = "$12\r\nHello\nworld!\r\n".toByteArray()
        // when
        val result2 = BulkStringType.deserialize(data2)
        // then
        assert(result2 == "Hello\nworld!")
    }

    @Test
    fun `test bulk string serializer`() {
        // given
        val data = "foo"
        // when
        val result = BulkStringType.serialize(data)
        // then
        assert(result.contentEquals("$3\r\nfoo\r\n".toByteArray()))

        // given
        val data1 = ""
        // when
        val result1 = BulkStringType.serialize(data1)
        // then
        assert(result1.contentEquals("$0\r\n\r\n".toByteArray()))

        // given
        val data2 = "Hello\nworld!"
        // when
        val result2 = BulkStringType.serialize(data2)
        // then
        assert(result2.contentEquals("$12\r\nHello\nworld!\r\n".toByteArray()))
    }
}
