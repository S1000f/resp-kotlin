package core

import org.junit.jupiter.api.TestInstance
import respkotlin.core.*
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MapTypeTest {

    @Test
    fun `test map deserializer`() {
        // given
        val data1 = "%2\r\n+first\r\n:1\r\n+second\r\n:2\r\n".toByteArray()
        // when
        val result1 = MapType.deserialize(data1)
        // then
        assert(result1 == mapOf("first" to 1L, "second" to 2L))

        // given
        val data2 = "%2\r\n+first\r\n+value\r\n+second\r\n:2\r\n".toByteArray()
        // when
        val result2 = MapType.deserialize(data2)
        // then
        assert(result2 == mapOf("first" to "value", "second" to 2L))

        // given
        val data3 = "%2\r\n+first\r\n$3\r\nfoo\r\n+second\r\n:2\r\n".toByteArray()
        // when
        val result3 = MapType.deserialize(data3)
        // then
        assert(result3 == mapOf("first" to "foo", "second" to 2L))

        // given
        val data4 = "%2\r\n$7\r\nfoo\nbar\r\n:1\r\n+second\r\n:2\r\n".toByteArray()
        // when
        val result4 = MapType.deserialize(data4)
        // then
        assert(result4 == mapOf("foo\nbar" to 1L, "second" to 2L))

        // given
        val data5 = "%2\r\n+first\r\n:1\r\n+second\r\n*2\r\n$3\r\nfoo\r\n:2\r\n".toByteArray()
        // when
        val result5 = MapType.deserialize(data5)
        // then
        assert(result5 == mapOf("first" to 1L, "second" to listOf("foo", 2L)))

        // given
        val data6 = "%2\r\n+first\r\n*1\r\n:42\r\n+second\r\n*2\r\n+foo\r\n$3\r\nbar\r\n".toByteArray()
        // when
        val result6 = MapType.deserialize(data6)
        // then
        assert(result6 == mapOf("first" to listOf(42L), "second" to listOf("foo", "bar")))

        // given
        val data7 = "%2\r\n*1\r\n$3\r\nfoo\r\n$3\r\nbar\r\n+second\r\n:2\r\n".toByteArray()
        // when
        val result7 = MapType.deserialize(data7)
        // then
        assert(result7 == mapOf(listOf("foo") to "bar", "second" to 2L))

        // given
        val data8 = "%2\r\n%2\r\n+k\r\n+foo\r\n+second\r\n:2\r\n$3\r\nbar\r\n+k2\r\n:42\r\n".toByteArray()
        // when
        val result8 = MapType.deserialize(data8)
        // then
        assert(result8 == mapOf(mapOf("k" to "foo", "second" to 2L) to "bar", "k2" to 42L))

        // given
        val data = "%2\r\n%2\r\n+k\r\n:1\r\n$3\r\nbar\r\n+val\r\n:42\r\n%1\r\n+k2\r\n:2\r\n*2\r\n$3\r\nfoo\r\n:42\r\n".toByteArray()
        // when
        val result = MapType.deserialize(data)
        // then
        assert(result == mapOf(mapOf("k" to 1L, "bar" to "val") to 42L, mapOf("k2" to 2L) to listOf("foo", 42L)))
    }

    @Test
    fun `test map serializer`() {
        // given
        val data = mapOf("first" to 1L, "second" to 2L)
        // when
        val result = MapType.serialize(data)
        // then
        assert(result.contentEquals("%2\r\n+first\r\n:1\r\n+second\r\n:2\r\n".toByteArray()))

        // given
        val data1 = mapOf("first" to "hello\nworld!", "second" to 2L)
        // when
        val result1 = MapType.serialize(data1)
        // then
        assert(result1.contentEquals("%2\r\n+first\r\n$12\r\nhello\nworld!\r\n+second\r\n:2\r\n".toByteArray()))

        // given
        val data2 = emptyMap<Any, Any>()
        // when
        val result2 = MapType.serialize(data2)
        // then
        assert(result2.contentEquals("%0\r\n".toByteArray()))

        // given
        val data3 = mapOf("first" to listOf("foo", 2L), "second" to "bar")
        // when
        val result3 = MapType.serialize(data3)
        // then
        assert(result3.contentEquals("%2\r\n+first\r\n*2\r\n+foo\r\n:2\r\n+second\r\n+bar\r\n".toByteArray()))

        // given
        val data4 = mapOf(listOf("foo") to "bar", "second" to 2L)
        // when
        val result4 = MapType.serialize(data4)
        // then
        assert(result4.contentEquals("%2\r\n*1\r\n+foo\r\n+bar\r\n+second\r\n:2\r\n".toByteArray()))

        // given
        val data5 = mapOf(mapOf("k" to 1L, "bar" to "val") to 42L, "foo" to listOf("bar", 42L))
        // when
        val result5 = MapType.serialize(data5)
        // then
        assert(result5.contentEquals("%2\r\n%2\r\n+k\r\n:1\r\n+bar\r\n+val\r\n:42\r\n+foo\r\n*2\r\n+bar\r\n:42\r\n".toByteArray()))
    }
}