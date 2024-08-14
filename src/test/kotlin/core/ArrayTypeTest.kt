package core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import respkotlin.core.ArrayType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArrayTypeTest {

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

        // given
        val data3 = "*2\r\n*2\r\n+foo\r\n*2\r\n+bar\r\n:42\r\n$11\r\nhello\nworld\r\n".toByteArray()
        // when
        val result3 = ArrayType.deserialize(data3)
        // then
        assert(result3 == listOf(listOf("foo", listOf("bar", 42L)), "hello\nworld"))
    }

    @Test
    fun `test array serialize`() {
        // given
        val list = listOf("foo", "bar")
        // when
        val matched2 = ArrayType.serialize(list)
        // then
        assert(matched2.contentEquals("*2\r\n+foo\r\n+bar\r\n".toByteArray()))

        // given
        val list1 = listOf("fo\no", 1000L)
        // when
        val matched3 = ArrayType.serialize(list1)
        // then
        assert(matched3.contentEquals("*2\r\n$4\r\nfo\no\r\n:1000\r\n".toByteArray()))

        // given
        val list2 = listOf("foo", listOf("bar", 1000L))
        // when
        val matched4 = ArrayType.serialize(list2)
        // then
        assert(matched4.contentEquals("*2\r\n+foo\r\n*2\r\n+bar\r\n:1000\r\n".toByteArray()))

        // given
        val list3 = listOf("hello\nworld", listOf("bar", 1000L), "baz")
        // when
        val matched5 = ArrayType.serialize(list3)
        // then
        assert(matched5.contentEquals("*3\r\n$11\r\nhello\nworld\r\n*2\r\n+bar\r\n:1000\r\n+baz\r\n".toByteArray()))

        // given
        val list4 = listOf(listOf("foo", 42L), listOf("bar", 1000L))
        // when
        val matched6 = ArrayType.serialize(list4)
        // then
        assert(matched6.contentEquals("*2\r\n*2\r\n+foo\r\n:42\r\n*2\r\n+bar\r\n:1000\r\n".toByteArray()))

        // given
        val list5 = listOf(listOf("foo", listOf("bar", 42L)), "hello\nworld")
        // when
        val matched7 = ArrayType.serialize(list5)
        // then
        assert(matched7.contentEquals("*2\r\n*2\r\n+foo\r\n*2\r\n+bar\r\n:42\r\n$11\r\nhello\nworld\r\n".toByteArray()))
    }
}
