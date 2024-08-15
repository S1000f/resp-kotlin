package respkotlin

import org.junit.jupiter.api.TestInstance
import respkotlin.core.*
import respkotlin.core.toDataType
import java.time.LocalDateTime
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ToolTest {

    @Test
    fun test() {
        val now = LocalDateTime.now()
        val serialize = LocalDateTimeType.serialize(now)
        println(String(serialize))

        val deserialize = LocalDateTimeType.deserialize(serialize)
        println(deserialize)
    }

    @Test
    fun test1() {
        registerDataType(LocalDateTime::class, LocalDateTimeType)

        val now = LocalDateTime.now()
        val serialize = LocalDateTimeType.serialize(now)

        when (val dataType = serialize.toDataType()) {
            is LocalDateTimeType -> {
                val deserialize = dataType.deserialize(serialize)
                println(deserialize)
            }

            else -> println("error")
        }
    }

    @Test
    fun test2() {
        registerDataType(LocalDateTime::class, LocalDateTimeType)

        val now = LocalDateTime.now()
        val list = listOf(now, 42L)
        val serialize = ArrayType.serialize(list)

        println(String(serialize))
    }
}

object LocalDateTimeType : DataType<LocalDateTime, LocalDateTime> {
    override fun serialize(data: LocalDateTime): ByteArray {
        return "$firstByte$data$TERMINATOR".toByteArray()
    }

    override fun deserialize(data: ByteArray): LocalDateTime {
        return LocalDateTime.parse(String(data, 1, data.size - 3))
    }

    override val firstByte: Char
        get() = 't'

    override val length: (ByteArray) -> Int
        get() = { it.size - TERMINATOR.length }
}