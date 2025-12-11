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
 * Check接口定义了检查函数描述符的结构
 */
interface Check {
    val description: String
    fun check(functionDescriptor: FunctionDescriptor): Boolean
    operator fun invoke(functionDescriptor: FunctionDescriptor): String? =
        if (!check(functionDescriptor)) description else null
}

/**
 * Checks类负责根据不同的条件检查函数描述符是否符合规范
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
     */
    fun isApplicable(functionDescriptor: FunctionDescriptor): Boolean {
        if (name != null && functionDescriptor.name != name) return false
        if (regex != null && !functionDescriptor.name.asString().matches(regex)) return false
        if (nameList != null && functionDescriptor.name !in nameList) return false
        return true
    }

    /**
     * 执行所有检查规则，并返回检查结果
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
     * 构造函数，根据检查规则和额外的检查逻辑创建Checks实例
     */
    constructor(vararg checks: Check, additionalChecks: FunctionDescriptor.() -> String? = { null })
            : this(null, null, null, additionalChecks, *checks)

    constructor(name: Name, vararg checks: Check, additionalChecks: FunctionDescriptor.() -> String? = { null })
            : this(name, null, null, additionalChecks, *checks)

    constructor(regex: Regex, vararg checks: Check, additionalChecks: FunctionDescriptor.() -> String? = { null })
            : this(null, regex, null, additionalChecks, *checks)

    constructor(
        nameList: Collection<Name>,
        vararg checks: Check,
        additionalChecks: FunctionDescriptor.() -> String? = { null }
    )
            : this(null, null, nameList, additionalChecks, *checks)
}

/**
 * AbstractModifierChecks类提供了检查函数描述符的抽象结构
 */
abstract class AbstractModifierChecks {
    internal abstract val checks: List<Checks>

    /**
     * 确保给定的条件成立，否则提供一个错误信息生成函数
     *
     * 此函数主要用于在给定条件不满足时，提供一个延迟计算的错误信息这种方式可以避免在条件成立时无谓地构造错误信息字符串，从而提高效率
     *
     * @param cond 需要检查的条件，如果条件为假，则会执行错误信息的生成函数
     * @param msg 一个无参数的lambda表达式，用于在条件不满足时生成错误信息返回值为错误信息字符串，如果条件满足，则返回null
     *
     * @return 如果条件满足，返回null；否则返回由msg生成的错误信息字符串
     */
    inline fun ensure(cond: Boolean, msg: () -> String) = if (!cond) msg() else null

    /**
     * 检查给定的函数描述符是否符合任何预定义的检查规则
     *
     * @param functionDescriptor 函数描述符，包含函数的相关信息
     * @return 返回检查结果，如果所有检查都通过，则返回对应的检查结果；如果函数名称不合法，则返回CheckResult.IllegalFunctionName
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
 * MemberKindCheck检查函数是否为成员函数或扩展函数
 */
sealed class MemberKindCheck(override val description: String) : Check {
    /**
     * 检查函数是否为成员函数或扩展函数的类
     * 继承自 MemberKindCheck 类，实现了其检查逻辑
     */
    data object MemberOrExtension : MemberKindCheck("must be a member or an extension function") {
        /**
         * 检查给定的函数描述符是否表示一个成员函数或扩展函数
         *
         * @param functionDescriptor 函数描述符，包含函数的反射信息
         * @return Boolean 表示函数是否为成员函数或扩展函数
         */
        override fun check(functionDescriptor: FunctionDescriptor) =
            functionDescriptor.dispatchReceiverParameter != null || functionDescriptor.extensionReceiverParameter != null
    }

    /**
     * 检查函数是否为成员函数的类
     * 继承自 MemberKindCheck 类，实现了其检查逻辑
     */
    data object Member : MemberKindCheck("must be a member function") {
        /**
         * 检查给定的函数描述符是否表示一个成员函数
         *
         * @param functionDescriptor 函数描述符，包含函数的反射信息
         * @return Boolean 表示函数是否为成员函数
         */
        override fun check(functionDescriptor: FunctionDescriptor) =
            functionDescriptor.dispatchReceiverParameter != null
    }
}

/**
 * ValueParameterCountCheck检查函数的值参数数量
 */
sealed class ValueParameterCountCheck(override val description: String) : Check {
    data object NoValueParameters : ValueParameterCountCheck("must have no value parameters") {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.isEmpty()
    }

    data object SingleValueParameter : ValueParameterCountCheck("must have a single value parameter") {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size == 1
    }

    data object NamedAndValue : ValueParameterCountCheck("can only have one named parameter 'value'") {
        override fun check(functionDescriptor: FunctionDescriptor): Boolean {

            val last = functionDescriptor.valueParameters.lastOrNull()
            if (last?.isNamed == true && last.name != Name.identifier("value")) {
                return false
            }
//去掉最后一个
            return !functionDescriptor.valueParameters.dropLast(1).any {
                it.isNamed
            }
        }


    }

    /**
     * AtLeast类继承自ValueParameterCountCheck，用于检查函数至少应包含的值参数数量
     * 它通过构造函数接收一个整数参数n，并确保被检查的函数至少有n个值参数
     *
     * @param n 函数至少应包含的值参数数量
     */
    class AtLeast(val n: Int) :
        ValueParameterCountCheck("must have at least $n value parameter" + (if (n > 1) "s" else "")) {
        /**
         * 检查给定函数描述符的值参数数量是否符合要求
         *
         * @param functionDescriptor 函数描述符，包含函数的元数据信息
         * @return 布尔值，指示函数的值参数数量是否至少为n
         */
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size >= n
    }

    /**
     * 检查函数是否具有指定数量值参数的类
     * 继承自ValueParameterCountCheck类，并实现其抽象方法check
     *
     * @param n 指定的值参数数量，函数必须具有确切的n个值参数
     */
    class Equals(val n: Int) : ValueParameterCountCheck("must have exactly $n value parameters") {
        /**
         * 检查给定函数描述符的函数是否具有指定数量的值参数
         *
         * @param functionDescriptor 函数描述符，包含函数的相关信息
         * @return 如果函数的值参数数量等于指定的数量n，则返回true，否则返回false
         */
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size == n
    }
}

/**
 * OperatorChecks对象提供了运算符重载函数的检查规则
 */
object OperatorChecks : AbstractModifierChecks() {
    override val checks by lazy {
        listOf(
            Checks(
                OperatorNameConventions.GET, MemberKindCheck.MemberOrExtension, ValueParameterCountCheck.AtLeast(1),
                ValueParameterCountCheck.NamedAndValue
            ),


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

//            检查 ! 的重载
            Checks(
                OperatorNameConventions.NOT,
                ValueParameterCountCheck.Equals(0),
            ),
            Checks(
                nameList = listOf(
                    OperatorNameConventions.INVOKE,
                    OperatorNameConventions.NOT_EQUALS,
                    OperatorNameConventions.EQUALS,

                    OperatorNameConventions.ANDAND,
                    OperatorNameConventions.OROR,
                    OperatorNameConventions.PLUS,
                    OperatorNameConventions.MINUS,
                    OperatorNameConventions.TIMES,
                    OperatorNameConventions.DIV,
                    OperatorNameConventions.AND,
                    OperatorNameConventions.OR,
                    OperatorNameConventions.XOR,
                    OperatorNameConventions.COMPARE_GT,
                    OperatorNameConventions.COMPARE_LT,
                    OperatorNameConventions.COMPARE_GTEQ,
                    OperatorNameConventions.COMPARE_LTEQ,
                    OperatorNameConventions.LEFT_SHIFT,
                    OperatorNameConventions.RIGHT_SHIFT,
                )


            )

        )


    }


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

sealed class CheckResult(val isSuccess: Boolean) {
    class IllegalSignature(val error: String) : CheckResult(false)
    object IllegalFunctionName : CheckResult(false)
    object SuccessCheck : CheckResult(true)
}
