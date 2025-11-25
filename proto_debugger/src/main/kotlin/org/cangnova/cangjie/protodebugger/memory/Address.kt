/*
 * Copyright 2025 LinQingYing. and contributors.
 * Licensed under the Apache License, Version 2.0
 */

package org.cangnova.cangjie.protodebugger.memory

import com.intellij.openapi.util.NlsSafe

/**
 * 内存地址的不可变表示
 *
 * 封装64位无符号地址值，提供类型安全的地址操作。
 * 采用值类型语义，支持地址运算和格式化输出。
 */
@JvmInline
value class Address(val value: ULong) : Comparable<Address> {

    companion object {
        val MAX_VALUE = Address(ULong.MAX_VALUE)
        val MIN_VALUE = Address(ULong.MIN_VALUE)
        val NULL = MIN_VALUE

        /**
         * 地址解析器
         *
         * 提供多种字符串格式的地址解析能力。
         * 使用组合模式将解析逻辑独立出来。
         */

        object Parser {
            private val HEX_PREFIX = Regex("^0[xX]")

            /**
             * 从十六进制字符串解析地址
             *
             * 支持格式:
             * - "0x1234" (带前缀)
             * - "1234" (无前缀)
             * - "0X1234" (大写前缀)
             *
             * @param hexString 十六进制字符串
             * @return 解析的地址
             * @throws NumberFormatException 如果格式无效
             */

            fun hex(hexString: String): Address {
                val normalized = hexString.replace(HEX_PREFIX, "")

                require(normalized.isNotEmpty()) { "Empty hex string" }
                require(normalized.first() !in "+-") { "Address cannot have sign: $hexString" }

                return try {
                    Address(normalized.toULong(16))
                } catch (e: NumberFormatException) {
                    throw NumberFormatException("Invalid hex address: $hexString")
                }
            }

            /**
             * 安全的十六进制解析
             *
             * @return 解析成功返回地址，失败返回 null
             */

            fun hexOrNull(hexString: String): Address? = runCatching { hex(hexString) }.getOrNull()

            /**
             * 从十进制字符串解析地址
             *
             * @param decString 十进制字符串
             * @return 解析的地址
             * @throws NumberFormatException 如果格式无效
             */

            fun decimal(decString: String): Address {
                return try {
                    Address(decString.toULong(10))
                } catch (e: NumberFormatException) {
                    throw NumberFormatException("Invalid decimal address: $decString")
                }
            }

            /**
             * 从二进制字符串解析地址
             *
             * @param binString 二进制字符串 (如 "0b101010")
             * @return 解析的地址
             */

            fun binary(binString: String): Address {
                val normalized = binString.removePrefix("0b").removePrefix("0B")
                return try {
                    Address(normalized.toULong(2))
                } catch (e: NumberFormatException) {
                    throw NumberFormatException("Invalid binary address: $binString")
                }
            }
        }

        /**
         * 地址工厂
         *
         * 提供从不同数值类型创建地址的便捷方法。
         * 使用工厂模式替代多个重载的构造函数。
         */

        object Factory {
            /**
             * 从无符号长整型创建地址
             */

            fun fromULong(value: ULong): Address = Address(value)

            /**
             * 从有符号长整型创建地址
             *
             * 将有符号值视为无符号值处理
             */

            fun fromLong(value: Long): Address = Address(value.toULong())

            /**
             * 从整型创建地址
             */

            fun fromInt(value: Int): Address = Address(value.toULong())

            /**
             * 从任意数值类型创建地址
             */

            fun fromNumber(value: Number): Address = Address(value.toLong().toULong())

            /**
             * 使用 invoke 操作符支持函数式调用
             *
             * 示例: Address(0x1234UL)
             */

            operator fun invoke(value: ULong): Address = Address(value)
        }

        /**
         * Number 扩展属性 - 便捷的地址创建方式
         *
         * 示例: val addr = 0x1234.addr
         */
        val Number.addr: Address get() = Factory.fromNumber(this)
    }

    // ===== 属性 =====

    val asLong: Long get() = value.toLong()
    val isNull: Boolean get() = value == 0UL
    val isValid: Boolean get() = value != 0UL

    // ===== 比较 =====

    override fun compareTo(other: Address): Int = value.compareTo(other.value)

    // ===== 算术运算 =====

    operator fun minus(other: Address): Long = (value - other.value).toLong()
    operator fun plus(offset: Long): Address = if (offset == 0L) this else Address(value + offset.toULong())
    operator fun minus(offset: Long): Address = if (offset == 0L) this else Address(value - offset.toULong())
    operator fun plus(offset: Int): Address = plus(offset.toLong())
    operator fun minus(offset: Int): Address = minus(offset.toLong())

    /** 饱和加法：溢出时返回 MAX_VALUE */
    fun saturatingAdd(offset: ULong): Address {
        val result = value + offset
        return if (result < value) MAX_VALUE else Address(result)
    }

    /** 饱和减法：下溢时返回 MIN_VALUE */
    fun saturatingSub(offset: ULong): Address {
        val result = value - offset
        return if (result > value) MIN_VALUE else Address(result)
    }

    // ===== 对齐操作 =====

    /**
     * 向下对齐到指定边界
     * @param alignment 对齐边界（必须是2的幂）
     */
    fun alignDown(alignment: ULong): Address {
        requirePowerOfTwo(alignment)
        val mask = alignment - 1UL
        return Address(value and mask.inv())
    }

    /**
     * 向上对齐到指定边界
     * @param alignment 对齐边界（必须是2的幂）
     */
    fun alignUp(alignment: ULong): Address {
        requirePowerOfTwo(alignment)
        val mask = alignment - 1UL
        return Address((value + mask) and mask.inv())
    }

    /** 检查是否按指定边界对齐 */
    fun isAlignedTo(alignment: ULong): Boolean = value and (alignment - 1UL) == 0UL

    private fun requirePowerOfTwo(value: ULong) {
        require(value > 0UL && (value and (value - 1UL)) == 0UL) {
            "Alignment must be a power of 2, got: $value"
        }
    }

    // ===== 范围操作 =====

    /** 创建包含结束地址的闭区间 */
    operator fun rangeTo(end: Address): AddressRange = AddressRange.of(this, end)

    /** 创建不包含结束地址的半开区间 */
    infix fun until(end: Address): AddressRange = AddressRange.of(this, end - 1)

    // ===== 格式化 =====

    /** 完整格式: "0x00000000DEADBEEF" */
    @NlsSafe
    override fun toString(): String = "0x${value.toString(16).padStart(16, '0').uppercase()}"

    /** 紧凑格式: "0xDEADBEEF" */
    fun toCompactString(): String = "0x${value.toString(16).uppercase()}"

    /** 指定宽度格式 */
    fun toString(minWidth: Int): String = "0x${value.toString(16).padStart(minWidth, '0').uppercase()}"
}
