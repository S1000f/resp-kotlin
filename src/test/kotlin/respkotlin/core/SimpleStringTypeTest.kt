package respkotlin.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import respkotlin.core.SimpleStringType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleStringTypeTest {

    @Test
    fun `test simple string deserializer`() {
        // given
        val data = "+OK\r\n".toByteArray()
        // when
        val result = SimpleStringType.deserialize(data)
        // then
        assert(result == "OK")

        // given
        val data1 = "++\r\n".toByteArray()
        // when
        val result1 = SimpleStringType.deserialize(data1)
        // then
        assert(result1 == "+")
    }

    @Test
    fun `test simple string serializer`() {
        // given
        val data = "OK"
        // when
        val result = SimpleStringType.serialize(data)
        // then
        assert(result.contentEquals("+OK\r\n".toByteArray()))

        // given
        val data1 = "+"
        // when
        val result1 = SimpleStringType.serialize(data1)
        // then
        assert(result1.contentEquals("++\r\n".toByteArray()))
    }
}
