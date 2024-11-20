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

package com.linqingying.cangjie.utils

import com.linqingying.cangjie.descriptors.FunctionDescriptor
import com.linqingying.cangjie.descriptors.ReceiverParameterDescriptor
import com.linqingying.cangjie.descriptors.TypeAliasDescriptor
import com.linqingying.cangjie.descriptors.findClassifierAcrossModuleDependencies
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.descriptorUtil.classId
import com.linqingying.cangjie.resolve.descriptorUtil.declaresOrInheritsDefaultValue
import com.linqingying.cangjie.resolve.descriptorUtil.module
import com.linqingying.cangjie.resolve.scopes.receivers.ImplicitClassReceiver
import com.linqingying.cangjie.types.util.isSubtypeOf
import com.linqingying.cangjie.utils.OperatorNameConventions.GET
import com.linqingying.cangjie.utils.OperatorNameConventions.SET


interface Check {
    val description: String
    fun check(functionDescriptor: FunctionDescriptor): Boolean
    operator fun invoke(functionDescriptor: FunctionDescriptor): String? =
        if (!check(functionDescriptor)) description else null
}

internal class Checks private constructor(
    val name: Name?,
    val regex: Regex?,
    val nameList: Collection<Name>?,
    val additionalCheck: (FunctionDescriptor) -> String?,
    vararg val checks: Check
) {
    fun isApplicable(functionDescriptor: FunctionDescriptor): Boolean {
        if (name != null && functionDescriptor.name != name) return false
        if (regex != null && !functionDescriptor.name.asString().matches(regex)) return false
        if (nameList != null && functionDescriptor.name !in nameList) return false
        return true
    }

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

    fun check(functionDescriptor: FunctionDescriptor): CheckResult {
        for (check in checks) {
            if (!check.isApplicable(functionDescriptor)) continue
            return check.checkAll(functionDescriptor)
        }

        return CheckResult.IllegalFunctionName
    }
}

sealed class MemberKindCheck(override val description: String) : Check {
    data object MemberOrExtension : MemberKindCheck("must be a member or an extension function") {
        override fun check(functionDescriptor: FunctionDescriptor) =
            functionDescriptor.dispatchReceiverParameter != null || functionDescriptor.extensionReceiverParameter != null
    }

    data object Member : MemberKindCheck("must be a member function") {
        override fun check(functionDescriptor: FunctionDescriptor) =
            functionDescriptor.dispatchReceiverParameter != null
    }
}

sealed class ValueParameterCountCheck(override val description: String) : Check {
    data object NoValueParameters : ValueParameterCountCheck("must have no value parameters") {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.isEmpty()
    }

    data object SingleValueParameter : ValueParameterCountCheck("must have a single value parameter") {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size == 1
    }

    object NamedAndValue : ValueParameterCountCheck("can only have one named parameter 'value'") {
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

    class AtLeast(val n: Int) :
        ValueParameterCountCheck("must have at least $n value parameter" + (if (n > 1) "s" else "")) {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size >= n
    }

    class Equals(val n: Int) : ValueParameterCountCheck("must have exactly $n value parameters") {
        override fun check(functionDescriptor: FunctionDescriptor) = functionDescriptor.valueParameters.size == n
    }
}

object OperatorChecks : AbstractModifierChecks() {
    override val checks by lazy {
        listOf(
            Checks(
                GET, MemberKindCheck.MemberOrExtension, ValueParameterCountCheck.AtLeast(1),
                ValueParameterCountCheck.NamedAndValue
            ),


            Checks(
                SET,
                MemberKindCheck.MemberOrExtension,
                ValueParameterCountCheck.NamedAndValue,
                ValueParameterCountCheck.AtLeast(2)
            ) {
                val lastIsOk =
                    valueParameters.lastOrNull()
                        ?.let { !it.declaresOrInheritsDefaultValue() && it.varargElementType == null } == true
                ensure(lastIsOk) { "last parameter should not have a default value or be a vararg" }

            }

        )


    }


    private fun FunctionDescriptor.incDecCheckForExpectClass(receiver: ReceiverParameterDescriptor): Boolean {
        val receiverValue = receiver.value
        if (receiverValue !is ImplicitClassReceiver) return false

        val classDescriptor = receiverValue.classDescriptor
        if (!classDescriptor.isExpect) return false

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
