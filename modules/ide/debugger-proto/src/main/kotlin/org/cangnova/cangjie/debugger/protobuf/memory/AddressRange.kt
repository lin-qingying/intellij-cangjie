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

package org.cangnova.cangjie.debugger.protobuf.memory

/**
 * 内存地址范围的不可变表示
 *
 * 表示连续内存区间 [start, endInclusive]。
 * 实现 ClosedRange 和 Iterable 接口，支持范围操作和遍历。
 *
 * 注意：不使用 data class，因为需要自定义 equals/hashCode 处理空范围
 */
class AddressRange private constructor(
    override val start: Address,
    override val endInclusive: Address,
    @Suppress("UNUSED_PARAMETER") marker: Unit  // 私有构造器标记
) : ClosedRange<Address>, Iterable<Address> {

    companion object {
        /** 空范围单例 */
        @JvmStatic
        val EMPTY: AddressRange = AddressRange(Address(1UL), Address.NULL, Unit)

        /** 完整地址空间 */
        @JvmStatic
        val FULL: AddressRange = AddressRange(Address.MIN_VALUE, Address.MAX_VALUE, Unit)

        /** 主构造工厂方法 */
        @JvmStatic
        fun of(start: Address, endInclusive: Address): AddressRange {
            if (start > endInclusive) return EMPTY
            return AddressRange(start, endInclusive, Unit)
        }

        /** 从起始地址和大小创建 */
        @JvmStatic
        fun ofSize(start: Address, size: ULong): AddressRange {
            if (size == 0UL) return EMPTY
            val end = start.saturatingAdd(size - 1UL)
            return AddressRange(start, end, Unit)
        }

        @JvmStatic
        fun ofSize(start: Address, size: Long): AddressRange {
            require(size >= 0) { "Size cannot be negative: $size" }
            return ofSize(start, size.toULong())
        }

        /** 从两个地址创建（自动排序） */
        @JvmStatic
        fun between(a: Address, b: Address): AddressRange =
            if (a <= b) of(a, b) else of(b, a)
    }

    // ===== 基本属性 =====

    /** 范围大小（使用 ULong 避免溢出） */
    val size: ULong by lazy {
        if (isEmpty()) 0UL else (endInclusive.value - start.value + 1UL)
    }

    /** 范围大小（Long 版本，超出范围返回 -1） */
    val sizeOrNegative: Long
        get() = if (size > Long.MAX_VALUE.toULong()) -1L else size.toLong()

    /** 结束地址（不包含），用于半开区间 */
    val endExclusive: Address
        get() = endInclusive.saturatingAdd(1UL)

    /** 范围中点 */
    val midpoint: Address
        get() = if (isEmpty()) Address.NULL else start + (size / 2UL).toLong()

    override fun isEmpty(): Boolean = start > endInclusive

    // ===== 包含检查 =====

    override fun contains(value: Address): Boolean =
        !isEmpty() && value >= start && value <= endInclusive

    operator fun contains(other: AddressRange): Boolean =
        other.isEmpty() || (!isEmpty() && other.start >= start && other.endInclusive <= endInclusive)

    // ===== 集合运算 =====

    /** 检查是否有交集 */
    fun overlaps(other: AddressRange): Boolean =
        !isEmpty() && !other.isEmpty() && start <= other.endInclusive && other.start <= endInclusive

    /** 检查是否相邻（可合并但不重叠） */
    fun adjacentTo(other: AddressRange): Boolean {
        if (isEmpty() || other.isEmpty()) return false
        return endExclusive == other.start || other.endExclusive == start
    }

    /** 交集 */
    infix fun intersect(other: AddressRange): AddressRange {
        if (!overlaps(other)) return EMPTY
        return of(maxOf(start, other.start), minOf(endInclusive, other.endInclusive))
    }

    /** 并集（仅当可合并时返回非空） */
    infix fun union(other: AddressRange): AddressRange? {
        if (isEmpty()) return other
        if (other.isEmpty()) return this
        if (!overlaps(other) && !adjacentTo(other)) return null
        return of(minOf(start, other.start), maxOf(endInclusive, other.endInclusive))
    }

    /** 差集：返回 this 中不在 other 中的部分 */
    infix fun subtract(other: AddressRange): List<AddressRange> {
        if (isEmpty() || !overlaps(other)) return listOf(this)
        if (other.contains(this)) return emptyList()

        return buildList {
            if (start < other.start) add(of(start, other.start - 1))
            if (endInclusive > other.endInclusive) add(of(other.endInclusive + 1, endInclusive))
        }
    }

    // ===== 分割与变换 =====

    /** 获取前缀（边界之前的部分） */
    fun before(boundary: Address): AddressRange {
        if (isEmpty() || boundary <= start) return EMPTY
        return of(start, minOf(boundary - 1, endInclusive))
    }

    /** 获取后缀（边界及之后的部分） */
    fun from(boundary: Address): AddressRange {
        if (isEmpty() || boundary > endInclusive) return EMPTY
        return of(maxOf(boundary, start), endInclusive)
    }

    /** 分块 */
    fun chunked(chunkSize: ULong): Sequence<AddressRange> {
        require(chunkSize > 0UL) { "Chunk size must be positive" }
        if (isEmpty()) return emptySequence()

        return sequence {
            var cur = start
            while (cur <= endInclusive) {
                val chunkEnd = minOf(cur.saturatingAdd(chunkSize - 1UL), endInclusive)
                yield(of(cur, chunkEnd))
                if (chunkEnd == Address.MAX_VALUE) break
                cur = chunkEnd + 1
            }
        }
    }

    fun chunked(chunkSize: Long): Sequence<AddressRange> = chunked(chunkSize.toULong())

    /** 扩展以包含指定地址 */
    fun expandTo(address: Address): AddressRange {
        if (isEmpty()) return of(address, address)
        if (address in this) return this
        return of(minOf(start, address), maxOf(endInclusive, address))
    }

    /** 平移 */
    fun shift(offset: Long): AddressRange {
        if (isEmpty()) return this
        return of(start + offset, endInclusive + offset)
    }

    // ===== 迭代器 =====

    override fun iterator(): Iterator<Address> = object : Iterator<Address> {
        private var cur = start
        private var done = isEmpty()

        override fun hasNext() = !done

        override fun next(): Address {
            if (done) throw NoSuchElementException()
            return cur.also {
                done = (cur == endInclusive)
                if (!done) cur += 1
            }
        }
    }

    // ===== equals / hashCode / toString =====

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddressRange) return false
        if (isEmpty() && other.isEmpty()) return true
        return start == other.start && endInclusive == other.endInclusive
    }

    override fun hashCode(): Int =
        if (isEmpty()) 0 else 31 * start.hashCode() + endInclusive.hashCode()

    override fun toString(): String =
        if (isEmpty()) "AddressRange.EMPTY" else "[${start.toCompactString()}..${endInclusive.toCompactString()}]"

    fun toDetailedString(): String =
        if (isEmpty()) "AddressRange.EMPTY" else "$this (size: $size bytes)"

    // ===== 解构 =====

    operator fun component1(): Address = start
    operator fun component2(): Address = endInclusive
}

// ===== Address 扩展（如果未在 Address.kt 中定义） =====

/** 创建闭区间 [this, end] */
operator fun Address.rangeTo(end: Address): AddressRange = AddressRange.of(this, end)

/** 创建半开区间 [this, end) */
infix fun Address.until(end: Address): AddressRange = AddressRange.of(this, end - 1)

/** 限制地址在范围内 */
fun Address.coerceIn(range: AddressRange): Address = when {
    range.isEmpty() -> this
    this < range.start -> range.start
    this > range.endInclusive -> range.endInclusive
    else -> this
}

/** 要求地址在范围内 */
fun Address.requireIn(range: AddressRange, lazyMsg: () -> String = { "$this not in $range" }): Address {
    require(this in range, lazyMsg)
    return this
}

/** 要求范围被包含 */
fun AddressRange.requireIn(
    container: AddressRange,
    lazyMsg: () -> String = { "$this not in $container" }
): AddressRange {
    require(this in container, lazyMsg)
    return this
}