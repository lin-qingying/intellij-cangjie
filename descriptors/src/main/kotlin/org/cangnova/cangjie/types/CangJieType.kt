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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.descriptors.annotations.Annotated
import org.cangnova.cangjie.descriptors.impl.FunctionClassDescriptor
import org.cangnova.cangjie.descriptors.impl.TupleClassDescriptor
import org.cangnova.cangjie.renderer.DescriptorRenderer
import org.cangnova.cangjie.renderer.DescriptorRendererOptions
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.error.ErrorType
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.FlexibleTypeMarker
import org.cangnova.cangjie.types.model.SimpleTypeMarker
import org.cangnova.cangjie.types.model.TypeArgumentListMarker

/**
 * 仓颉语言类型系统核心组件
 *
 * 本文件定义了仓颉语言IDE插件的类型系统，包括：
 * - Option类型处理（isOption属性）
 * - 基础类型（BasicType）
 * - Option类型（OptionType）
 * - 灵活类型（FlexibleType）
 * - 类型转换和操作
 *
 * 核心概念：
 * - isOption：表示类型是否为Option类型（如 ?Int、Option<Int>）
 * - 语法糖：?Int 等价于 Option<Int>
 * - 类型解包：从Option类型中提取内部类型
 * - 嵌套层级：计算Option的嵌套深度（如 Option<Option<Int>>）
 */

/**
 * 检查类型是否为错误类型
 *
 * 示例：
 * ```kotlin
 * val errorType: CangJieType = ErrorType(...)
 * errorType.isError // true
 *
 * val normalType: CangJieType = BasicType(...)
 * normalType.isError // false
 * ```
 */
val CangJieType.isError: Boolean
    get() = unwrap().let { unwrapped ->
        unwrapped is ErrorType
//                ||
//                (unwrapped is FlexibleType && unwrapped.delegate is ErrorType)
    }

/**
 * 子类型关系表示接口
 *
 * 用于表示类型之间的子类型关系，包含：
 * - subTypeRepresentative：子类型代表
 * - superTypeRepresentative：超类型代表
 * - sameTypeConstructor：检查是否具有相同的类型构造器
 *
 * 示例：
 * ```kotlin
 * // 对于 FlexibleType(Int, Number)
 * subTypeRepresentative = Int
 * superTypeRepresentative = Number
 * sameTypeConstructor(Int) = true
 * sameTypeConstructor(String) = false
 * ```
 */
interface SubtypingRepresentatives {
    val subTypeRepresentative: CangJieType
    val superTypeRepresentative: CangJieType

    fun sameTypeConstructor(type: CangJieType): Boolean
}

/**
 * 仓颉类型基类
 *
 * 所有仓颉语言类型的基类，定义了类型系统的核心功能：
 *
 * 核心属性：
 * - isOption：是否为Option类型（如 ?Int、Option<Int>）
 * - constructor：类型构造器
 * - arguments：类型参数列表
 * - attributes：类型属性
 * - memberScope：成员作用域
 *
 * 核心方法：
 * - unwrap()：解包类型，返回未包装的类型
 * - refine()：类型精化，用于类型推断和替换
 *
 * 示例：
 * ```kotlin
 * // Int 类型
 * val intType: CangJieType = BasicType(...)
 * intType.isOption // false
 * intType.unwrap() // 返回 UnwrappedType
 * intType.refine(typeRefiner) // 返回精化后的类型
 *
 * // ?Int 类型
 * val optionIntType: CangJieType = OptionType(intType)
 * optionIntType.isOption // true
 * optionIntType.unwrap() // 返回 UnwrappedType
 * optionIntType.refine(typeRefiner) // 返回精化后的类型
 * ```
 */
sealed class CangJieType : Annotated, CangJieTypeMarker {
    /**
     * 解包类型
     *
     * 将包装类型解包为未包装类型。对于简单类型，返回自身；
     * 对于包装类型，返回其内部未包装的类型。
     *
     * 示例：
     * ```kotlin
     * val unwrappedType = someType.unwrap() // 返回 UnwrappedType
     * ```
     *
     * @return 未包装的类型
     */
    abstract fun unwrap(): UnwrappedType

    /**
     * 类型构造器
     *
     * 表示类型的构造器，用于创建该类型的实例。
     * 每个类型都有对应的构造器。
     *
     * 示例：
     * ```kotlin
     * val constructor = type.constructor // 获取类型构造器
     * ```
     */
    abstract val constructor: TypeConstructor

    /**
     * 类型参数列表
     *
     * 泛型类型的参数列表。对于非泛型类型，返回空列表。
     *
     * 示例：
     * ```kotlin
     * // List<Int> 类型
     * val listType: CangJieType = ...
     * listType.arguments // [TypeArgument(Int)]
     *
     * // Int 类型
     * val intType: CangJieType = BasicType(...)
     * intType.arguments // []
     * ```
     */
    abstract val arguments: List<TypeArgument>

    /**
     * 类型属性
     *
     * 类型的附加属性，如注解、修饰符等。
     *
     * 示例：
     * ```kotlin
     * val attributes = type.attributes // 获取类型属性
     * ```
     */
    abstract val attributes: TypeAttributes



    /**
     * 成员作用域
     *
     * 该类型可访问的成员（方法、属性等）的作用域。
     *
     * 示例：
     * ```kotlin
     * val memberScope = type.memberScope // 获取成员作用域
     * ```
     */
    abstract val memberScope: MemberScope

    /**
     * 类型精化
     *
     * 使用类型精化器对类型进行精化，通常用于类型推断和类型替换。
     * 这是一个抽象方法，具体实现由子类提供。
     *
     * 示例：
     * ```kotlin
     * val refinedType = type.refine(typeRefiner) // 精化类型
     * ```
     *
     * @param cangjieTypeRefiner 类型精化器
     * @return 精化后的类型
     */

    abstract fun refine(cangjieTypeRefiner: CangJieTypeRefiner): CangJieType
}
val CangJieType.isOption get() = OptionTypeUtils.isOptionType(this)

/**
 * 未包装类型基类
 *
 * 表示已经解包的类型，是类型系统的基础组件。
 * 所有具体的类型实现都继承自此类。
 *
 * 核心方法：
 * - replaceAttributes()：替换类型属性
 * - makeOptionAsSpecified()：转换为指定的Option状态
 *
 * 示例：
 * ```kotlin
 * val unwrappedType: UnwrappedType = BasicType(...)
 * unwrappedType.unwrap() // 返回自身
 * unwrappedType.replaceAttributes(newAttributes) // 替换属性
 * unwrappedType.makeOptionAsSpecified(true) // 转换为Option类型
 * ```
 */
sealed class UnwrappedType : CangJieType() {
    /**
     * 解包类型（最终实现）
     *
     * 对于未包装类型，直接返回自身。
     *
     * 示例：
     * ```kotlin
     * val unwrappedType: UnwrappedType = BasicType(...)
     * unwrappedType.unwrap() // 返回自身
     * ```
     *
     * @return 自身
     */
    final override fun unwrap(): UnwrappedType = this

    /**
     * 替换类型属性
     *
     * 创建具有新属性的类型副本，保持其他属性不变。
     *
     * 示例：
     * ```kotlin
     * val newAttributes = TypeAttributes(...)
     * val newType = type.replaceAttributes(newAttributes) // 替换属性
     * ```
     *
     * @param newAttributes 新的类型属性
     * @return 具有新属性的类型
     */
    abstract fun replaceAttributes(newAttributes: TypeAttributes): UnwrappedType

    /**
     * 类型精化（未包装类型）
     *
     * 对未包装类型进行精化，返回精化后的未包装类型。
     *
     * 示例：
     * ```kotlin
     * val refinedType = type.refine(typeRefiner) // 精化类型
     * ```
     *
     * @param cangjieTypeRefiner 类型精化器
     * @return 精化后的未包装类型
     */

    abstract override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): UnwrappedType


}

/**
 * 基础类型
 *
 * 表示仓颉语言中的基础类型，如 Int、String、Bool 等。
 * 这些类型不是Option类型，isOption = false。
 *
 * 特点：
 * - 没有类型参数（arguments为空）
 * - 没有特殊属性（attributes为空）
 * - 不是Option类型（isOption = false）
 * - 支持相等性比较和哈希计算
 *
 * 示例：
 * ```kotlin
 * // Int 类型
 * val intType = BasicType(intConstructor, intMemberScope)
 * intType.isOption // false
 * intType.toString() // "Int"
 * intType.unwrapOption() // 返回 Int 类型
 * intType.countOptionNestedLevel() // 0
 *
 * // String 类型
 * val stringType = BasicType(stringConstructor, stringMemberScope)
 * stringType.isOption // false
 * stringType.toString() // "String"
 * ```
 */
class BasicType(
    override val constructor: TypeConstructor,
    override val memberScope: MemberScope
) : SimpleType() {

    /**
     * 类型参数列表（基础类型）
     *
     * 基础类型没有类型参数，返回空列表。
     *
     * 示例：
     * ```kotlin
     * val intType = BasicType(...)
     * intType.arguments // []
     * ```
     */
    override val arguments: List<TypeArgument> get() = listOf()

    /**
     * 类型属性（基础类型）
     *
     * 基础类型没有特殊属性，返回空属性。
     *
     * 示例：
     * ```kotlin
     * val intType = BasicType(...)
     * intType.attributes // TypeAttributes.Empty
     * ```
     */
    override val attributes: TypeAttributes get() = TypeAttributes.Empty



    /**
     * 相等性比较
     *
     * 比较两个基础类型是否相等。基于类型名称进行比较。
     *
     * 示例：
     * ```kotlin
     * val intType1 = BasicType(intConstructor, intMemberScope)
     * val intType2 = BasicType(intConstructor, intMemberScope)
     * intType1 == intType2 // true
     *
     * val stringType = BasicType(stringConstructor, stringMemberScope)
     * intType1 == stringType // false
     * ```
     *
     * @param other 要比较的对象
     * @return 是否相等
     */
    override fun equals(other: Any?): Boolean {
        if (other !is BasicType) return false
        if (this === other) return true
        return typeName == other.typeName
    }

    /**
     * 类型名称
     *
     * 获取基础类型的名称，用于显示和比较。
     *
     * 示例：
     * ```kotlin
     * val intType = BasicType(intConstructor, intMemberScope)
     * intType.typeName // "Int"
     * ```
     */
    val typeName = constructor.declarationDescriptor?.name ?: ""

    /**
     * 字符串表示
     *
     * 返回类型的字符串表示，用于调试和显示。
     *
     * 示例：
     * ```kotlin
     * val intType = BasicType(...)
     * intType.toString() // "Int"
     * ```
     *
     * @return 类型的字符串表示
     */
    override fun toString(): String = "$typeName"

    /**
     * 类型精化（基础类型）
     *
     * 基础类型的精化直接返回自身，因为基础类型不需要进一步精化。
     *
     * 示例：
     * ```kotlin
     * val intType = BasicType(...)
     * val refinedType = intType.refine(typeRefiner) // 返回 intType
     * ```
     *
     * @param cangjieTypeRefiner 类型精化器
     * @return 精化后的类型（自身）
     */

    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): UnwrappedType = this

    /**
     * 替换类型属性（基础类型）
     *
     * 基础类型不支持属性替换，直接返回自身。
     *
     * 示例：
     * ```kotlin
     * val intType = BasicType(...)
     * val newAttributes = TypeAttributes(...)
     * val newType = intType.replaceAttributes(newAttributes) // 返回 intType
     * ```
     *
     * @param newAttributes 新的类型属性
     * @return 自身
     */
    override fun replaceAttributes(newAttributes: TypeAttributes) = this



    /**
     * 哈希码计算
     *
     * 计算基础类型的哈希码，用于哈希表存储。
     * 基于构造器、成员作用域和类型名称计算。
     *
     * 示例：
     * ```kotlin
     * val intType = BasicType(...)
     * val hashCode = intType.hashCode() // 计算哈希码
     * ```
     *
     * @return 哈希码
     */
    override fun hashCode(): Int {
        var result = constructor.hashCode()
        result = 31 * result + memberScope.hashCode()
        result = 31 * result + typeName.hashCode()
        return result
    }
}

/**
 * 简单类型抽象类
 *
 * 所有简单类型的基类，包括基础类型和Option类型。
 * 提供了统一的类型操作和字符串表示。
 *
 * 特点：
 * - 支持注解显示
 * - 支持Option标记显示（isOption为true时显示"?"）
 * - 支持类型参数显示
 * - 提供makeOptionAsSpecified抽象方法
 *
 * 示例：
 * ```kotlin
 * // Int 类型
 * val intType: SimpleType = BasicType(...)
 * intType.toString() // "Int"
 *
 * // ?Int 类型
 * val optionIntType: SimpleType = OptionType(intType)
 * optionIntType.toString() // "?Int"
 *
 * // 带注解的类型
 * val annotatedType: SimpleType = ...
 * annotatedType.toString() // "[@NotNull] ?Int"
 *
 * // 泛型类型
 * val listType: SimpleType = ...
 * listType.toString() // "List<Int>"
 * ```
 */
abstract class SimpleType : UnwrappedType(), SimpleTypeMarker, TypeArgumentListMarker {
    /**
     * 替换类型属性（简单类型）
     *
     * 创建具有新属性的简单类型副本。
     *
     * 示例：
     * ```kotlin
     * val newAttributes = TypeAttributes(...)
     * val newType = simpleType.replaceAttributes(newAttributes) // 替换属性
     * ```
     *
     * @param newAttributes 新的类型属性
     * @return 具有新属性的简单类型
     */
    abstract override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType


    /**
     * 字符串表示（简单类型）
     *
     * 返回简单类型的字符串表示，包括注解、Option标记和类型参数。
     *
     * 格式：
     * - 注解：[@注解名]
     * - Option标记：?（如果isOption为true）
     * - 类型名：constructor
     * - 类型参数：<参数1, 参数2, ...>
     *
     * 示例：
     * ```kotlin
     * // 基础类型
     * val intType: SimpleType = BasicType(...)
     * intType.toString() // "Int"
     *
     * // Option类型
     * val optionIntType: SimpleType = OptionType(intType)
     * optionIntType.toString() // "?Int"
     *
     * // 带注解的Option类型
     * val annotatedOptionType: SimpleType = ...
     * annotatedOptionType.toString() // "[@NotNull] ?Int"
     *
     * // 泛型类型
     * val listType: SimpleType = ...
     * listType.toString() // "List<Int>"
     *
     * // 带注解的泛型Option类型
     * val annotatedGenericOptionType: SimpleType = ...
     * annotatedGenericOptionType.toString() // "[@NotNull] ?List<Int>"
     * ```
     *
     * @return 类型的字符串表示
     */
    override fun toString(): String {
        return buildString {
            for (annotation in annotations) {
                append("[", DescriptorRenderer.DEBUG_TEXT.renderAnnotation(annotation), "] ")
            }
            if (isOption) append("?")  // 显示Option标记

            append(constructor)
            if (arguments.isNotEmpty()) arguments.joinTo(this, separator = ", ", prefix = "<", postfix = ">")
        }
    }
}


/**
 * Function类型
 *
 * 表示仓颉语言中的函数类型，如 (Int, String) -> Bool。
 * 函数类型包含参数类型列表和返回类型。
 *
 * 特点：
 * - 包含参数类型列表（parameterTypes）
 * - 包含返回类型（returnType）
 * - 支持接收者类型（receiverType，可选）
 * - 支持上下文接收者类型列表（contextReceiverTypes，可选）
 *
 * 示例：
 * ```kotlin
 * // (Int, String) -> Bool 类型
 * val paramTypes = listOf(intType, stringType)
 * val returnType = boolType
 * val functionType = FunctionType(
 *     constructor = functionConstructor,
 *     attributes = TypeAttributes.Empty,
 *     parameterTypes = paramTypes,
 *     returnType = returnType
 * )
 * functionType.toString() // "(Int, String) -> Bool"
 *
 * // 带接收者的函数类型：Int.(String) -> Bool
 * val receiverFunctionType = FunctionType(
 *     constructor = functionConstructor,
 *     attributes = TypeAttributes.Empty,
 *     receiverType = intType,
 *     parameterTypes = listOf(stringType),
 *     returnType = boolType
 * )
 * receiverFunctionType.toString() // "Int.(String) -> Bool"
 * ```
 */
class FunctionType(
    override val constructor: FunctionClassDescriptor.FunctionTypeConstructor,


    val parameterTypes: List<CangJieType>,
    val returnType: CangJieType,

    override val attributes: TypeAttributes = TypeAttributes.Empty,
) : SimpleType() {


    /**
     * 类型参数列表（函数类型）
     *
     * 返回包含接收者类型、参数类型和返回类型的类型参数列表。
     * 顺序：[...parameterTypes, returnType]
     */
    override val arguments: List<TypeArgument>
        get() = buildList {

            parameterTypes.forEach { add(TypeArgumentImpl(it)) }
            add(TypeArgumentImpl(returnType))
        }

    /**
     * 成员作用域（函数类型）
     *
     * 使用Function类型的成员作用域。
     */
    override val memberScope: MemberScope
        get() = constructor.declarationDescriptor.getUnsubstitutedMemberScope(CangJieTypeRefiner.Default)


    /**
     * 替换类型属性（函数类型）
     */
    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType {
        if (newAttributes == attributes) return this
        return FunctionType(
            constructor = constructor,
            attributes = newAttributes,

            parameterTypes = parameterTypes,
            returnType = returnType,

        )
    }

    /**
     * 类型精化（函数类型）
     */

    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): UnwrappedType {
        val refinedParameterTypes = parameterTypes.map { it.refine(cangjieTypeRefiner) }
        val refinedReturnType = returnType.refine(cangjieTypeRefiner)

        return FunctionType(
            constructor = constructor,
            attributes = attributes,
            parameterTypes = refinedParameterTypes,
            returnType = refinedReturnType,

        )
    }



    /**
     * 字符串表示（函数类型）
     *
     * 格式：
     * - 无接收者：(P1, P2, ...) -> R
     * - 有接收者：Receiver.(P1, P2, ...) -> R
     * - 有上下文接收者：context(C1, C2) Receiver.(P1, P2, ...) -> R
     */
    override fun toString(): String {
        return buildString {

            if (isOption) append("?")


            // 添加参数类型
            append("(")
            parameterTypes.joinTo(this, separator = ", ")
            append(")")

            // 添加返回类型
            append(" -> ")
            append(returnType)
        }
    }
}

/**
 * Tuple类型
 *
 * 表示仓颉语言中的元组类型，如 (Int, String, Bool)。
 * 元组类型包含一组有序的元素类型。
 *
 * 特点：
 * - 包含元素类型列表（elementTypes）
 * - 元素数量固定且有序
 * - 支持嵌套元组（如 (Int, (String, Bool))）
 * - 支持任意数量的元素
 *
 * 示例：
 * ```kotlin
 * // (Int, String) 类型
 * val elementTypes = listOf(intType, stringType)
 * val tupleType = TupleType(
 *     constructor = tupleConstructor,
 *     attributes = TypeAttributes.Empty,
 *     elementTypes = elementTypes
 * )
 * tupleType.toString() // "(Int, String)"
 *
 * // 嵌套元组类型：(Int, (String, Bool))
 * val innerTuple = TupleType(
 *     constructor = innerTupleConstructor,
 *     attributes = TypeAttributes.Empty,
 *     elementTypes = listOf(stringType, boolType)
 * )
 * val nestedTupleType = TupleType(
 *     constructor = tupleConstructor,
 *     attributes = TypeAttributes.Empty,
 *     elementTypes = listOf(intType, innerTuple)
 * )
 * nestedTupleType.toString() // "(Int, (String, Bool))"
 * ```
 */
class TupleType(
    override val constructor: TupleClassDescriptor.TupleTypeConstructor,

    val elementTypes: List<CangJieType>,
    override val attributes: TypeAttributes = TypeAttributes.Empty
) : SimpleType() {

    /**
     * 类型参数列表（元组类型）
     *
     * 返回包含所有元素类型的类型参数列表。
     */
    override val arguments: List<TypeArgument>
        get() = elementTypes.map { TypeArgumentImpl(it) }

    /**
     * 成员作用域（元组类型）
     *
     * 使用Tuple类型的成员作用域。
     */
    override val memberScope: MemberScope
        get() = constructor.declarationDescriptor.getUnsubstitutedMemberScope(CangJieTypeRefiner.Default)

    /**
     * 替换类型属性（元组类型）
     */
    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType {
        if (newAttributes == attributes) return this
        return TupleType(
            constructor = constructor,
            attributes = newAttributes,
            elementTypes = elementTypes,
        )
    }

    /**
     * 类型精化（元组类型）
     */

    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): UnwrappedType {
        val refinedElementTypes = elementTypes.map { it.refine(cangjieTypeRefiner) }

        return TupleType(
            constructor = constructor,
            attributes = attributes,
            elementTypes = refinedElementTypes,
        )
    }


    /**
     * 字符串表示（元组类型）
     *
     * 格式：(T1, T2, T3, ...)
     */
    override fun toString(): String {
        return buildString {
            // 添加注解

            if (isOption) append("?")

            // 添加元素类型
            append("(")
            elementTypes.joinTo(this, separator = ", ")
            append(")")
        }
    }

    /**
     * 获取指定位置的元素类型
     *
     * @param index 元素位置索引（从0开始）
     * @return 指定位置的元素类型
     * @throws IndexOutOfBoundsException 如果索引超出范围
     */
    fun getElementType(index: Int): CangJieType {
        return elementTypes[index]
    }

    /**
     * 元组的元素数量
     */
    val arity: Int
        get() = elementTypes.size

    /**
     * 检查是否为空元组
     */
    val isEmpty: Boolean
        get() = elementTypes.isEmpty()

    /**
     * 相等性比较
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TupleType) return false

        return constructor == other.constructor &&
                attributes == other.attributes &&
                elementTypes == other.elementTypes &&
                isOption == other.isOption
    }

    /**
     * 哈希码计算
     */
    override fun hashCode(): Int {
        var result = constructor.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + elementTypes.hashCode()
        result = 31 * result + isOption.hashCode()
        return result
    }
}

// 删除复杂的SugarOptionType和ExplicitOptionType
// IDE插件不需要区分语法糖和显式形式，统一使用OptionType

/**
 * 将类型转换为简单类型
 *
 * 在类型替换后用于将类型转换为简单类型。
 * 如果类型不是简单类型，会抛出错误。
 *
 * 示例：
 * ```kotlin
 * val simpleType: SimpleType = someType.asSimpleType() // 转换为简单类型
 * ```
 *
 * @return 简单类型
 * @throws Error 如果类型不是简单类型
 */
fun CangJieType.asSimpleType(): SimpleType {
    return unwrap() as? SimpleType ?: error("This should be simple type: $this")
}


/**
 * 灵活类型
 *
 * 表示一个类型范围，包含下界（lowerBound）和上界（upperBound）。
 * 用于表示类型推断过程中的不确定性，或类型约束。
 *
 * 特点：
 * - lowerBound 是 upperBound 的子类型
 * - 通过 delegate 委托大部分操作
 * - 支持子类型关系表示
 * - 提供makeOptionAsSpecified方法实现
 *
 * 使用场景：
 * - 类型推断：当编译器无法确定确切类型时
 * - 类型约束：表示类型必须满足的约束条件
 * - 渐进式类型检查：类型在检查过程中逐渐精确化
 *
 * 示例：
 * ```kotlin
 * // 类型推断场景：知道类型至少是 Int，但可能更具体
 * val flexibleType = FlexibleType(
 *     lowerBound = Int类型,
 *     upperBound = Number类型
 * )
 * flexibleType.subTypeRepresentative // Int类型
 * flexibleType.superTypeRepresentative // Number类型
 *
 * // 类型约束场景：类型必须满足特定约束
 * val constrainedType = FlexibleType(
 *     lowerBound = 具体类型,
 *     upperBound = 约束类型
 * )
 *
 * // 渐进式类型检查
 * val evolvingType = FlexibleType(
 *     lowerBound = 当前已知类型,
 *     upperBound = 可能更精确的类型
 * )
 * ```
 */
abstract class FlexibleType(val lowerBound: SimpleType, val upperBound: SimpleType) :
    UnwrappedType(), SubtypingRepresentatives, FlexibleTypeMarker {

    /**
     * 委托类型
     *
     * 灵活类型委托大部分操作给这个简单类型。
     *
     * 示例：
     * ```kotlin
     * val delegate = flexibleType.delegate // 获取委托类型
     * ```
     */
    abstract val delegate: SimpleType

    /**
     * 子类型代表
     *
     * 返回下界作为子类型代表。
     *
     * 示例：
     * ```kotlin
     * val flexibleType = FlexibleType(Int类型, Number类型)
     * flexibleType.subTypeRepresentative // 返回 Int类型
     * ```
     */
    override val subTypeRepresentative: CangJieType
        get() = lowerBound

    /**
     * 超类型代表
     *
     * 返回上界作为超类型代表。
     *
     * 示例：
     * ```kotlin
     * val flexibleType = FlexibleType(Int类型, Number类型)
     * flexibleType.superTypeRepresentative // 返回 Number类型
     * ```
     */
    override val superTypeRepresentative: CangJieType
        get() = upperBound

    /**
     * 检查是否具有相同的类型构造器
     *
     * 灵活类型与任何类型都不具有相同的类型构造器。
     *
     * 示例：
     * ```kotlin
     * val flexibleType = FlexibleType(...)
     * flexibleType.sameTypeConstructor(anyType) // false
     * ```
     *
     * @param type 要检查的类型
     * @return 始终返回false
     */
    override fun sameTypeConstructor(type: CangJieType) = false

    /**
     * 渲染类型
     *
     * 使用指定的渲染器和选项渲染类型。
     *
     * 示例：
     * ```kotlin
     * val renderer = DescriptorRenderer.DEBUG_TEXT
     * val options = DescriptorRendererOptions()
     * val rendered = flexibleType.render(renderer, options) // 渲染类型
     * ```
     *
     * @param renderer 描述符渲染器
     * @param options 渲染选项
     * @return 渲染后的字符串
     */
    abstract fun render(renderer: DescriptorRenderer, options: DescriptorRendererOptions): String

    /**
     * 类型属性（灵活类型）
     *
     * 委托给委托类型的属性。
     *
     * 示例：
     * ```kotlin
     * val flexibleType = FlexibleType(...)
     * val attributes = flexibleType.attributes // 返回 delegate.attributes
     * ```
     */
    override val attributes: TypeAttributes get() = delegate.attributes

    /**
     * 类型构造器（灵活类型）
     *
     * 委托给委托类型的构造器。
     *
     * 示例：
     * ```kotlin
     * val flexibleType = FlexibleType(...)
     * val constructor = flexibleType.constructor // 返回 delegate.constructor
     * ```
     */
    override val constructor: TypeConstructor get() = delegate.constructor

    /**
     * 类型参数列表（灵活类型）
     *
     * 委托给委托类型的参数列表。
     *
     * 示例：
     * ```kotlin
     * val flexibleType = FlexibleType(...)
     * val arguments = flexibleType.arguments // 返回 delegate.arguments
     * ```
     */
    override val arguments: List<TypeArgument> get() = delegate.arguments



    /**
     * 成员作用域（灵活类型）
     *
     * 委托给委托类型的成员作用域。
     *
     * 示例：
     * ```kotlin
     * val flexibleType = FlexibleType(...)
     * val memberScope = flexibleType.memberScope // 返回 delegate.memberScope
     * ```
     */
    override val memberScope: MemberScope get() = delegate.memberScope

    /**
     * 字符串表示（灵活类型）
     *
     * 使用调试文本渲染器渲染类型。
     *
     * 示例：
     * ```kotlin
     * val flexibleType = FlexibleType(...)
     * flexibleType.toString() // 返回渲染后的字符串
     * ```
     *
     * @return 类型的字符串表示
     */
    override fun toString(): String = DescriptorRenderer.DEBUG_TEXT.renderType(this)

//    
//    abstract override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): FlexibleType
}

// =======================================================
// Option类型扩展方法
// =======================================================
// 注意：makeOptionAsSpecified方法现在是UnwrappedType的抽象方法，
// 不再作为扩展方法提供

/**
 * 解包Option类型
 *
 * 递归解包Option类型，直到得到非Option的内部类型。
 * 对于嵌套的Option类型（如 Option<Option<Int>>），会完全解包到最内层类型。
 *
 * 示例：
 * ```kotlin
 * // 简单Option类型
 * val optionIntType: CangJieType = OptionType(intType)
 * val unboxedType = optionIntType.unwrapOption() // 返回 Int 类型
 *
 * // 嵌套Option类型
 * val nestedOptionType: CangJieType = OptionType(optionIntType)
 * val unboxedType = nestedOptionType.unwrapOption() // 返回 Int 类型
 *
 * // 非Option类型
 * val intType: CangJieType = BasicType(...)
 * val unboxedType = intType.unwrapOption() // 返回 Int 类型（无变化）
 * ```
 *
 * @return 解包后的内部类型
 */
fun CangJieType.unwrapOption(): CangJieType {
   return OptionTypeUtils.unwrapOptionType(this) ?: this
}

/**
 * 计算Option嵌套层级
 *
 * 计算类型的Option嵌套深度，即Option包装的层数。
 * 对于非Option类型返回0，对于嵌套Option类型返回嵌套层数。
 *
 * 示例：
 * ```kotlin
 * // 非Option类型
 * val intType: CangJieType = BasicType(...)
 * intType.countOptionNestedLevel() // 0
 *
 * // 简单Option类型
 * val optionIntType: CangJieType = OptionType(intType)
 * optionIntType.countOptionNestedLevel() // 1
 *
 * // 嵌套Option类型
 * val nestedOptionType: CangJieType = OptionType(optionIntType)
 * nestedOptionType.countOptionNestedLevel() // 2
 *
 * // 三重嵌套
 * val tripleNestedType: CangJieType = OptionType(nestedOptionType)
 * tripleNestedType.countOptionNestedLevel() // 3
 * ```
 *
 * @return Option嵌套层级数
 */
fun CangJieType.countOptionNestedLevel(): Int {
    return OptionTypeUtils.getOptionNestedLevel(this)
}

class ThisType(private val otype: SimpleType) : SimpleType() {


    fun getType(): CangJieType {
        return otype.arguments[0].type
    }

    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType {
        return otype.replaceAttributes(newAttributes)

    }


    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): UnwrappedType {
        return otype.refine(cangjieTypeRefiner)
    }

    override val constructor: TypeConstructor
        get() = otype.constructor
    override val arguments: List<TypeArgument>
        get() = otype.arguments
    override val attributes: TypeAttributes
        get() = otype.attributes

    override val memberScope: MemberScope
        get() = otype.memberScope
}
