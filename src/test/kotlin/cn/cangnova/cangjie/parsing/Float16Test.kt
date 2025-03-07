package cn.cangnova.cangjie.parsing

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Float16Test {
    val value = 11111.1f
    val float16 = Float16.fromFloat(value)

    fun init(){

    }

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
    }

    @Test
    fun toShort() {
        println(float16.toShort())
    }

    @Test
    fun isInfinite() {
    }

    @Test
    fun toFloat() {
    }

    @Test
    fun toDouble() {
    }

    @Test
    fun toInt() {
    }

    @Test
    fun toLong() {
    }

    @Test
    fun toByte() {
    }

    @Test
    fun compareTo() {
    }

    @Test
    fun testEquals() {
    }

    @Test
    fun testHashCode() {
    }

    @Test
    fun testToString() {
    }

    @Test
    fun copy() {
    }
}
