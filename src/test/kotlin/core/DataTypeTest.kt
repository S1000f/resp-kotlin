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
    fun `test simple string serializer`() {
        // given
        val data = "OK"
        // when
        val result = SimpleStringType.serialize(data)
        // then
        assert(result.contentEquals("+OK\r\n".toByteArray()))

        // given
        val data1 = "+"
        // when
        val result1 = SimpleStringType.serialize(data1)
        // then
        assert(result1.contentEquals("++\r\n".toByteArray()))
    }

    @Test
    fun `test simple error deserializer`() {
        // given
        val data = "-ERR unknown command 'foobar'\r\n".toByteArray()
        // when
        val result = SimpleErrorType.deserialize(data)
        // then
        assert(result == "ERR unknown command 'foobar'")

        // given
        val data1 = "-WRONGTYPE Operation against a key holding the wrong kind of value\r\n".toByteArray()
        // when
        val result1 = SimpleErrorType.deserialize(data1)
        // then
        assert(result1 == "WRONGTYPE Operation against a key holding the wrong kind of value")
    }

    @Test
    fun `test simple error serializer`() {
        // given
        val data = "ERR unknown command 'foobar'"
        // when
        val result = SimpleErrorType.serialize(data)
        // then
        assert(result.contentEquals("-ERR unknown command 'foobar'\r\n".toByteArray()))

        // given
        val data1 = "WRONGTYPE Operation against a key holding the wrong kind of value"
        // when
        val result1 = SimpleErrorType.serialize(data1)
        // then
        assert(result1.contentEquals("-WRONGTYPE Operation against a key holding the wrong kind of value\r\n".toByteArray()))
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

        // given
        val data2 = "$12\r\nHello\nworld!\r\n".toByteArray()
        // when
        val result2 = BulkStringType.deserialize(data2)
        // then
        assert(result2 == "Hello\nworld!")
    }

    @Test
    fun `test bulk string serializer`() {
        // given
        val data = "foo"
        // when
        val result = BulkStringType.serialize(data)
        // then
        assert(result.contentEquals("$3\r\nfoo\r\n".toByteArray()))

        // given
        val data1 = ""
        // when
        val result1 = BulkStringType.serialize(data1)
        // then
        assert(result1.contentEquals("$0\r\n\r\n".toByteArray()))

        // given
        val data2 = "Hello\nworld!"
        // when
        val result2 = BulkStringType.serialize(data2)
        // then
        assert(result2.contentEquals("$12\r\nHello\nworld!\r\n".toByteArray()))
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