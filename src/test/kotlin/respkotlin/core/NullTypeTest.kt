package respkotlin.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import respkotlin.core.NullType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NullTypeTest {

    @Test
    fun `test null deserializer`() {
        // given
        val data = "_\r\n".toByteArray()
        // when
        val result = NullType.deserialize(data)
        // then
        assert(result == Unit)
    }

    @Test
    fun `test null serializer`() {
        // given
        val data = Unit
        // when
        val result = NullType.serialize(data)
        // then
        assert(result.contentEquals("_\r\n".toByteArray()))
    }
}
