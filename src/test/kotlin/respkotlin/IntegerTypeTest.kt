package respkotlin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegerTypeTest {

    @Test
    fun `test integer deserializer`() {
        // given
        val data = ":1000\r\n".toByteArray()
        // when
        val result = IntegerType.deserialize(data)
        // then
        assert(result == 1000L)

        // given
        val data1 = ":-1000\r\n".toByteArray()
        // when
        val result1 = IntegerType.deserialize(data1)
        // then
        assert(result1 == -1000L)

        // given
        val data2 = ":+10\r\n".toByteArray()
        // when
        val result2 = IntegerType.deserialize(data2)
        // then
        assert(result2 == 10L)

        // given
        val data3 = ":0\r\n".toByteArray()
        // when
        val result3 = IntegerType.deserialize(data3)
        // then
        assert(result3 == 0L)

        // given
        val data4 = ":-0\r\n".toByteArray()
        // when
        val result4 = IntegerType.deserialize(data4)
        // then
        assert(result4 == 0L)
        assert(result4 == -0L)
    }

    @Test
    fun `test integer serializer`() {
        // given
        val data = 1000L
        // when
        val result = IntegerType.serialize(data)
        // then
        assert(result.contentEquals(":1000\r\n".toByteArray()))

        // given
        val data1 = -1000L
        // when
        val result1 = IntegerType.serialize(data1)
        // then
        assert(result1.contentEquals(":-1000\r\n".toByteArray()))

        // given
        val data2 = 0L
        // when
        val result2 = IntegerType.serialize(data2)
        // then
        assert(result2.contentEquals(":0\r\n".toByteArray()))

        // given
        val data3 = -0L
        // when
        val result3 = IntegerType.serialize(data3)
        // then
        assert(result3.contentEquals(":0\r\n".toByteArray()))
    }
}
