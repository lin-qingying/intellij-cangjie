/*
 * Copyright 2024 LinQingYing. and contributors.
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

package org.cangnova.cangjie.codeinsight

import org.cangnova.cangjie.codeinsight.CangJieNameSuggester.MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.capitalizeAsciiOnly
import org.cangnova.cangjie.utils.decapitalizeAsciiOnly

/**
 * 抽象仓颉名称建议器
 *
 * 为仓颉语言的各种元素（变量、参数、类型参数、类型别名等）提供智能的名称建议。
 * 这是名称建议功能的基础类，提供通用的名称生成和验证逻辑。
 *
 * **核心功能**：
 * - 根据全限定名生成变量名
 * - 验证名称并添加数字后缀避免冲突
 * - 为类型参数生成标准名称（T, U, V...）
 * - 为类型别名生成描述性名称
 * - 支持驼峰命名法转换
 *
 * **使用场景**：
 * - 代码补全：建议变量名
 * - 重构：自动生成参数名、局部变量名
 * - 快速修复：建议缺失的声明名称
 * - 代码生成：为生成的代码元素命名
 *
 * **子类**：
 * - [CangJieNameSuggester] - 具体实现类，提供完整的名称建议功能
 *
 * **命名约定**：
 * - 变量名：小驼峰（camelCase）
 * - 类型名：大驼峰（PascalCase）
 * - 类型参数：单个大写字母（T, U, V...）
 * - 常量：全大写加下划线（CONSTANT_NAME）
 */
abstract class AbstractCangJieNameSuggester {
    /**
     * 根据全限定名建议变量名
     *
     * 从全限定名中提取各个部分，组合生成符合规范的变量名候选列表。
     * 采用从右到左（从最具体到最抽象）的顺序生成名称。
     *
     * **命名策略**：
     * - 反向遍历全限定名的各个部分
     * - 逐步添加前缀，生成越来越具体的名称
     * - 跳过 Companion 对象（通常不需要）
     * - 所有名称都经过验证器检查
     *
     * **示例**：
     * ```kotlin
     * // FqName: std.collection.ArrayList
     * // 生成的候选名称：
     * // 1. arrayList
     * // 2. collectionArrayList
     * // 3. stdCollectionArrayList
     *
     * // FqName: MyClass.Companion.create
     * // ignoreCompanion = true 时：
     * // 1. create
     * // 2. myClassCreate
     * ```
     *
     * **大小写处理**：
     * - 如果当前名称以小写开头，新前缀也小写
     * - 如果当前名称以大写开头，新前缀大写
     * - 确保符合变量命名规范
     *
     * @param fqName 全限定名（如 std.collection.ArrayList）
     * @param ignoreCompanion 是否忽略 Companion 部分（默认 true）
     * @param validator 名称验证器，检查名称是否可用（不冲突、符合规范等）
     * @param defaultName 默认名称生成器，当没有有效候选时使用
     * @return Collection<String> 候选名称列表（按优先级排序）
     */
    fun suggestNamesByFqName(
        fqName: FqName,
        ignoreCompanion: Boolean = true,
        validator: (String) -> Boolean = { true },
        defaultName: () -> String? = { null }
    ): Collection<String> {
        val result = LinkedHashSet<String>()

        var name = ""
        fqName.asString().split('.').asReversed().forEach {
            if (ignoreCompanion && it == "Companion") return@forEach
            name = name.withPrefix(it)
            result.addName(name, validator)
        }

        if (result.isEmpty()) {
            result.addName(defaultName(), validator)
        }

        return result
    }


    /**
     * 验证名称并在冲突时添加数字后缀
     *
     * 如果原始名称可用，直接返回。否则尝试添加数字后缀（1, 2, 3...）
     * 直到找到一个可用的名称。
     *
     * **冲突解决策略**：
     * - 首先尝试不带后缀的名称
     * - 然后尝试 name1, name2, name3...
     * - 最多尝试 [MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS] 次
     *
     * **示例**：
     * ```kotlin
     * // 假设作用域中已有 value, value1, value2
     * suggestNameByName("value", validator)  // 返回 "value3"
     *
     * // 假设作用域中没有 count
     * suggestNameByName("count", validator)  // 返回 "count"
     * ```
     *
     * @param name 待验证的名称
     * @param validator 验证器函数，检查名称是否在作用域中可用
     * @return String 可用的名称（可能带数字后缀）
     */
    fun suggestNameByName(name: String, validator: (String) -> Boolean): String {
        if (validator(name)) return name
        var i = 1
        while (i <= MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS && !validator(name + i)) {
            ++i
        }

        return name + i
    }

    /**
     * 为多个类型参数生成名称
     *
     * 生成指定数量的类型参数名称，使用标准的单字母命名约定（T, U, V...）。
     * 确保所有名称都通过验证器检查，如有冲突会添加数字后缀。
     *
     * **标准命名序列**：
     * T, U, V, W, X, Y, Z（来自 [COMMON_TYPE_PARAMETER_NAMES]）
     *
     * **冲突处理**：
     * - 如果 T 已被使用，尝试 T1, T2...
     * - 然后尝试 U, U1, U2...
     * - 以此类推
     *
     * **示例**：
     * ```kotlin
     * // 生成 3 个类型参数名称
     * suggestNamesForTypeParameters(3, validator)
     * // 可能返回：["T", "U", "V"]
     *
     * // 如果 T 已被使用
     * // 可能返回：["T1", "U", "V"]
     * ```
     *
     * @param count 需要的类型参数数量
     * @param validator 验证器函数
     * @return List<String> 类型参数名称列表
     */
    fun suggestNamesForTypeParameters(count: Int, validator: (String) -> Boolean): List<String> {
        val result = ArrayList<String>()
        for (i in 0 until count) {
            result.add(suggestNameByMultipleNames(COMMON_TYPE_PARAMETER_NAMES, validator))
        }
        return result
    }

    /**
     * 根据类型元素 PSI 建议类型别名名称
     *
     * 为类型别名（type alias）生成描述性的名称。
     * 通过渲染类型元素的结构，生成简洁且具有描述性的名称。
     *
     * **渲染规则**：
     * - **可空类型**：添加 "Option" 前缀（如 OptionInt）
     * - **函数类型**：
     *   - 参数类型拼接 + "To" + 返回类型
     *   - 例如：`(Int, String) -> Bool` → `IntStringToBool`
     * - **用户类型**：
     *   - 类型参数 + 类型名
     *   - 例如：`List<Int>` → `IntList`
     * - **其他类型**：首字母大写的类型文本
     *
     * **示例**：
     * ```kotlin
     * // 类型：Int?
     * // 建议名称：OptionInt
     *
     * // 类型：(String) -> Int
     * // 建议名称：StringToInt
     *
     * // 类型：Map<String, Int>
     * // 建议名称：StringIntMap
     * ```
     *
     * @param typeElement 类型元素的 PSI 节点
     * @param validator 验证器函数
     * @return String 建议的类型别名名称
     */
    fun suggestTypeAliasNameByPsi(typeElement: CjTypeElement, validator: (String) -> Boolean): String {
        fun CjTypeElement.render(): String {
            return when (this) {
                is CjOptionType -> "Option${getInnerType()?.render() ?: ""}"
                is CjFunctionType -> {
                    val arguments = listOfNotNull(receiverTypeReference) + parameters.mapNotNull { it.typeReference }
                    val argText = arguments.joinToString(separator = "") { it.typeElement?.render() ?: "" }
                    val returnText = returnTypeReference?.typeElement?.render() ?: "Unit"
                    "${argText}To$returnText"
                }
                is CjUserType -> {
                    val argText = typeArguments.joinToString(separator = "") { it.typeReference?.typeElement?.render() ?: "" }
                    "$argText${referenceExpression?.text ?: ""}"
                }
                else -> text.capitalizeAsciiOnly()
            }
        }

        return suggestNameByName(typeElement.render(), validator)
    }

    /**
     * 从多个候选名称中选择一个可用的名称
     *
     * 尝试多个名称变体，按顺序检查每个变体，直到找到可用的。
     * 如果所有变体都不可用，添加数字后缀并重复尝试。
     *
     * **尝试顺序**（假设候选列表为 [a, b, c]）：
     * 1. 第一轮：a, b, c
     * 2. 第二轮：a1, b1, c1
     * 3. 第三轮：a2, b2, c2
     * 4. 以此类推...
     *
     * **使用场景**：
     * - 类型参数命名：从 T, U, V... 中选择
     * - 变量命名：从多个语义相关的名称中选择
     * - 确保有备用方案
     *
     * **示例**：
     * ```kotlin
     * val names = listOf("value", "item", "element")
     * // 假设 value 和 item 都被占用
     * suggestNameByMultipleNames(names, validator)
     * // 返回 "element"
     *
     * // 如果 value, item, element 都被占用
     * // 返回 "value1"
     * ```
     *
     * @param names 候选名称列表
     * @param validator 验证器函数
     * @return String 第一个可用的名称（可能带数字后缀）
     */
    fun suggestNameByMultipleNames(names: Collection<String>, validator: (String) -> Boolean): String {
        var i = 0
        while (true) {
            for (name in names) {
                val candidate = if (i > 0) name + i else name
                if (validator(candidate)) return candidate
            }
            i++
        }
    }

    /**
     * 添加驼峰命名的名称变体
     *
     * 将给定名称转换为驼峰命名法的多种形式，并添加到集合中。
     * 委托给 [CangJieNameSuggester.getCamelNames] 进行转换。
     *
     * @param name 原始名称
     * @param validator 验证器函数
     * @param mustStartWithLowerCase 是否必须以小写字母开头（默认 true）
     */
    protected fun MutableCollection<String>.addCamelNames(
        name: String,
        validator: (String) -> Boolean,
        mustStartWithLowerCase: Boolean = true
    ) {
        addAll(CangJieNameSuggester.getCamelNames(name, validator, mustStartWithLowerCase))
    }

    /**
     * 根据表达式 PSI 添加名称建议
     *
     * 从表达式的语法结构中提取语义信息，生成相关的名称建议。
     * 委托给 [CangJieNameSuggester.suggestNamesByExpressionPSI] 进行分析。
     *
     * @param expression 表达式 PSI 节点
     * @param validator 验证器函数
     */
    protected fun MutableCollection<String>.addNamesByExpressionPSI(expression: CjExpression?, validator: (String) -> Boolean) {
        addAll(CangJieNameSuggester.suggestNamesByExpressionPSI(expression, validator))
    }

    /**
     * 验证并添加单个名称
     *
     * 将名称转换为有效的标识符并添加到集合中（如果通过验证）。
     * 委托给 [CangJieNameSuggester.suggestNameByValidIdentifierName] 进行验证和规范化。
     *
     * @param name 待添加的名称（可为 null）
     * @param validator 验证器函数
     */
    protected fun MutableCollection<String>.addName(name: String?, validator: (String) -> Boolean) {
        addIfNotNull(CangJieNameSuggester.suggestNameByValidIdentifierName(name, validator, false))
    }

    /**
     * 为字符串添加前缀
     *
     * 根据当前字符串的首字母大小写，智能地添加前缀。
     * 确保生成的组合名称符合驼峰命名规范。
     *
     * **规则**：
     * - 如果当前字符串为空，返回前缀本身
     * - 如果当前字符串首字母小写，前缀也小写
     * - 如果当前字符串首字母大写，前缀也大写
     * - 当前字符串首字母大写后拼接
     *
     * **示例**：
     * ```kotlin
     * "".withPrefix("array")          // "array"
     * "list".withPrefix("Array")      // "arrayList"
     * "List".withPrefix("array")      // "ArrayList"
     * "List".withPrefix("HashMap")    // "HashMapList"
     * ```
     *
     * @param prefix 要添加的前缀
     * @return String 组合后的名称
     */
    private fun String.withPrefix(prefix: String): String {
        if (isEmpty()) return prefix
        val c = this[0]
        return (if (c in 'a'..'z') prefix.decapitalizeAsciiOnly()
        else prefix.capitalizeAsciiOnly()) + capitalizeAsciiOnly()
    }

    companion object {
        /**
         * 常用的类型参数名称列表
         *
         * 遵循标准的泛型命名约定，使用单个大写字母。
         * 按字母顺序排列：T, U, V, W, X, Y, Z
         *
         * **惯例**：
         * - T: Type（最常用的类型参数）
         * - U, V: 后续类型参数
         * - E: Element（集合元素）
         * - K: Key（映射键）
         * - V: Value（映射值）
         * - R: Result/Return（返回类型）
         */
        private val COMMON_TYPE_PARAMETER_NAMES = listOf("T", "U", "V", "W", "X", "Y", "Z")
    }
}
