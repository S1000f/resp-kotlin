package core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import respkotlin.core.BooleanType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BooleanTypeTest {

    @Test
    fun `test boolean deserializer`() {
        // given
        val data = "#t\r\n".toByteArray()
        // when
        val result = BooleanType.deserialize(data)
        // then
        assert(result)

        // given
        val data1 = "#f\r\n".toByteArray()
        // when
        val result1 = BooleanType.deserialize(data1)
        // then
        assert(!result1)
    }

    @Test
    fun `test boolean serializer`() {
        // given
        val data = true
        // when
        val result = BooleanType.serialize(data)
        // then
        assert(result.contentEquals("#t\r\n".toByteArray()))

        // given
        val data1 = false
        // when
        val result1 = BooleanType.serialize(data1)
        // then
        assert(result1.contentEquals("#f\r\n".toByteArray()))
    }
}
