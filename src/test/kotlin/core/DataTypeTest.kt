package core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import respkotlin.core.*
import respkotlin.core.ArrayType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataTypeTest {

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
    }

    @Test
    fun `test array deserializer`() {
        // given
        val data = "*1\r\n$3\r\nfoo\r\n".toByteArray()
        // when
        val result = ArrayType.deserialize(data)
        // then
        assert(result == listOf("foo"))

        // given
        val data1 = "*2\r\n$3\r\nfoo\r\n$2\r\nba\r\n".toByteArray()
        // when
        val result1 = ArrayType.deserialize(data1)
        // then
        assert(result1 == listOf("foo", "ba"))

        // given
        val data2 = "*2\r\n$1\r\nf\r\n$3\r\nbar\r\n".toByteArray()
        // when
        val result2 = ArrayType.deserialize(data2)
        // then
        assert(result2 == listOf("f", "bar"))

        // given
        val data3 = "*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n".toByteArray()
        // when
        val result3 = ArrayType.deserialize(data3)
        // then
        assert(result3 == listOf("foo", "bar"))

        // given
        val data4 = "*3\r\n:42\r\n:-42\r\n:+43\r\n".toByteArray()
        // when
        val result4 = ArrayType.deserialize(data4)
        // then
        assert(result4 == listOf(42L, -42L, 43L))
    }

    @Test
    fun `test array deserializer with mixed data types`() {
        // given
        val data = "*3\r\n$3\r\nfoo\r\n:1000\r\n$3\r\nbar\r\n".toByteArray()
        // when
        val result = ArrayType.deserialize(data)
        // then
        assert(result == listOf("foo", 1000L, "bar"))

        // given
        val data1 = "*3\r\n:+42\r\n$3\r\nbar\r\n:1000\r\n".toByteArray()
        // when
        val result1 = ArrayType.deserialize(data1)
        // then
        assert(result1 == listOf(42L, "bar", 1000L))
    }

    @Test
    fun `test nested array deserialize`() {
        // given
        val data = "*2\r\n*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$3\r\nbaz\r\n".toByteArray()
        // when
        val result = ArrayType.deserialize(data)
        // then
        assert(result == listOf(listOf("foo", "bar"), "baz"))

        // given
        val data1 = "*2\r\n$3\r\nfoo\r\n*2\r\n$3\r\nbar\r\n$2\r\nbz\r\n".toByteArray()
        // when
        val result1 = ArrayType.deserialize(data1)
        // then
        assert(result1 == listOf("foo", listOf("bar", "bz")))

        // given
        val data2 = "*2\r\n*2\r\n$3\r\nfoo\r\n$2\r\nba\r\n*2\r\n$1\r\nz\r\n$3\r\nqux\r\n".toByteArray()
        // when
        val result2 = ArrayType.deserialize(data2)
        // then
        assert(result2 == listOf(listOf("foo", "ba"), listOf("z", "qux")))
    }

    @Test
    fun `test nested array deserialize with mixed data types`() {
        // given
        val data = "*2\r\n*2\r\n$3\r\nfoo\r\n:1000\r\n$3\r\nbar\r\n".toByteArray()
        // when
        val result = ArrayType.deserialize(data)
        // then
        assert(result == listOf(listOf("foo", 1000L), "bar"))

        // given
        val data1 = "*2\r\n:42\r\n*2\r\n$3\r\nbar\r\n:1000\r\n".toByteArray()
        // when
        val result1 = ArrayType.deserialize(data1)
        // then
        assert(result1 == listOf(42L, listOf("bar", 1000L)))

        // given
        val data2 = "*2\r\n*2\r\n$3\r\nfoo\r\n:1000\r\n*3\r\n$0\r\n\r\n:42\r\n+OK\r\n".toByteArray()
        // when
        val result2 = ArrayType.deserialize(data2)
        // then
        assert(result2 == listOf(listOf("foo", 1000L), listOf("", 42L, "OK")))
    }

}