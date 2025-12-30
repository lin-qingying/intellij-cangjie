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

import com.intellij.util.SmartList
import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.CallableMemberDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DescriptorToSourceUtils
import org.cangnova.cangjie.descriptors.FunctionDescriptor
import org.cangnova.cangjie.descriptors.MemberDescriptor
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.descriptors.VariableDescriptor
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.psiUtil.unpackFunctionLiteral
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.getSuperClassNotAny
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.DeferredType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.contains


/**
 * 获取类的所有超类（不包括 Any）
 *
 * 从当前类开始，沿继承链向上遍历所有超类，直到没有超类或只剩 Any 为止。
 * 结果以 SmartList 返回，保持继承顺序（最近的超类在前）。
 *
 * @receiver ClassDescriptor 要查询的类描述符
 * @return SmartList<ClassDescriptor> 所有超类的列表（不包括 Any）
 */
fun ClassDescriptor.getAllSuperclassesWithoutAny() =
    generateSequence(
        getSuperClassNotAny(),
        ClassDescriptor::getSuperClassNotAny
    ).toCollection(SmartList<ClassDescriptor>())

/**
 * 判断变量是否使用下划线命名
 *
 * 仓颉语言中，下划线 `_` 用于表示忽略的变量（类似 Kotlin），
 * 例如在解构赋值或模式匹配中忽略某些值。
 *
 * @receiver VariableDescriptor 变量描述符
 * @return Boolean true 表示变量名为 "_"
 */
val VariableDescriptor.isUnderscoreNamed
    get() = !name.isSpecial && name.identifier == "_"

/**
 * 查找第一个满足条件的被重写成员
 *
 * 使用深度优先搜索遍历重写链，找到第一个满足谓词条件的成员。
 * 重写链的遍历顺序：当前成员 -> 直接重写的成员 -> 间接重写的成员...
 *
 * @receiver CallableMemberDescriptor 起始成员描述符
 * @param useOriginal 是否使用原始描述符（去除替换后的类型参数）
 * @param predicate 判断成员是否满足条件的谓词函数
 * @return CallableMemberDescriptor? 第一个满足条件的成员，未找到则返回 null
 */
fun CallableMemberDescriptor.firstOverridden(
    useOriginal: Boolean = false,
    predicate: (CallableMemberDescriptor) -> Boolean
): CallableMemberDescriptor? {
    var result: CallableMemberDescriptor? = null
    return DFS.dfs(listOf(this),
        { current ->
            val descriptor = if (useOriginal) current?.original else current
            (descriptor?.overriddenDescriptors ?: emptyList()) as MutableIterable<CallableMemberDescriptor>
        },
        object : DFS.AbstractNodeHandler<CallableMemberDescriptor, CallableMemberDescriptor?>() {
            override fun beforeChildren(current: CallableMemberDescriptor) = result == null
            override fun afterChildren(current: CallableMemberDescriptor) {
                if (result == null && predicate(current)) {
                    result = current
                }
            }

            override fun result(): CallableMemberDescriptor? = result
        }
    )
}

/**
 * 在声明对应的 PSI 元素上报告诊断信息
 *
 * 根据描述符找到对应的 PSI 声明节点，然后调用 what 函数生成诊断信息并报告。
 * 如果 PSI 元素类型不匹配或找不到声明，会抛出 AssertionError。
 *
 * @param T PSI 声明类型（如 CjNamedFunction、CjClass 等）
 * @param trace 绑定跟踪器，用于记录诊断信息
 * @param descriptor 声明的描述符
 * @param what 根据 PSI 元素生成诊断信息的函数
 * @throws AssertionError 如果找不到声明或声明类型不匹配
 */
inline fun <reified T : CjDeclaration> reportOnDeclarationAs(
    trace: BindingTrace,
    descriptor: DeclarationDescriptor,
    what: (T) -> Diagnostic
) {
    DescriptorToSourceUtils.descriptorToDeclaration(descriptor)?.let { psiElement ->
        (psiElement as? T)?.let {
            trace.report(what(it))
        }
            ?: throw AssertionError("Declaration for $descriptor is expected to be ${T::class.simpleName}, actual declaration: $psiElement")
    } ?: throw AssertionError("No declaration for $descriptor")
}

/**
 * 判断函数是否适用于类型转换特性的期望类型推断
 *
 * 检查函数是否满足以下条件：
 * 1. 有且仅有一个类型参数
 * 2. 返回类型正是该类型参数
 * 3. 参数类型中不包含该类型参数（避免类型参数在输入输出都出现）
 *
 * 这种模式用于类型转换辅助函数，例如 `fun <T> asType(): T`。
 *
 * @receiver FunctionDescriptor 函数描述符
 * @return Boolean true 表示函数符合期望类型推断的模式
 */
fun FunctionDescriptor.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameter = typeParameters.singleOrNull() ?: return false

    val returnType = returnType ?: return false
    if (returnType is DeferredType && returnType.isComputing) return false

    if (returnType.constructor != typeParameter.typeConstructor) return false

    fun CangJieType.isBadType() = contains { it.constructor == typeParameter.typeConstructor }

    return !(valueParameters.any { it.type.isBadType() }  )
}

/**
 * 判断声明是否在 extend 中定义
 *
 * 仓颉语言的 extend 可以为类型添加新的成员函数和属性，
 * 但有特殊限制：
 * - 不能覆盖原始类型的声明
 * - 不能访问原始类型的 private 成员
 */
val DeclarationDescriptor.isExtension: Boolean
    get() {
        // 判断成员的容器是否是 ExtendDescriptor
        return containingDeclaration is org.cangnova.cangjie.descriptors.extend.ExtendDescriptor
    }

/**
 * 获取类型构造器的所有超类型（包括 Any）
 *
 * 如果类型没有明确的超类（所有超类型都是接口），
 * 则自动添加 Any 作为超类型，以确保类型系统的完整性。
 *
 * @receiver TypeConstructor 类型构造器
 * @return Collection<CangJieType> 所有超类型的集合（如有必要会包含 Any）
 */
fun TypeConstructor.supertypesWithAny(): Collection<CangJieType> {
    val supertypes = supertypes
    val noSuperClass = supertypes.map { it.constructor.declarationDescriptor as? ClassDescriptor }.all {
        it == null || it.kind == ClassKind.INTERFACE
    }
    return if (noSuperClass) supertypes + builtIns.stdlibTypes.anyType else supertypes
}

/**
 * 获取枚举类型的类值类型
 *
 * 枚举类型的构造器可以作为值直接访问（如 Color.Red），
 * 这个属性返回枚举类本身的类型，用于类型推导和检查。
 *
 * 对于非枚举类型，返回 null。
 *
 * @receiver ClassDescriptor 类描述符
 * @return CangJieType? 枚举类的默认类型，非枚举类返回 null
 */
val ClassDescriptor.enumClassValueType: CangJieType?
    get() = if (kind == ClassKind.ENUM) defaultType else null

/**
 * 获取调用表达式的最后一个 lambda 参数
 *
 * 仓颉语言支持尾随 lambda 语法，lambda 可以作为函数调用的最后一个参数。
 * 此函数检查调用表达式的最后一个值参数是否为 lambda 表达式。
 *
 * 注意：如果调用中已经有 lambda 参数（通过 lambda 参数语法），则返回 null。
 *
 * @receiver CjCallExpression 调用表达式
 * @return CjLambdaExpression? 最后一个 lambda 表达式，如果不存在则返回 null
 */
fun CjCallExpression.getLastLambdaExpression(): CjLambdaExpression? {
    if (lambdaArguments.isNotEmpty()) return null
    return valueArguments.lastOrNull()?.getArgumentExpression()?.unpackFunctionLiteral()
}
