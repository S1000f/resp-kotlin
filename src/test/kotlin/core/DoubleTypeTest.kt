package core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import respkotlin.core.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoubleTypeTest {

    @Test
    fun `test double deserializer`() {
        // given
        val data0 = ",+42\r\n".toByteArray()
        // when
        val result0 = DoubleType.deserialize(data0)
        // then
        assert(result0 == 42.0)

        // given
        val data = ",42\r\n".toByteArray()
        // when
        val result = DoubleType.deserialize(data)
        // then
        assert(result == 42.0)

        // given
        val data1 = ",-42\r\n".toByteArray()
        // when
        val result1 = DoubleType.deserialize(data1)
        // then
        assert(result1 == -42.0)

        // given
        val data2 = ",0\r\n".toByteArray()
        // when
        val result2 = DoubleType.deserialize(data2)
        // then
        assert(result2 == 0.0)

        // given
        val data3 = ",-0\r\n".toByteArray()
        // when
        val result3 = DoubleType.deserialize(data3)
        // then
        assert(result3 == 0.0)

        // given
        val data4 = ",42.13\r\n".toByteArray()
        // when
        val result4 = DoubleType.deserialize(data4)
        // then
        assert(result4 == 42.13)

        // given
        val data5 = ",-42.13\r\n".toByteArray()
        // when
        val result5 = DoubleType.deserialize(data5)
        // then
        assert(result5 == -42.13)

        // given
        val data6 = ",+42.13\r\n".toByteArray()
        // when
        val result6 = DoubleType.deserialize(data6)
        // then
        assert(result6 == 42.13)

        // given
        val data7 = ",42.13e2\r\n".toByteArray()
        // when
        val result7 = DoubleType.deserialize(data7)
        // then
        assert(result7 == 4213.0)

        // given
        val data8 = ",-42.13e2\r\n".toByteArray()
        // when
        val result8 = DoubleType.deserialize(data8)
        // then
        assert(result8 == -4213.0)

        // given
        val data9 = ",42.13e-2\r\n".toByteArray()
        // when
        val result9 = DoubleType.deserialize(data9)
        // then
        assert(result9 == 0.4213)

        // given
        val data10 = ",-42.13e-2\r\n".toByteArray()
        // when
        val result10 = DoubleType.deserialize(data10)
        // then
        assert(result10 == -0.4213)

        // given
        val data11 = ",42.13e+2\r\n".toByteArray()
        // when
        val result11 = DoubleType.deserialize(data11)
        // then
        assert(result11 == 4213.0)

        // given
        val data12 = ",42.13E2\r\n".toByteArray()
        // when
        val result12 = DoubleType.deserialize(data12)
        // then
        assert(result12 == 4213.0)

        // given
        val data13 = ",inf\r\n".toByteArray()
        // when
        val result13 = DoubleType.deserialize(data13)
        // then
        assert(result13 == Double.POSITIVE_INFINITY)

        // given
        val data14 = ",-inf\r\n".toByteArray()
        // when
        val result14 = DoubleType.deserialize(data14)
        // then
        assert(result14 == Double.NEGATIVE_INFINITY)

        // given
        val data15 = ",nan\r\n".toByteArray()
        // when
        val result15 = DoubleType.deserialize(data15)
        // then
        assert(result15.isNaN())

        // given
        val data16 = ",42e3\r\n".toByteArray()
        // when
        val result16 = DoubleType.deserialize(data16)
        // then
        assert(result16 == 42000.0)
    }

    @Test
    fun `test double serializer`() {
        // given
        val data = 42.0
        // when
        val result = DoubleType.serialize(data)
        // then
        assert(result.contentEquals(",42.0\r\n".toByteArray()))

        // given
        val data1 = -42.0
        // when
        val result1 = DoubleType.serialize(data1)
        // then
        assert(result1.contentEquals(",-42.0\r\n".toByteArray()))

        // given
        val data2 = 0.0
        // when
        val result2 = DoubleType.serialize(data2)
        // then
        assert(result2.contentEquals(",0.0\r\n".toByteArray()))

        // given
        val data3 = -0.0
        // when
        val result3 = DoubleType.serialize(data3)
        // then
        assert(result3.contentEquals(",-0.0\r\n".toByteArray()))

        // given
        val data4 = 42e1
        // when
        val result4 = DoubleType.serialize(data4)
        // then
        assert(result4.contentEquals(",420.0\r\n".toByteArray()))

        // given
        val data5 = 42.13e2
        // when
        val result5 = DoubleType.serialize(data5)
        // then
        assert(result5.contentEquals(",4213.0\r\n".toByteArray()))

        // given
        val data6 = 42.13e+2
        // when
        val result6 = DoubleType.serialize(data6)
        // then
        assert(result6.contentEquals(",4213.0\r\n".toByteArray()))

        // given
        val data7 = 42.13E-2
        // when
        val result7 = DoubleType.serialize(data7)
        // then
        assert(result7.contentEquals(",0.4213\r\n".toByteArray()))

        // given
        val data8 = Double.POSITIVE_INFINITY
        // when
        val result8 = DoubleType.serialize(data8)
        // then
        assert(result8.contentEquals(",inf\r\n".toByteArray()))

        // given
        val data9 = Double.NEGATIVE_INFINITY
        // when
        val result9 = DoubleType.serialize(data9)
        // then
        assert(result9.contentEquals(",-inf\r\n".toByteArray()))

        // given
        val data10 = Double.NaN
        // when
        val result10 = DoubleType.serialize(data10)
        // then
        assert(result10.contentEquals(",nan\r\n".toByteArray()))
    }
}
