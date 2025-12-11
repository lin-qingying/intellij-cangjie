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

package org.cangnova.cangjie.resolve.calls.components

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ParameterDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.resolve.calls.model.CangJieCallArgument
import org.cangnova.cangjie.resolve.calls.model.ReceiverCangJieCallArgument
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.cangnova.cangjie.types.UnwrappedType
import org.cangnova.cangjie.types.checker.intersectWrappedTypes
import org.cangnova.cangjie.types.checker.prepareArgumentTypeRegardingCaptureTypes
import org.cangnova.cangjie.utils.DFS

val ValueParameterDescriptor.isVararg: Boolean get() = varargElementType != null
fun CangJieCallArgument.isArrayType(): Boolean {
    if (this !is ReceiverCangJieCallArgument) return false

    if (receiver !is ReceiverValueWithSmartCastInfo) return false

    return CangJieBuiltIns.isArray((receiver as ReceiverValueWithSmartCastInfo).receiverValue.type)


}

internal fun CangJieCallArgument.getExpectedType(
    parameter: ParameterDescriptor,
    languageVersionSettings: LanguageVersionSettings
) =
    if (
        this.isSpread /*||
        this.isArrayAssignedAsNamedArgumentInAnnotation(parameter, languageVersionSettings) ||
        this.isArrayAssignedAsNamedArgumentInFunction(parameter, languageVersionSettings)*/
    ) {
        parameter.type.unwrap()
    } else {
        val varargType = (parameter as? ValueParameterDescriptor)?.varargElementType?.unwrap()
        if (isArrayType() && varargType != null) {
            parameter.type.unwrap()

        } else {
            varargType ?: parameter.type.unwrap()

        }

    }

/**
 * @return `true` iff the parameter has a default value, i.e. declares it, inherits it by overriding a parameter which has a default value,
 * or is a parameter of an 'actual' declaration, such that the corresponding 'expect' parameter has a default value.
 */
fun ValueParameterDescriptor.hasDefaultValue(): Boolean {
    return DFS.ifAny(
        listOf(this),
        { current -> current.overriddenDescriptors.map(ValueParameterDescriptor::original) },
        { it.declaresDefaultValue() || it.isActualParameterWithCorrespondingExpectedDefault }
    )
}

/**
 * @see isActualParameterWithAnyExpectedDefault
 */
val ValueParameterDescriptor.isActualParameterWithCorrespondingExpectedDefault: Boolean
    get() = checkExpectedParameter { it.declaresDefaultValue() }

private fun ValueParameterDescriptor.checkExpectedParameter(checker: (ValueParameterDescriptor) -> Boolean): Boolean {
//    val function = containingDeclaration
//    if (function is FunctionDescriptor && function.isActual) {
//        val expected = function.findCompatibleExpectsForActual().firstOrNull()
//        return expected is FunctionDescriptor && checker(expected.valueParameters[index])
//    }
    return false
}

/**
 * 获取一个稳定类型的属性，该属性考虑了所有可能的智能转换。
 *
 * @return 返回一个解包后的类型 [UnwrappedType]，该类型被认为是稳定的，可以用于进一步的类型检查或转换。
 */
val ReceiverValueWithSmartCastInfo.stableType: UnwrappedType
    get() {
        // 如果当前接收者值不稳定或没有从智能转换中获取类型，则直接返回原始类型
        if (!isStable || !hasTypesFromSmartCasts())
            return receiverValue.type.unwrap()

        /*
         * 必须首先进行类型交集操作，因为在捕获后，子类型关系可能会发生变化，某些类型不会被排除在交集类型之外。
         *
         * 示例：
         *      allOriginalTypes = [Inv<out CharSequence>, Inv<String>]
         *      intersect(Inv<out CharSequence>, Inv<String>) = Inv<String>
         *      capture(Inv<String>) = Inv<String>
         * 但如果先进行捕获：
         *      capture(Inv<out CharSequence>) = Inv<CapturedType(out CharSequence)>
         *      capture(Inv<String>) = Inv<String>
         *      intersect(Inv<CapturedType(out CharSequence)>, Inv<String>) = Inv<CapturedType(out CharSequence)> & Inv<String>
         *
         * 这样的冗余类型可能会导致约束系统中的矛盾或不精确的解决方案。
         */
        val intersectionType = intersectWrappedTypes(allOriginalTypes)

        // 准备并返回考虑捕获类型的参数类型，如果没有则返回交集类型
        return prepareArgumentTypeRegardingCaptureTypes(intersectionType) ?: intersectionType
    }


internal fun unexpectedArgument(argument: CangJieCallArgument): Nothing =
    error("Unexpected argument type: $argument, ${argument.javaClass.canonicalName}.")

internal val ReceiverValueWithSmartCastInfo.unstableType: UnwrappedType?
    get() {
        if (isStable || !hasTypesFromSmartCasts())
            return if (isStable) null else receiverValue.type.unwrap()

        val intersectionType = intersectWrappedTypes(allOriginalTypes)

        return prepareArgumentTypeRegardingCaptureTypes(intersectionType) ?: intersectionType
    }
