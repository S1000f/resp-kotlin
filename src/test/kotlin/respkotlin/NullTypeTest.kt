package respkotlin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NullTypeTest {

    @Test
    fun `test null deserializer`() {
        // given
        val data = "_\r\n".toByteArray()
        // when
        val result = NullType.deserialize(data)
        // then
        assertNull(result)
    }

    @Test
    fun `test null serializer`() {
        // when
        val result = NullType.serialize(null)
        // then
        assert(result.contentEquals("_\r\n".toByteArray()))
    }
}
