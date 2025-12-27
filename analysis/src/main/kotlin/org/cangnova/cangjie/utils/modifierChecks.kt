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

package org.cangnova.cangjie.utils

import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.ReceiverParameterDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.descriptors.findClassifierAcrossModuleDependencies
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.name.OperatorNameConventions.NOT_EQUALS
import org.cangnova.cangjie.resolve.classId
import org.cangnova.cangjie.resolve.declaresOrInheritsDefaultValue
import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.resolve.scopes.receivers.ImplicitClassReceiver
import org.cangnova.cangjie.types.isSubtypeOf


/**
 * 函数检查规则接口
 *
 * 定义了检查函数描述符是否符合特定规范的统一结构。
 * 所有检查规则都实现此接口，提供检查逻辑和错误描述。
 *
 * 使用方式：
 * 1. 实现 [check] 方法定义检查逻辑
 * 2. 提供 [description] 属性作为检查失败时的错误消息
 * 3. 通过 [invoke] 运算符快速执行检查并获取错误信息
 */
interface Check {
    /**
     * 检查失败时返回的错误描述信息
     *
     * 例如："must be a member function"、"must have exactly 2 value parameters"
     */
    val description: String

    /**
     * 执行检查逻辑
     *
     * @param functionDescriptor 要检查的函数描述符
     * @return Boolean true 表示检查通过，false 表示检查失败
     */
    fun check(functionDescriptor: FunctionDescriptor): Boolean

    /**
     * 运算符重载，快速执行检查并返回错误信息
     *
     * 如果检查失败，返回错误描述；如果检查通过，返回 null。
     *
     * @param functionDescriptor 要检查的函数描述符
     * @return String? 检查失败时返回错误描述，检查通过时返回 null
     */
    operator fun invoke(functionDescriptor: FunctionDescriptor): String? =
        if (!check(functionDescriptor)) description else null
}

/**
 * 函数检查规则集合
 *
 * 封装一组针对特定函数名称或模式的检查规则。
 * 支持多种匹配方式：精确名称、正则表达式、名称列表。
 *
 * 检查流程：
 * 1. [isApplicable] 判断规则是否适用于目标函数
 * 2. [checkAll] 依次执行所有检查规则
 * 3. 返回 [CheckResult] 表示检查结果
 *
 * @property name 精确匹配的函数名称，null 表示不使用名称匹配
 * @property regex 正则表达式匹配的函数名称模式，null 表示不使用正则匹配
 * @property nameList 多个函数名称的列表，null 表示不使用列表匹配
 * @property additionalCheck 额外的自定义检查逻辑
 * @property checks 一组基础检查规则
 */
internal class Checks private constructor(
    val name: Name?,
    val regex: Regex?,
    private val nameList: Collection<Name>?,
    val additionalCheck: (FunctionDescriptor) -> String?,
    vararg val checks: Check
) {
    /**
     * 判断当前检查规则是否适用于给定的函数描述符
     *
     * 匹配优先级：
     * 1. 如果设置了 [name]，函数名必须精确匹配
     * 2. 如果设置了 [regex]，函数名必须匹配正则表达式
     * 3. 如果设置了 [nameList]，函数名必须在列表中
     *
     * @param functionDescriptor 要检查的函数描述符
     * @return Boolean true 表示规则适用于该函数，false 表示不适用
     */
    fun isApplicable(functionDescriptor: FunctionDescriptor): Boolean {
        if (name != null && functionDescriptor.name != name) return false
        if (regex != null && !functionDescriptor.name.asString().matches(regex)) return false
        if (nameList != null && functionDescriptor.name !in nameList) return false
        return true
    }

    /**
     * 执行所有检查规则，并返回检查结果
     *
     * 按顺序执行：
     * 1. 遍历 [checks] 中的基础检查规则
     * 2. 执行 [additionalCheck] 中的自定义检查逻辑
     * 3. 任何一个检查失败，立即返回失败结果
     * 4. 所有检查通过，返回成功结果
     *
     * @param functionDescriptor 要检查的函数描述符
     * @return CheckResult 检查结果
     */
    fun checkAll(functionDescriptor: FunctionDescriptor): CheckResult {
        for (check in checks) {
            val checkResult = check(functionDescriptor)
            if (checkResult != null) {
                return CheckResult.IllegalSignature(checkResult)
            }
        }

        val additionalCheckResult = additionalCheck(functionDescriptor)
        if (additionalCheckResult != null) {
            return CheckResult.IllegalSignature(additionalCheckResult)
        }

        return CheckResult.SuccessCheck
    }

    /**
     * 创建通用检查规则集合（不限定函数名称）
     *
     * @param checks 一组检查规则
     * @param additionalChecks 额外的自定义检查逻辑
     */
    constructor(vararg checks: Check, additionalChecks: FunctionDescriptor.() -> String? = { null })
            : this(null, null, null, additionalChecks, *checks)

    /**
     * 创建针对特定函数名称的检查规则集合
     *
     * @param name 精确匹配的函数名称
     * @param checks 一组检查规则
     * @param additionalChecks 额外的自定义检查逻辑
     */
    constructor(name: Name, vararg checks: Check, additionalChecks: FunctionDescriptor.() -> String? = { null })
            : this(name, null, null, additionalChecks, *checks)

    /**
     * 创建针对正则表达式匹配的函数名称的检查规则集合
     *
     * @param regex 正则表达式模式
     * @param checks 一组检查规则
     * @param additionalChecks 额外的自定义检查逻辑
     */
    constructor(regex: Regex, vararg checks: Check, additionalChecks: FunctionDescriptor.() -> String? = { null })
            : this(null, regex, null, additionalChecks, *checks)

    /**
     * 创建针对函数名称列表的检查规则集合
     *
     * @param nameList 函数名称列表
     * @param checks 一组检查规则
     * @param additionalChecks 额外的自定义检查逻辑
     */
    constructor(
        nameList: Collection<Name>,
        vararg checks: Check,
        additionalChecks: FunctionDescriptor.() -> String? = { null }
    )
            : this(null, null, nameList, additionalChecks, *checks)
}

/**
 * 函数修饰符检查器抽象基类
 *
 * 提供了检查函数描述符是否符合特定修饰符要求的通用框架。
 * 子类需要实现 [checks] 属性，定义具体的检查规则列表。
 *
 * 典型用例：
 * - [OperatorChecks] 检查运算符重载函数的签名
 */
abstract class AbstractModifierChecks {
    /**
     * 具体的检查规则列表
     *
     * 子类必须实现此属性，提供一组针对特定场景的检查规则。
     */
    internal abstract val checks: List<Checks>

    /**
     * 确保给定的条件成立，否则提供一个错误信息生成函数
     *
     * 此函数主要用于在给定条件不满足时，提供一个延迟计算的错误信息。
     * 这种方式可以避免在条件成立时无谓地构造错误信息字符串，从而提高效率。
     *
     * 使用示例：
     * ```kotlin
     * ensure(parameters.size >= 2) { "must have at least 2 parameters" }
     * ```
     *
     * @param cond 需要检查的条件，如果条件为假，则会执行错误信息的生成函数
     * @param msg 一个无参数的 lambda 表达式，用于在条件不满足时生成错误信息
     * @return String? 如果条件满足，返回 null；否则返回由 [msg] 生成的错误信息字符串
     */
    inline fun ensure(cond: Boolean, msg: () -> String) = if (!cond) msg() else null

    /**
     * 检查给定的函数描述符是否符合任何预定义的检查规则
     *
     * 检查流程：
     * 1. 遍历 [checks] 中的所有检查规则
     * 2. 对每个规则，先判断是否适用于该函数（通过 [Checks.isApplicable]）
     * 3. 如果适用，执行该规则的所有检查（通过 [Checks.checkAll]）
     * 4. 如果没有任何规则适用，返回 [CheckResult.IllegalFunctionName]
     *
     * @param functionDescriptor 函数描述符，包含函数的相关信息
     * @return CheckResult 检查结果
     * - [CheckResult.SuccessCheck] - 所有检查都通过
     * - [CheckResult.IllegalSignature] - 函数签名不符合规范
     * - [CheckResult.IllegalFunctionName] - 函数名称不合法（没有匹配的检查规则）
     */
    fun check(functionDescriptor: FunctionDescriptor): CheckResult {
        // 遍历所有预定义的检查规则
        for (check in checks) {
            // 检查当前规则是否适用于给定的函数描述符
            if (!check.isApplicable(functionDescriptor)) continue
            // 如果适用，则执行该规则的检查，并返回结果
            return check.checkAll(functionDescriptor)
        }

        // 如果没有规则适用，则返回函数名称不合法的结果
        return CheckResult.IllegalFunctionName
    }
}

/**
 * 成员类型检查规则
 *
 * 检查函数是否为成员函数或扩展函数。
 * 仓颉语言支持类成员函数和 extend 扩展函数。
 */
sealed class MemberKindCheck(override val description: String) : Check {
    /**
     * 检查函数是否为成员函数或扩展函数
     *
     * 通过检查 [FunctionDescriptor.dispatchReceiverParameter] 是否存在来判断。
     * - 成员函数：定义在类或 struct 内部的函数
     * - 扩展函数：通过 extend 块为类型添加的函数
     */
    data object MemberOrExtension : MemberKindCheck("must be a member or an extension function") {
        /**
         * 检查给定的函数描述符是否表示一个成员函数或扩展函数
         *
         * @param functionDescriptor 函数描述符，包含函数的反射信息
         * @return Boolean true 表示函数是成员函数或扩展函数，false 表示是顶层函数
         */
        override fun check(functionDescriptor: FunctionDescriptor) =
            functionDescriptor.dispatchReceiverParameter != null
    }

    /**
     * 检查函数是否为成员函数
     *
     * 要求函数必须定义在类或 struct 内部，不包括 extend 扩展函数。
     */
    data object Member : MemberKindCheck("must be a member function") {
        /**
         * 检查给定的函数描述符是否表示一个成员函数
         *
         * @param functionDescriptor 函数描述符，包含函数的反射信息
         * @return Boolean true 表示函数是成员函数，false 表示不是
         */
        override fun check(functionDescriptor: FunctionDescriptor) =
            functionDescriptor.dispatchReceiverParameter != null
    }
}

/**
 * 值参数数量检查规则
 *
 * 检查函数的值参数（value parameters）数量是否符合要求。
 * 仓颉语言的运算符重载对参数数量有严格的限制。
 */
sealed class ValueParameterCountCheck(override val description: String) : Check {
    /**
     * 检查函数是否没有值参数
     *
     * 适用于一元运算符，如 `operator func !(): Bool`
     */
    data object NoValueParameters : ValueParameterCountCheck("must have no value parameters") {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.isEmpty()
    }

    /**
     * 检查函数是否有且仅有一个值参数
     *
     * 适用于二元运算符，如 `operator func +(other: Int): Int`
     */
    data object SingleValueParameter : ValueParameterCountCheck("must have a single value parameter") {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size == 1
    }

    /**
     * 检查命名参数规则
     *
     * 仓颉语言的特殊规则：
     * - 只能有一个命名参数，且必须命名为 'value'
     * - 用于下标运算符 `operator func [](index: Int): T` 和 `operator func []=(index: Int, value: T)`
     */
    data object NamedAndValue : ValueParameterCountCheck("can only have one named parameter 'value'") {
        override fun check(functionDescriptor: FunctionDescriptor): Boolean {

            val last = functionDescriptor.valueParameters.lastOrNull()
            if (last?.isNamed == true && last.name != Name.identifier("value")) {
                return false
            }
            // 去掉最后一个参数后，检查其他参数是否有命名参数
            return !functionDescriptor.valueParameters.dropLast(1).any {
                it.isNamed
            }
        }


    }

    /**
     * 检查函数至少应包含的值参数数量
     *
     * 用于需要多个参数的运算符，如下标访问 `operator func [](i: Int, j: Int): T`
     *
     * @param n 函数至少应包含的值参数数量
     */
    class AtLeast(val n: Int) :
        ValueParameterCountCheck("must have at least $n value parameter" + (if (n > 1) "s" else "")) {
        /**
         * 检查给定函数描述符的值参数数量是否符合要求
         *
         * @param functionDescriptor 函数描述符，包含函数的元数据信息
         * @return Boolean 函数的值参数数量是否至少为 [n]
         */
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size >= n
    }

    /**
     * 检查函数是否具有指定数量的值参数
     *
     * 用于参数数量固定的运算符检查。
     *
     * @param n 指定的值参数数量，函数必须具有确切的 [n] 个值参数
     */
    class Equals(val n: Int) : ValueParameterCountCheck("must have exactly $n value parameters") {
        /**
         * 检查给定函数描述符的函数是否具有指定数量的值参数
         *
         * @param functionDescriptor 函数描述符，包含函数的相关信息
         * @return Boolean 函数的值参数数量是否等于 [n]
         */
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size == n
    }
}

/**
 * 运算符重载函数检查规则
 *
 * 提供了仓颉语言所有运算符重载函数的签名检查规则。
 * 确保运算符重载函数符合语言规范，包括参数数量、命名参数、默认值等。
 *
 * 支持的运算符检查：
 * - 下标运算符：`operator func [](index: Int): T` 和 `operator func []=(index: Int, value: T)`
 * - 一元运算符：`operator func !(): Bool`
 * - 二元运算符：`operator func +(other: T): T`、`operator func ==(other: T): Bool` 等
 * - 调用运算符：`operator func invoke(...): T`
 * - 比较运算符：`operator func >(other: T): Bool`、`operator func <(other: T): Bool` 等
 * - 位运算符：`operator func <<(count: Int): T`、`operator func >>(count: Int): T` 等
 */
object OperatorChecks : AbstractModifierChecks() {
    override val checks by lazy {
        listOf(
            // 下标访问运算符 []
            // 要求：至少一个参数，支持命名参数 'value'
            Checks(
                OperatorNameConventions.GET, MemberKindCheck.MemberOrExtension, ValueParameterCountCheck.AtLeast(1),
                ValueParameterCountCheck.NamedAndValue
            ),


            // 下标赋值运算符 []=
            // 要求：至少两个参数（索引 + value），最后一个参数不能有默认值或 vararg
            Checks(
                OperatorNameConventions.SET,
                MemberKindCheck.MemberOrExtension,
                ValueParameterCountCheck.NamedAndValue,
                ValueParameterCountCheck.AtLeast(2)
            ) {
                val lastIsOk =
                    valueParameters.lastOrNull()
                        ?.let { !it.declaresOrInheritsDefaultValue() && it.varargElementType == null } == true
                ensure(lastIsOk) { "last parameter should not have a default value or be a vararg" }

            },

            // 逻辑非运算符 !
            // 要求：无参数
            Checks(
                OperatorNameConventions.NOT,
                ValueParameterCountCheck.Equals(0),
            ),

            // 其他运算符：invoke、!=、==、&&、||、+、-、*、/、&、|、^、>、<、>=、<=、<<、>>
            // 这些运算符没有特殊的参数约束，只要求函数签名合法即可
            Checks(
                nameList = listOf(
                    OperatorNameConventions.INVOKE,         // 调用运算符
                    OperatorNameConventions.NOT_EQUALS,     // 不等于 !=
                    OperatorNameConventions.EQUALS,         // 等于 ==

                    OperatorNameConventions.ANDAND,         // 逻辑与 &&
                    OperatorNameConventions.OROR,           // 逻辑或 ||
                    OperatorNameConventions.PLUS,           // 加法 +
                    OperatorNameConventions.MINUS,          // 减法 -
                    OperatorNameConventions.TIMES,          // 乘法 *
                    OperatorNameConventions.DIV,            // 除法 /
                    OperatorNameConventions.AND,            // 位与 &
                    OperatorNameConventions.OR,             // 位或 |
                    OperatorNameConventions.XOR,            // 位异或 ^
                    OperatorNameConventions.COMPARE_GT,     // 大于 >
                    OperatorNameConventions.COMPARE_LT,     // 小于 <
                    OperatorNameConventions.COMPARE_GTEQ,   // 大于等于 >=
                    OperatorNameConventions.COMPARE_LTEQ,   // 小于等于 <=
                    OperatorNameConventions.LEFT_SHIFT,     // 左移 <<
                    OperatorNameConventions.RIGHT_SHIFT,    // 右移 >>
                )


            )

        )


    }


    /**
     * 检查 expect 类的自增/自减运算符返回类型
     *
     * 仓颉语言的特殊规则：
     * - expect 类的自增/自减运算符返回类型必须是 actual 类型别名展开后的类型的子类型
     *
     * @receiver FunctionDescriptor 函数描述符
     * @param receiver 接收器参数描述符
     * @return Boolean true 表示返回类型符合要求
     */
    private fun FunctionDescriptor.incDecCheckForExpectClass(receiver: ReceiverParameterDescriptor): Boolean {
        val receiverValue = receiver.value
        if (receiverValue !is ImplicitClassReceiver) return false

        val classDescriptor = receiverValue.classDescriptor

        val potentialActualAliasId = classDescriptor.classId ?: return false
        val actualReceiverTypeAlias =
            classDescriptor.module.findClassifierAcrossModuleDependencies(potentialActualAliasId) as? TypeAliasDescriptor
                ?: return false

        returnType?.let { returnType ->
            return returnType.isSubtypeOf(actualReceiverTypeAlias.expandedType)
        }

        return false
    }
}

/**
 * 检查结果密封类
 *
 * 表示函数签名检查的结果，包含三种可能的情况：
 * - [SuccessCheck] - 检查通过
 * - [IllegalSignature] - 函数签名不合法（包含错误信息）
 * - [IllegalFunctionName] - 函数名称不合法（没有匹配的检查规则）
 *
 * @property isSuccess 检查是否成功
 */
sealed class CheckResult(val isSuccess: Boolean) {
    /**
     * 函数签名不合法
     *
     * 包含具体的错误描述信息，例如：
     * - "must be a member function"
     * - "must have exactly 2 value parameters"
     * - "last parameter should not have a default value or be a vararg"
     *
     * @property error 错误描述信息
     */
    class IllegalSignature(val error: String) : CheckResult(false)

    /**
     * 函数名称不合法
     *
     * 表示函数名称不在预定义的运算符列表中，或没有匹配的检查规则。
     */
    object IllegalFunctionName : CheckResult(false)

    /**
     * 检查成功
     *
     * 表示函数签名符合所有检查规则的要求。
     */
    object SuccessCheck : CheckResult(true)
}
