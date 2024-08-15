package respkotlin.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import respkotlin.core.BigNumberType
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BigNumberTypeTest {

    @Test
    fun `test big number deserializer`() {
        // given
        val data = "(3492890328409238509324850943850943825024385\r\n".toByteArray()
        // when
        val result = BigNumberType.deserialize(data)
        // then
        assert(result == BigInteger("3492890328409238509324850943850943825024385"))

        // given
        val data1 = "(-3492890328409238509324850943850943825024385\r\n".toByteArray()
        // when
        val result1 = BigNumberType.deserialize(data1)
        // then
        assert(result1 == BigInteger("-3492890328409238509324850943850943825024385"))
    }

    @Test
    fun `test big number serializer`() {
        // given
        val data = BigInteger("3492890328409238509324850943850943825024385")
        // when
        val result = BigNumberType.serialize(data)
        // then
        assert(result.contentEquals("(3492890328409238509324850943850943825024385\r\n".toByteArray()))

        // given
        val data1 = BigInteger("-3492890328409238509324850943850943825024385")
        // when
        val result1 = BigNumberType.serialize(data1)
        // then
        assert(result1.contentEquals("(-3492890328409238509324850943850943825024385\r\n".toByteArray()))
    }
}
