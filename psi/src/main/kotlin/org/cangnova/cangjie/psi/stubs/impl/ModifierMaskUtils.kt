/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi.stubs.impl

import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjModifierKeywordToken
import org.cangnova.cangjie.lexer.CjTokens.MODIFIER_KEYWORDS_ARRAY
import org.cangnova.cangjie.psi.CjModifierList

/**
 * 修饰符掩码工具
 *
 * ## 核心功能
 * 将修饰符列表编码为紧凑的位掩码（Long），用于 Stub 序列化，大幅减少内存占用和 I/O 开销。
 *
 * ## 设计原理
 * - 使用 64 位 Long 型整数作为位掩码，每个位代表一个修饰符
 * - 修饰符按照 MODIFIER_KEYWORDS_ARRAY 的顺序分配位位置
 * - 例如：public = 位 0，private = 位 1，static = 位 2，依此类推
 *
 * ## 性能优势
 * 1. **内存优化**：一个 Long (8 字节) 代替多个对象引用
 * 2. **序列化效率**：减少 Stub 索引文件大小
 * 3. **查询速度**：位运算比对象遍历快得多
 *
 * ## 限制
 * - 最多支持 64 个不同的修饰符（因为 Long 是 64 位）
 * - 如果修饰符超过 64 个，需要改用其他数据结构
 *
 * ## 使用场景
 * - Stub 序列化/反序列化（将修饰符保存到索引文件）
 * - 快速检查元素是否有特定修饰符
 * - 减少内存占用（特别是大型项目中有数万个声明时）
 */
object ModifierMaskUtils {
    /**
     * 初始化检查：确保修饰符数量不超过 64 个
     *
     * 如果修饰符超过 64 个，位掩码无法表示，需要改用其他实现
     */
    init {
        assert(MODIFIER_KEYWORDS_ARRAY.size <= 64) { "Current implementation depends on the ability to represent modifier list as bit mask" }
    }

    /**
     * 从修饰符列表计算位掩码
     *
     * ## 示例
     * ```
     * // 假设修饰符列表包含: public, static
     * // MODIFIER_KEYWORDS_ARRAY = [public, private, protected, static, ...]
     * // 结果掩码: 位 0 = 1 (public), 位 3 = 1 (static)
     * // 二进制: 0000...001001 = 9
     * ```
     *
     * @param modifierList PSI 修饰符列表
     * @return 64 位掩码，每个位代表对应位置的修饰符是否存在
     */
    @JvmStatic
    fun computeMaskFromModifierList(modifierList: CjModifierList): Long = computeMask { modifierList.hasModifier(it) }

    /**
     * 从修饰符检查函数计算位掩码
     *
     * ## 工作原理
     * 1. 遍历所有已知的修饰符关键字
     * 2. 对每个修饰符，调用 hasModifier 检查是否存在
     * 3. 如果存在，将对应位设置为 1（使用位或运算）
     *
     * ## 位运算说明
     * - `1L shl index`：将 1 左移 index 位，生成只有第 index 位为 1 的掩码
     * - `mask or (1L shl index)`：将该位设置为 1，保持其他位不变
     *
     * @param hasModifier 修饰符检查函数，返回是否包含指定修饰符
     * @return 计算出的位掩码
     */
    @JvmStatic
    fun computeMask(hasModifier: (CjModifierKeywordToken) -> Boolean): Long {
        var mask = 0L
        for ((index, modifierKeywordToken) in MODIFIER_KEYWORDS_ARRAY.withIndex()) {
            if (hasModifier(modifierKeywordToken)) {
                mask = mask or (1L shl index)
            }
        }
        return mask
    }

    /**
     * 检查掩码中是否包含指定修饰符
     *
     * ## 工作原理
     * 1. 查找修饰符在 MODIFIER_KEYWORDS_ARRAY 中的索引
     * 2. 使用位与运算检查该位是否为 1
     *
     * ## 位运算说明
     * - `1L shl index`：生成只有第 index 位为 1 的掩码
     * - `mask and (1L shl index)`：提取掩码的第 index 位
     * - `!= 0L`：如果该位为 1，结果不为 0，返回 true
     *
     * ## 示例
     * ```
     * // 假设 mask = 9 (二进制 1001)，index = 0 (public)
     * // 1L shl 0 = 1 (二进制 0001)
     * // 9 and 1 = 1 (二进制 0001)
     * // 1 != 0 → true (包含 public 修饰符)
     * ```
     *
     * @param mask 位掩码
     * @param modifierToken 要检查的修饰符关键字
     * @return 如果掩码包含该修饰符，返回 true
     */
    @JvmStatic
    fun maskHasModifier(mask: Long, modifierToken: CjKeywordToken): Boolean {
        val index = MODIFIER_KEYWORDS_ARRAY.indexOf(modifierToken)
        if (index < 0) {
            // unsafe、const、foreign 等关键字因兼具块语法角色（如 unsafe { }）而未提升为
            // CjModifierKeywordToken，不在 MODIFIER_KEYWORDS_ARRAY 中。
            // computeMask 不会编码这些 token，所以掩码中不可能包含它们，返回 false。
            return false
        }
        return (mask and (1L shl index)) != 0L
    }

    /**
     * 将掩码转换为可读的字符串表示
     *
     * ## 输出格式
     * `[public static final]` - 包含的修饰符用空格分隔，用方括号包裹
     *
     * ## 使用场景
     * - 调试时查看修饰符内容
     * - 日志记录
     * - 测试断言
     *
     * @param mask 位掩码
     * @return 修饰符的字符串表示
     */
    @JvmStatic
    fun maskToString(mask: Long): String {
        val sb = StringBuilder()
        sb.append("[")
        var first = true
        for (modifierKeyword in MODIFIER_KEYWORDS_ARRAY) {
            if (maskHasModifier(mask, modifierKeyword)) {
                if (!first) {
                    sb.append(" ")
                }
                sb.append(modifierKeyword.value)
                first = false
            }
        }
        sb.append("]")
        return sb.toString()
    }
}
