package org.cangnova.cangjie.protodebugger.memory

import org.cangnova.cangjie.protodebugger.memory.Address.Companion.coercing
import kotlin.jvm.internal.Intrinsics

/**
 * 创建到指定地址（不包含）的范围
 *
 * 使用场景：
 * - 创建半开区间 [start, endExclusive)
 * - 内存区域的范围定义
 * - 地址范围的迭代和遍历
 *
 * @param endExclusive 范围的结束地址（不包含）
 * @return 创建的AddressRange对象
 */
infix fun Address.rangeToExclusive(endExclusive: Address): AddressRange {
    return rangeTo(
        endExclusive.minus(
            1
        )
    )
}

/**
 * 地址范围运算符重载
 *
 * 使用中缀运算符 .. 创建包含两端地址的范围 [start, endInclusive]
 *
 * @param endInclusive 范围的结束地址（包含）
 * @return 创建的AddressRange对象
 */
@JvmName(name = "addressRangeInclusive")
infix operator fun Address.rangeTo(endInclusive: Address): AddressRange =
    AddressRange(this, endInclusive)

/**
 * 获取范围的强制结束地址
 *
 * 返回范围的结束地址加1，用于半开区间的计算。
 *
 * @return 强制结束地址
 */
val AddressRange.endCoerced: Address
    get() {
        return this.endInclusive + 1.coercing
    }

/**
 * 地址范围类
 *
 * 该类表示一个内存地址范围，包含起始地址和结束地址。
 * 它实现了ClosedRange和Iterable接口，支持范围操作和迭代。
 *
 * 使用场景：
 * - 内存区域的表示和操作
 * - 地址范围的重叠检查
 * - 内存视图的分页和分段
 * - 断点和观察点的范围设置
 *
 * 主要功能：
 * - 地址范围的包含关系检查
 * - 范围大小的计算
 * - 范围的迭代和遍历
 * - 范围的重叠和交集操作
 *
 * @param start 范围的起始地址
 * @param endInclusive 范围的结束地址（包含）
 */
data class AddressRange(
    override val start: Address,
    override val endInclusive: Address
) : ClosedRange<Address>,
    Iterable<Address> {
    companion object {
        /**
         * 空地址范围
         * 表示一个空的地址范围，用于初始化或返回空值的情况
         */
        @field:JvmField
        val EMPTY: AddressRange = AddressRange(Address.MIN_VALUE.plus(1), Address.MIN_VALUE)

        /**
         * 完整地址范围
         * 表示所有可能的地址范围，从最小地址到最大地址
         */
        @field:JvmField
        val WHOLE: AddressRange = AddressRange(Address.MIN_VALUE, Address.MAX_VALUE)
    }

    /**
     * 地址范围的大小
     *
     * 计算范围包含的地址数量，包括起始和结束地址。
     *
     * @return 范围的大小（字节数）
     */
    val size: Long = endInclusive.minus(start) + 1L

    /**
     * 检查范围是否包含另一个地址范围
     *
     * 判断当前范围是否完全包含指定的另一个范围。
     * 要求两个范围都不能为空，且当前范围要包含指定范围的起始和结束地址。
     *
     * 使用场景：
     * - 内存区域的包含关系检查
     * - 范围重叠的验证
     * - 内存保护范围的检查
     *
     * @param range 要检查的地址范围
     * @return 如果当前范围完全包含指定范围返回true，否则返回false
     */
    operator fun contains(range: AddressRange): Boolean {
        return !range.isEmpty() && this.contains(range.start) && this.contains(range.endInclusive)
    }

    override fun equals(other: Any?): Boolean {
        return other is AddressRange && (this.isEmpty() && other.isEmpty() || Intrinsics.areEqual(
            this.start,
            other.start
        ) && Intrinsics.areEqual(
            this.endInclusive,
            other.endInclusive
        ))

    }

    override fun hashCode(): Int {
        return if (this.isEmpty()) -1 else 31 * this.start.hashCode() + this.endInclusive.hashCode()

    }

    fun headUntil(endExclusive: Address): AddressRange {


        return if (!this.isEmpty() && endExclusive > this.start)
            this.start.rangeTo(
                endExclusive.minus(1).coerceIn(this)
            ) else EMPTY
    }

    fun intersectWith(range: AddressRange): AddressRange {
        return if (!intersects(range)) EMPTY else
            this.start.coerceIn(range).rangeTo(endInclusive.coerceIn(range))

    }

    fun intersects(range: AddressRange): Boolean {
        return !range.isEmpty() && (this.contains(range.start)
                || this.contains(range.endInclusive))
                || !this.isEmpty() && (range.contains(
            this.start
        ) || range.contains(this.endInclusive))

    }

    override operator fun iterator(): Iterator<Address> {
        return generateSequence(start) { it ->
            if (it < this.endInclusive) {
                it + 1
            } else {
                null
            }
        }.iterator()
    }

    fun tailAfter(startExclusive: Address): AddressRange {
        return if (!this.isEmpty() && startExclusive < this.endInclusive)
            startExclusive.plus(1).coerceIn(this).rangeTo(endInclusive)
        else EMPTY

    }

    override fun toString(): String {
        return "$start..$endInclusive"

    }
}

