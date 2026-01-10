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
import org.cangnova.cangjie.utils.DFS

/**
 * 判断值参数是否为可变参数（vararg）
 *
 * 可变参数允许函数接受可变数量的参数，在仓颉语言中类似于其他语言的 varargs。
 * 通过检查参数是否有 vararg 元素类型来判断。
 *
 * @return 如果是可变参数则返回 true，否则返回 false
 */
val ValueParameterDescriptor.isVararg: Boolean get() = varargElementType != null

/**
 * 判断调用参数是否为数组类型
 *
 * 此函数用于确定传递给函数的参数是否为数组类型。
 * 这在处理可变参数时特别重要，因为数组可以直接展开为可变参数。
 *
 * 检查条件：
 * 1. 参数必须是接收者类型的参数
 * 2. 接收者必须包含智能转换信息
 * 3. 接收者的类型必须是数组类型
 *
 * @return 如果参数是数组类型则返回 true，否则返回 false
 */
fun CangJieCallArgument.isArrayType(): Boolean {
    if (this !is ReceiverCangJieCallArgument) return false

    if (receiver !is ReceiverValueWithSmartCastInfo) return false

    return CangJieBuiltIns.isArray((receiver as ReceiverValueWithSmartCastInfo).receiverValue.type)


}

/**
 * 获取调用参数的期望类型
 *
 * 根据参数的特性（是否为展开参数、是否为数组、是否为可变参数）确定参数的期望类型。
 * 这对于类型检查和类型推断至关重要。
 *
 * 类型确定逻辑：
 * 1. 如果参数使用了展开操作符（*），返回参数的完整类型
 * 2. 如果参数是数组且对应可变参数，返回参数的完整类型
 * 3. 如果参数对应可变参数但不是数组，返回可变参数的元素类型
 * 4. 否则返回参数的声明类型
 *
 * @param parameter 函数的形式参数描述符
 * @param languageVersionSettings 语言版本设置（用于版本相关的行为）
 * @return 参数的期望类型（已解包）
 */
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
 * 判断值参数是否有默认值
 *
 * 通过深度优先搜索（DFS）遍历参数的重写链，检查参数是否具有默认值。
 * 参数可以通过以下方式获得默认值：
 * 1. 直接声明了默认值
 * 2. 通过重写具有默认值的参数而继承默认值
 * 3. 作为 'actual' 声明的参数，其对应的 'expect' 参数具有默认值
 *
 * 使用 DFS 遍历重写链确保能够找到所有可能的默认值来源。
 *
 * @return 如果参数有默认值（直接或通过继承）则返回 true，否则返回 false
 */
fun ValueParameterDescriptor.hasDefaultValue(): Boolean {
    return DFS.ifAny(
        listOf(this),
        { current -> current.overriddenDescriptors.map(ValueParameterDescriptor::original) },
        { it.declaresDefaultValue || it.isActualParameterWithCorrespondingExpectedDefault }
    )
}

/**
 * 判断参数是否为 actual 参数且对应的 expect 参数有默认值
 *
 * 在多平台项目中，actual 声明实现 expect 声明。
 * 此属性检查当前参数是否为 actual 参数，并且其对应的 expect 参数声明了默认值。
 *
 * @see isActualParameterWithAnyExpectedDefault
 */
val ValueParameterDescriptor.isActualParameterWithCorrespondingExpectedDefault: Boolean
    get() = checkExpectedParameter { it.declaresDefaultValue }

/**
 * 检查期望参数（expect parameter）
 *
 * 用于在多平台项目中检查 actual 参数对应的 expect 参数是否满足某个条件。
 * 目前实现返回 false，因为多平台支持尚未完全实现。
 *
 * @param checker 用于检查期望参数的谓词函数
 * @return 目前始终返回 false（多平台功能待实现）
 */
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
        return intersectionType
    }


/**
 * 抛出意外参数类型错误
 *
 * 当遇到不支持或未预期的参数类型时，使用此函数抛出错误。
 * 这是一个内部工具函数，用于在参数处理逻辑中发现不应该出现的参数类型时报告错误。
 *
 * @param argument 意外的调用参数
 * @throws IllegalStateException 始终抛出，包含参数类型信息
 * @return Nothing 此函数永不返回（总是抛出异常）
 */
internal fun unexpectedArgument(argument: CangJieCallArgument): Nothing =
    error("Unexpected argument type: $argument, ${argument.javaClass.canonicalName}.")

/**
 * 获取接收者的不稳定类型
 *
 * 与 [stableType] 相对，此属性返回不稳定接收者的类型，或在接收者稳定时返回 null。
 * 不稳定的接收者是指其值在使用点可能发生变化的接收者（如可变变量）。
 *
 * 类型确定逻辑：
 * 1. 如果接收者稳定，返回 null（稳定接收者没有不稳定类型）
 * 2. 如果接收者没有智能转换类型，返回原始接收者类型
 * 3. 如果接收者不稳定但有智能转换，返回所有原始类型的交集并处理捕获类型
 *
 * @return 不稳定接收者的类型，或稳定接收者的 null
 */
internal val ReceiverValueWithSmartCastInfo.unstableType: UnwrappedType?
    get() {
        if (isStable || !hasTypesFromSmartCasts())
            return if (isStable) null else receiverValue.type.unwrap()

        val intersectionType = intersectWrappedTypes(allOriginalTypes)
        return intersectionType
    }
