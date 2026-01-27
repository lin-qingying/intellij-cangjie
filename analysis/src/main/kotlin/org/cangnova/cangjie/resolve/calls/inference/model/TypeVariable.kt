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

package org.cangnova.cangjie.resolve.calls.inference.model

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.resolve.builtIns

import org.cangnova.cangjie.resolve.calls.model.PostponableCangJieCallArgument
import org.cangnova.cangjie.resolve.descriptorUtil.hasOnlyInputTypesAnnotation
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.checker.TypeVariableConstructor
import org.cangnova.cangjie.types.model.TypeVariableMarker
import org.cangnova.cangjie.types.model.TypeVariableTypeConstructorMarker
import kotlin.collections.emptyList

/**
 * 类型变量的类型构造器
 *
 * 用于在类型推断过程中表示未确定的类型变量。这个类实现了类型构造器接口，
 * 但与普通的类型构造器不同，它不代表一个具体的类型，而是代表一个待推断的类型变量。
 *
 * @property builtIns 仓颉语言的内置类型系统
 * @property debugName 用于调试的类型变量名称
 * @property originalTypeParameter 原始的类型参数描述符，如果这个类型变量来源于某个类型参数
 */
class TypeVariableTypeConstructor(
    override val builtIns: CangJieBuiltIns,
    val debugName: String,
    override val originalTypeParameter: TypeParameterDescriptor?
) : TypeVariableConstructor, TypeVariableTypeConstructorMarker {

    /**
     * 父类型集合
     * 类型变量在推断过程中没有预定义的父类型，返回空列表
     */
    override val supertypes: Collection<CangJieType>
        get() = emptyList()

    /**
     * 类型参数列表
     * 类型变量本身不接受类型参数，返回空列表
     */
    override val parameters: List<TypeParameterDescriptor>
        get() = emptyList()

    /**
     * 是否为 final 类型
     * 类型变量不是 final 的，因为它可以被推断为任何类型
     */
    override val isFinal: Boolean
        get() = false

    /**
     * 是否可表示
     * 类型变量在源代码中不能直接表示，只存在于类型推断的内部过程中
     */
    override val isDenotable: Boolean
        get() = false

    /**
     * 声明描述符
     * 类型变量没有对应的声明，返回 null
     */
    override val declarationDescriptor: ClassifierDescriptor?
        get() = null

    /**
     * 类型精化
     * 类型变量不需要精化，直接返回自身
     *
     * @param cangjieTypeRefiner 类型精化器
     * @return 返回自身
     */
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this

    /**
     * 字符串表示
     * 返回类型变量的调试名称
     */
    override fun toString() = "TypeVariable($debugName)"

    /**
     * 标记类型变量是否出现在不变或逆变位置
     *
     * 这个标志用于类型推断中的方差检查。如果类型变量出现在不变或逆变位置，
     * 那么它的推断会受到更多限制。
     */
    var isContainedInInvariantOrContravariantPositions: Boolean = false
}

/**
 * 为类型构造器创建类型变量的简单类型
 *
 * 这个扩展函数将一个类型变量构造器转换为实际可用的简单类型对象。
 * 它使用内置的 any 类型的成员作用域作为类型变量的成员作用域。
 *
 * @receiver 必须是 TypeVariableTypeConstructor 类型的类型构造器
 * @return 返回对应的简单类型
 * @throws IllegalArgumentException 如果接收者不是 TypeVariableTypeConstructor 类型
 */
fun TypeConstructor.typeForTypeVariable(): SimpleType {
    require(this is TypeVariableTypeConstructor)
    return CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
        TypeAttributes.Empty, this, emptyList(),
        builtIns.stdlibTypes.any.unsubstitutedMemberScope
    )
}

/**
 * 从可调用描述符创建的类型变量
 *
 * 当函数或方法的类型参数需要在调用时进行类型推断时，会为每个类型参数创建这样的类型变量。
 * 例如：func  foo<T>(x: T): T，在调用 foo 时会为 T 创建一个类型变量。
 *
 * @property originalTypeParameter 原始的类型参数描述符
 */
class TypeVariableFromCallableDescriptor(
    val originalTypeParameter: TypeParameterDescriptor
) : NewTypeVariable(
    originalTypeParameter.builtIns,
    SpecialNames.safeIdentifier(originalTypeParameter.name).identifier,
    originalTypeParameter
) {
    /**
     * 检查原始类型参数是否只有输入类型注解
     *
     * @return 如果类型参数标记为只能用于输入位置则返回 true
     */
    override fun hasOnlyInputTypesAnnotation(): Boolean = originalTypeParameter.hasOnlyInputTypesAnnotation()
}

/**
 * 新类型变量的抽象基类
 *
 * 这是所有类型变量的基类，封装了类型推断过程中需要的类型变量的通用行为。
 * 类型变量是类型推断算法的核心概念，用于表示尚未确定的类型。
 *
 * @param builtIns 仓颉语言的内置类型系统
 * @param name 类型变量的名称
 * @param originalTypeParameter 可选的原始类型参数，如果类型变量对应某个类型参数
 */
sealed class NewTypeVariable(
    builtIns: CangJieBuiltIns,
    name: String,
    originalTypeParameter: TypeParameterDescriptor? = null
) : TypeVariableMarker {

    /**
     * 新鲜的类型构造器
     *
     * "fresh" 表示这是一个全新创建的、未被使用过的类型变量构造器，
     * 保证在类型推断过程中不会与其他类型变量混淆。
     */
    val freshTypeConstructor = TypeVariableTypeConstructor(builtIns, name, originalTypeParameter)

    /**
     * 默认类型
     *
     * 如果接收者的类型是 TypeVariable(T)，则使用成员作用域。
     * TODO: 需要将类型变量的父类型的方法添加到成员作用域中
     */
    val defaultType: SimpleType = freshTypeConstructor.typeForTypeVariable()

    /**
     * 检查是否只有输入类型注解
     *
     * 输入类型注解（input types annotation）表示类型变量只能用于输入位置（逆变位置），
     * 这会影响类型推断的方差分析。
     *
     * @return 如果类型变量标记为只能用于输入位置则返回 true
     */
    abstract fun hasOnlyInputTypesAnnotation(): Boolean

    /**
     * 字符串表示
     * 返回类型构造器的字符串表示
     */
    override fun toString() = freshTypeConstructor.toString()
}

/**
 * 可调用引用参数类型的类型变量
 *
 * 用于推断可调用引用（如函数引用、属性引用）的参数类型。
 * 例如：val ref = ::someFunction，需要推断 someFunction 的参数类型。
 *
 * @param builtIns 仓颉语言的内置类型系统
 * @param name 类型变量的名称
 */
class TypeVariableForCallableReferenceParameterType(
    builtIns: CangJieBuiltIns,
    name: String
) : NewTypeVariable(builtIns, name) {
    /**
     * 可调用引用的参数类型通常不受输入类型注解的限制
     */
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}

/**
 * 可调用引用返回类型的类型变量
 *
 * 用于推断可调用引用的返回类型。
 * 例如：val ref: (Int) -> ? = ::someFunction，需要推断返回类型。
 *
 * @param builtIns 仓颉语言的内置类型系统
 * @param name 类型变量的名称
 */
class TypeVariableForCallableReferenceReturnType(
    builtIns: CangJieBuiltIns,
    name: String
) : NewTypeVariable(builtIns, name) {
    /**
     * 可调用引用的返回类型通常不受输入类型注解的限制
     */
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}

/**
 * Lambda 表达式返回类型的类型变量
 *
 * 用于推断 Lambda 表达式的返回类型。
 * 例如：val lambda = { x: Int -> ... }，需要推断 lambda 的返回类型。
 *
 * @param builtIns 仓颉语言的内置类型系统
 * @param name 类型变量的名称
 */
class TypeVariableForLambdaReturnType(
    builtIns: CangJieBuiltIns,
    name: String
) : NewTypeVariable(builtIns, name) {
    /**
     * Lambda 返回类型通常不受输入类型注解的限制
     */
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}

/**
 * Lambda 表达式参数类型的类型变量
 *
 * 用于推断 Lambda 表达式的参数类型。
 * 例如：list.map { it * 2 }，需要推断 it 的类型。
 *
 * @property atom 可延迟的仓颉调用参数，表示这个 Lambda 表达式在调用中的位置
 * @property index Lambda 参数的索引位置（第几个参数）
 * @param builtIns 仓颉语言的内置类型系统
 * @param name 类型变量的名称
 */
class TypeVariableForLambdaParameterType(
    val atom: PostponableCangJieCallArgument,
    val index: Int,
    builtIns: CangJieBuiltIns,
    name: String
) : NewTypeVariable(builtIns, name) {
    /**
     * Lambda 参数类型通常不受输入类型注解的限制
     */
    override fun hasOnlyInputTypesAnnotation(): Boolean = false
}