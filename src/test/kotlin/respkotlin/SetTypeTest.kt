package respkotlin

import org.junit.jupiter.api.TestInstance
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetTypeTest {

    @Test
    fun `test set deserializer`() {
        // given
        val data = "~1\r\n$3\r\nfoo\r\n".toByteArray()
        // when
        val result = SetType.deserialize(data)
        // then
        assert(result == setOf("foo"))

        // given
        val data1 = "~2\r\n$3\r\nfoo\r\n$3\r\nfoo\r\n".toByteArray()
        // when
        val result1 = SetType.deserialize(data1)
        // then
        assert(result1 == setOf("foo"))

        // given
        val data2 = "~3\r\n$3\r\nfoo\r\n:42\r\n$3\r\nbar\r\n".toByteArray()
        // when
        val result2 = SetType.deserialize(data2)
        // then
        assert(result2 == setOf("foo", 42L, "bar"))
    }

    @Test
    fun `test set deserializer with nested data types`() {
        // given
        val data = "~2\r\n$3\r\nfoo\r\n~2\r\n$3\r\nbar\r\n:42\r\n".toByteArray()
        // when
        val result = SetType.deserialize(data)
        // then
        assert(result == setOf("foo", setOf("bar", 42L)))

        // given
        val data1 = "~2\r\n~1\r\n+foo\r\n*2\r\n+bar\r\n:42\r\n".toByteArray()
        // when
        val result1 = SetType.deserialize(data1)
        // then
        assert(result1 == setOf(setOf("foo"), listOf("bar", 42L)))

        // given
        val data2 = "~3\r\n%1\r\n+foo\r\n:42\r\n*1\r\n+bar\r\n~2\r\n$3\r\nbaz\r\n:3\r\n".toByteArray()
        // when
        val result2 = SetType.deserialize(data2)
        // then
        assert(result2 == setOf(mapOf("foo" to 42L), listOf("bar"), setOf("baz", 3L)))
    }
}