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

package org.cangnova.cangjie.metadata.model

/**
 * 高性能属性包类
 * 
 * 采用位图技术高效存储和管理AST节点的属性集合，支持超过500个不同的属性类型。
 * 
 * 核心设计：
 * - 使用ULong数组存储位图，每个ULong管理64个属性
 * - 通过位运算快速计算属性在数组中的位置
 * - 支持批量操作和高效的属性测试
 * - 内存占用最小化，性能最大化
 * 
 * 性能特点：
 * - O(1) 属性设置和测试时间复杂度
 * - 内存紧凑存储，每个属性仅占用1位
 * - 支持快速的批量属性操作
 */
@OptIn(ExperimentalUnsignedTypes::class)
class AttributePack {
    companion object {
        /** 每个ULong管理的位数 */
        const val BITS_PER_LONG = 64
        
        /** 预计算的ULong数组大小，避免运行时计算 */
        val arraySize = (Attribute.AST_ATTR_END.ordinal + BITS_PER_LONG - 1) / BITS_PER_LONG
        
        /** 位掩码缓存：用于快速位运算 */
        @OptIn(ExperimentalUnsignedTypes::class)
        val BIT_MASKS = ULongArray(BITS_PER_LONG) { 1UL shl it }
    }
    
    /** 高性能位图数组：使用ULong原生类型存储所有属性状态 */
    private val bits: ULongArray
    
    /**
     * 默认构造函数 - 零初始化
     * 
     * 创建足够容纳所有属性的ULong数组，所有属性默认为false。
     * 时间复杂度：O(n) where n = arraySize
     */
    constructor() {
        bits = ULongArray(arraySize)
    }
    
    /**
     * ULong数组构造函数 - 直接初始化
     * 
     * 从现有ULong数组创建AttributePack，执行拷贝以避免共享状态。
     * 自动填充缺失的位以确保数组完整性。
     * 
     * @param ulongs 源ULong数组，每个元素编码64个连续属性的状态
     * 时间复杂度：O(n) where n = max(ulongs.size, arraySize)
     */
    constructor(ulongs: ULongArray) {
        bits = ULongArray(arraySize) { index ->
            if (index < ulongs.size) ulongs[index] else 0UL
        }
    }
    
    /**
     * ULong列表构造函数 - 集合兼容性
     * 
     * 从ULong列表创建AttributePack，提供与集合类型的兼容性。
     * 
     * @param ulongs ULong列表，每个元素包含64位属性信息
     * 时间复杂度：O(n) where n = ulongs.size
     */
    constructor(ulongs: List<ULong>) : this(ulongs.toULongArray())
    
    /**
     * 属性设置方法 - 位运算优化
     * 
     * 设置单个属性的状态，使用位运算实现最高性能。
     * 
     * @param attr 目标属性枚举值
     * @param enable 属性状态：true=启用, false=禁用（默认true）
     * 时间复杂度：O(1)
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun setAttr(attr: Attribute, enable: Boolean = true) {
        val ordinal = attr.ordinal
        val arrayIndex = ordinal / BITS_PER_LONG
        val bitIndex = ordinal % BITS_PER_LONG
        
        if (enable) {
            bits[arrayIndex] = bits[arrayIndex] or BIT_MASKS[bitIndex]
        } else {
            bits[arrayIndex] = bits[arrayIndex] and BIT_MASKS[bitIndex].inv()
        }
    }
    
    /**
     * 批量属性设置方法 - 可变参数优化
     * 
     * 一次性设置多个属性为启用状态，减少多次方法调用的开销。
     * 
     * @param attrs 要启用的属性列表
     * 时间复杂度：O(k) where k = attrs.size
     * 
     * 使用示例：
     * ```
     * pack.setAttrs(Attribute.PUBLIC, Attribute.STATIC, Attribute.FINAL)
     * ```
     */
    fun setAttrs(vararg attrs: Attribute) {
        for (attr in attrs) {
            setAttr(attr, true)
        }
    }
    
    /**
     * 属性测试方法 - 核心性能方法
     * 
     * 测试指定属性是否为启用状态，这是最频繁调用的方法。
     * 使用位运算和位掩码实现最佳性能。
     * 
     * @param attr 要测试的属性
     * @return true如果属性已启用，false否则
     * 时间复杂度：O(1)
     */
    fun testAttr(attr: Attribute): Boolean {
        val ordinal = attr.ordinal
        val arrayIndex = ordinal / BITS_PER_LONG
        val bitIndex = ordinal % BITS_PER_LONG
        
        return (bits[arrayIndex] and BIT_MASKS[bitIndex]) != 0UL
    }
    
    /**
     * 多属性逻辑与测试 - 短路优化
     * 
     * 测试多个属性是否全部为启用状态，使用短路逻辑优化性能。
     * 一旦发现任何属性为false，立即返回false。
     * 
     * @param attr 第一个必须测试的属性
     * @param otherAttrs 其他要测试的属性（可变参数）
     * @return 当且仅当所有属性都为true时返回true
     * 时间复杂度：O(k) where k = 1 + otherAttrs.size，但有短路优化
     */
    fun testAttr(attr: Attribute, vararg otherAttrs: Attribute): Boolean {
        // 短路优化：第一个属性为false时立即返回
        if (!testAttr(attr)) return false
        
        // 优化：使用传统循环避免lambda开销
        for (otherAttr in otherAttrs) {
            if (!testAttr(otherAttr)) return false
        }
        return true
    }
    
    /**
     * ULong数组导出方法 - 原生数据访问
     * 
     * 获取内部位图数组的拷贝，防止外部修改影响内部状态。
     * 提供最紧凑的数据表示形式。
     * 
     * @return 位图数据的独立拷贝
     * 时间复杂度：O(n) where n = arraySize
     * 空间复杂度：O(n)
     */
    fun toULongArray(): ULongArray {
        return bits.copyOf()
    }
    
    /**
     * ULong列表转换方法 - 集合兼容性
     * 
     * 将内部位图转换为ULong列表，提供与集合API的兼容性。
     * 每个ULong编码64个连续属性的状态。
     * 
     * @return ULong列表，每个元素包含64位属性状态
     * 时间复杂度：O(n) where n = arraySize
     */
    fun toULongList(): List<ULong> {
        return bits.toList()
    }
    
    /**
     * 全局清除方法 - 批量重置
     * 
     * 将所有属性重置为false状态，保持数组结构不变。
     * 比重新创建对象更高效。
     * 
     * 时间复杂度：O(n) where n = arraySize
     */
    fun clearAll() {
        bits.fill(0UL)
    }
    
    /**
     * 深拷贝方法 - 状态同步
     * 
     * 将另一个AttributePack的所有状态复制到当前实例。
     * 使用数组拷贝实现高效的批量复制。
     * 
     * @param other 源AttributePack实例
     * 时间复杂度：O(n) where n = min(this.size, other.size)
     * 
     * 注意：此操作会完全覆盖当前所有属性状态
     */
    fun copyFrom(other: AttributePack) {
        for (i in bits.indices) {
            bits[i] = if (i < other.bits.size) other.bits[i] else 0UL
        }
    }
    
    /**
     * 位或操作 - 属性合并
     * 
     * 将另一个AttributePack的属性状态合并到当前实例。
     * 使用位或运算，保持已有的true状态。
     * 
     * @param other 要合并的AttributePack实例
     * 时间复杂度：O(n) where n = min(this.size, other.size)
     */
    fun orWith(other: AttributePack) {
        for (i in 0 until minOf(bits.size, other.bits.size)) {
            bits[i] = bits[i] or other.bits[i]
        }
    }
    
    /**
     * 位与操作 - 属性交集
     * 
     * 保留与另一个AttributePack共同的属性状态。
     * 使用位与运算，只保留两者都为true的状态。
     * 
     * @param other 要求交集的AttributePack实例
     * 时间复杂度：O(n) where n = arraySize
     */
    fun andWith(other: AttributePack) {
        for (i in bits.indices) {
            bits[i] = bits[i] and (if (i < other.bits.size) other.bits[i] else 0UL)
        }
    }
    
    /**
     * 非空检测方法 - 快速判断
     * 
     * 检查是否至少有一个属性被设置为true。
     * 使用短路逻辑，一旦找到非零值立即返回。
     * 
     * @return true如果存在任何启用的属性
     * 时间复杂度：O(n) 最坏情况，但通常O(1)由于短路优化
     */
    fun hasAnyAttr(): Boolean {
        return bits.any { it != 0UL }
    }
    
    /**
     * 属性计数方法 - 统计启用状态
     * 
     * 计算当前启用的属性总数。
     * 使用位计数算法实现高效统计。
     * 
     * @return 启用属性的数量
     * 时间复杂度：O(n*log(bits_per_long)) where n = arraySize
     */
    fun countSetAttributes(): Int {
        return bits.sumOf { it.countOneBits() }
    }
    
    /**
     * 活跃属性枚举方法 - 状态查询
     * 
     * 返回所有当前为true状态的属性列表，排除结束标记。
     * 适用于调试、日志记录和状态分析。
     * 
     * @return 所有启用属性的列表
     * 时间复杂度：O(k) where k = Attribute.values().size
     * 
     * 性能提示：此方法会遍历所有可能的属性，仅在需要完整状态时使用
     */
    fun getSetAttributes(): List<Attribute> {
        return Attribute.values()
            .filter { it != Attribute.AST_ATTR_END && testAttr(it) }
    }
    
    /**
     * 相等性比较 - 状态对比
     * 
     * 比较两个AttributePack实例是否具有相同的属性状态。
     * 使用数组内容比较实现精确匹配。
     * 
     * @param other 要比较的对象
     * @return true如果属性状态完全相同
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AttributePack) return false
        return bits.contentEquals(other.bits)
    }
    
    /**
     * 哈希码计算 - 一致性保证
     * 
     * 基于内部位图数组计算哈希码，确保相等对象具有相同哈希码。
     * 
     * @return 基于属性状态的哈希码
     */
    override fun hashCode(): Int {
        return bits.contentHashCode()
    }
    
    /**
     * 字符串表示 - 调试支持
     * 
     * 提供友好的字符串表示，显示启用的属性列表。
     * 仅用于调试和日志记录。
     * 
     * @return 包含启用属性的字符串表示
     */
    override fun toString(): String {
        val enabledAttrs = getSetAttributes()
        return "AttributePack(enabled=${enabledAttrs.size}, attrs=$enabledAttrs)"
    }
}