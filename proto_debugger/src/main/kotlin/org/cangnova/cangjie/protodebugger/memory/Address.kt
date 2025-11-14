/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.protodebugger.memory

import com.intellij.openapi.util.NlsSafe
import java.lang.Long.compareUnsigned
import java.lang.Long.toUnsignedString
import kotlin.jvm.internal.Intrinsics

/**
 * 内存地址类
 *
 * 该类表示一个内存地址，使用无符号长整数存储地址值。
 * 它提供了地址的比较、算术运算和格式化等功能，支持调试器中的内存操作。
 *
 * 使用场景：
 * - 调试器中内存地址的表示和操作
 * - 内存视图中的地址计算和偏移
 * - 断点和内存观察点的地址管理
 * - 内存范围和区域的边界定义
 *
 * 主要功能：
 * - 无符号地址值的存储和比较
 * - 地址的算术运算（加减法）
 * - 十六进制地址格式的字符串表示
 * - 地址解析和转换工具方法
 *
 * @param unsignedLongValue 地址的无符号长整数值
 */
data class Address(val unsignedLongValue: Long) :
    Comparable<Address> {
    companion object {
        /**
         * 最大地址值
         * 表示无符号长整数的最大值，即0xFFFFFFFFFFFFFFFF
         */
        @field:JvmField
        val MAX_VALUE: Address = fromUnsignedLong(-1)

        /**
         * 最小地址值
         * 表示无符号长整数的最小值，即0x0000000000000000
         */
        @field:JvmField
        val MIN_VALUE: Address = fromUnsignedLong(0)

        /**
         * 空地址
         * 通常用于表示空指针或无效地址，值为0
         */
        @field:JvmField
        val NULL: Address = MIN_VALUE

        /**
         * 数字转换为地址的扩展属性
         *
         * 使用场景：
         * - 将数值转换为内存地址
         * - 地址计算中的数值转换
         *
         * @return 对应的Address对象
         */
        val Number.asAddress: Address
            get() = fromUnsignedLong(this.toLong())

        /**
         * 数字转换为强制大小类型的扩展属性
         *
         * 用于地址运算中需要强制类型转换的场景
         *
         * @return CoercingSize对象
         */
        val Number.coercing: CoercingSize get() = CoercingSize(this.toLong())

        /**
         * 从无符号长整数创建地址
         *
         * @param l 无符号长整数值
         * @return 对应的Address对象
         */
        @JvmStatic
        fun fromUnsignedLong(l: Long): Address = Address(l)

        /**
         * 从十六进制字符串解析地址
         *
         * 支持带或不带"0x"前缀的十六进制字符串。
         * 自动处理格式验证和范围检查。
         *
         * 使用场景：
         * - 从用户输入解析地址
         * - 从调试器输出解析地址
         * - 配置文件中的地址值解析
         *
         * @param s 十六进制地址字符串，可包含"0x"前缀
         * @return 解析后的Address对象
         * @throws NumberFormatException 当字符串格式无效或超出范围时
         */
        @JvmStatic
        @Throws(NumberFormatException::class)
        fun parseHexString(s: String): Address {
            val hex = if (s.startsWith("0x", true)) s.substring(2) else s

            try {
                if (hex.isNotEmpty() && "+-".contains(hex[0])) {
                    throw NumberFormatException("Illegal leading sign: $s")
                } else {
                    val unsignedLong = java.lang.Long.parseUnsignedLong(hex, 16)
                    if (hex.length > 16) {
                        val trimmedHex = Regex("^0+").replaceFirst(hex, "")
                        if (trimmedHex.length > 16) {
                            throw NumberFormatException("String value $hex exceeds range of unsigned long.")
                        }
                    }
                    return fromUnsignedLong(unsignedLong)
                }
            } catch (e: NumberFormatException) {
                throw NumberFormatException("Invalid address: ${e.message}")
            }
        }

        /**
         * 强制大小类
         *
         * 用于地址运算中需要强制类型转换的场景，
         * 确保运算不会溢出地址范围。
         *
         * @param unsignedLongValue 无符号长整数值
         */
        data class CoercingSize(val unsignedLongValue: Long)
    }

    /**
     * 检查地址是否为空地址
     *
     * @return 如果地址等于NULL则返回true，否则返回false
     */
    val isNull: Boolean = Intrinsics.areEqual(this, NULL)

    /**
     * 地址比较
     *
     * 实现Comparable接口，使用无符号比较逻辑。
     *
     * @param other 要比较的另一个地址
     * @return 如果当前地址小于other返回负数，等于返回0，大于返回正数
     */
    override operator fun compareTo(other: Address): Int {
        return compareUnsigned(unsignedLongValue, other.unsignedLongValue)
    }

    /**
     * 地址减法运算符
     *
     * 计算两个地址之间的差值，返回字节数。
     *
     * 使用场景：
     * - 计算内存区域的大小
     * - 计算地址偏移量
     * - 分析内存布局
     *
     * @param other 要减去的地址
     * @return 地址差值（字节数）
     */
    infix operator fun minus(other: Address): Long {
        return unsignedLongValue - other.unsignedLongValue
    }

    /**
     * 地址减法运算符（强制大小类型）
     *
     * 使用强制类型转换进行安全的地址减法运算，
     * 防止结果溢出到无效范围。
     *
     * @param diffSize 要减去的大小
     * @return 计算后的地址，不会小于MIN_VALUE
     */
    infix operator fun minus(diffSize: CoercingSize): Address {
        return safeMinus(diffSize.unsignedLongValue)
    }

    /**
     * 地址减法运算符（数值类型）
     *
     * 将地址减去指定的数值偏移量。
     *
     * @param diff 要减去的数值
     * @return 计算后的地址
     */
    infix operator fun minus(diff: Number): Address {
        val it = diff.toLong()
        return if (it == 0L) this else Address(unsignedLongValue - it)
    }

    /**
     * 地址加法运算符（强制大小类型）
     *
     * 使用强制类型转换进行安全的地址加法运算，
     * 防止结果溢出到无效范围。
     *
     * @param diffSize 要增加的大小
     * @return 计算后的地址，不会大于MAX_VALUE
     */
    infix operator fun plus(diffSize: CoercingSize): Address {
        return safePlus(diffSize.unsignedLongValue)
    }

    /**
     * 地址加法运算符（数值类型）
     *
     * 将地址加上指定的数值偏移量。
     *
     * @param diff 要增加的数值
     * @return 计算后的地址
     */
    infix operator fun plus(diff: Number): Address {
        val it = diff.toLong()
        return if (it == 0L) this else Address(unsignedLongValue + it)
    }

    /**
     * 安全的地址减法运算
     *
     * 确保减法结果不会小于MIN_VALUE，
     * 如果会溢出则返回MIN_VALUE。
     *
     * @param unsignedValue 要减去的无符号值
     * @return 安全计算后的地址
     */
    private infix fun safeMinus(unsignedValue: Number): Address {
        return if (compareUnsigned(
                unsignedValue.toLong(),
                this.minus(MIN_VALUE)
            ) > 0
        ) MIN_VALUE else this.minus(unsignedValue)
    }

    /**
     * 安全的地址加法运算
     *
     * 确保加法结果不会大于MAX_VALUE，
     * 如果会溢出则返回MAX_VALUE。
     *
     * @param unsignedValue 要增加的无符号值
     * @return 安全计算后的地址
     */
    private infix fun safePlus(unsignedValue: Number): Address {
        return if (compareUnsigned(
                unsignedValue.toLong(),
                MAX_VALUE.minus(this)
            ) > 0
        ) MAX_VALUE else this.plus(unsignedValue)
    }

    /**
     * 返回地址的十六进制字符串表示
     *
     * 格式为"0x"前缀加16位十六进制数字，左侧补零。
     * 例如：0x000000007FFFFFFF
     *
     * @return 格式化的十六进制地址字符串
     */
    @NlsSafe
    override fun toString(): String {
        val hex = toUnsignedString(unsignedLongValue, 16)
        return "0x" + hex.padStart(16, '0')
    }


}

