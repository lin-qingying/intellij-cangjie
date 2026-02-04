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

import org.cangnova.cangjie.descriptors.ClassAndEnumDescriptor
import org.cangnova.cangjie.descriptors.HasScopeDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.impl.PrimitiveClassDescriptor
import org.cangnova.cangjie.descriptors.impl.getRefinedMemberScopeIfPossible
import org.cangnova.cangjie.descriptors.impl.getRefinedUnsubstitutedMemberScopeIfPossible
import org.cangnova.cangjie.resolve.constants.FloatLiteralTypeConstructor
import org.cangnova.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import org.cangnova.cangjie.resolve.getCangJieTypeRefiner
import org.cangnova.cangjie.resolve.module
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.checker.TypeVariableConstructor
import org.cangnova.cangjie.types.error.ErrorScopeKind

private class ExpandedTypeOrRefinedConstructor(val expandedType: SimpleType?, val refinedConstructor: TypeConstructor?)

/**
 * 仓颉类型工厂
 *
 * 提供创建各种仓颉语言类型的工厂方法，包括：
 * - 简单类型（SimpleType）
 * - 基础类型（BasicType）
 * - 灵活类型（FlexibleType）
 * - 字面量类型（FloatLiteralType、IntegerLiteralType）
 * - 数组类型（VArrayType）
 *
 * 核心功能：
 * - simpleType：创建简单类型
 * - flexibleType：创建灵活类型
 * - basicType：创建基础类型
 * - arrayType：创建数组类型
 *
 * 示例：
 * ```kotlin
 * // 创建简单类型
 * val simpleType = CangJieTypeFactory.simpleType(attributes, constructor, arguments, false)
 *
 * // 创建灵活类型
 * val flexibleType = CangJieTypeFactory.flexibleType(lowerBound, upperBound)
 *
 * // 创建基础类型
 * val basicType = CangJieTypeFactory.basicType(descriptor)
 *
 * // 创建数组类型
 * val arrayType = CangJieTypeFactory.arrayType(elementType)
 * ```
 */
object CangJieTypeFactory {

    /**
     * 创建非空的简单类型
     *
     * 使用类或枚举描述符创建非Option的简单类型。
     *
     * 示例：
     * ```kotlin
     * val attributes = TypeAttributes.Empty
     * val descriptor: ClassAndEnumDescriptor = ...
     * val arguments = listOf(...)
     * val simpleType = CangJieTypeFactory.simpleNonOptionType(attributes, descriptor, arguments)
     * // 创建非Option的简单类型
     * ```
     *
     * @param attributes 类型属性
     * @param descriptor 类或枚举描述符
     * @param arguments 类型参数列表
     * @return 非空的简单类型
     */
    @JvmStatic
    fun simpleType(
        attributes: TypeAttributes,
        descriptor: ClassAndEnumDescriptor,
        arguments: List<TypeArgument>
    ): SimpleType = simpleType(attributes, descriptor.typeConstructor, arguments)

    /**
     * 创建基础类型
     *
     * 使用基础类型描述符创建基础类型。
     *
     * 示例：
     * ```kotlin
     * val descriptor: BasicTypeDescriptor = ...
     * val basicType = CangJieTypeFactory.basicType(descriptor)
     * // 创建基础类型，如Int、String等
     * ```
     *
     * @param descriptor 基础类型描述符
     * @return 基础类型
     */
    @JvmStatic
    fun basicType(descriptor: PrimitiveClassDescriptor): BasicType {
        return BasicType(
            descriptor.typeConstructor,
            descriptor.unsubstitutedMemberScope
        )
    }

    /**
     * 创建浮点数字面量类型
     *
     * 创建表示浮点数字面量的类型，使用错误作用域。
     *
     * 示例：
     * ```kotlin
     * val attributes = TypeAttributes.Empty
     * val constructor = FloatLiteralTypeConstructor(...)
     * val floatLiteralType = CangJieTypeFactory.floatLiteralType(attributes, constructor, false)
     * // 创建浮点数字面量类型
     * ```
     *
     * @param attributes 类型属性
     * @param constructor 浮点数字面量类型构造器
     * @param option 是否为Option类型
     * @return 浮点数字面量类型
     */
    @JvmStatic
    fun floatLiteralType(
        attributes: TypeAttributes,
        constructor: FloatLiteralTypeConstructor,

    ): SimpleType = simpleTypeWithNonTrivialMemberScope(
        attributes,
        constructor,
        emptyList(),

        ErrorUtils.createErrorScope(
            ErrorScopeKind.FLOAT_LITERAL_TYPE_SCOPE,
            throwExceptions = true,
            "unknown float literal type"
        )
    )

    /**
     * 创建整数字面量类型
     *
     * 创建表示整数字面量的类型，使用错误作用域。
     *
     * 示例：
     * ```kotlin
     * val attributes = TypeAttributes.Empty
     * val constructor = IntegerLiteralTypeConstructor(...)
     * val integerLiteralType = CangJieTypeFactory.integerLiteralType(attributes, constructor, false)
     * // 创建整数字面量类型
     * ```
     *
     * @param attributes 类型属性
     * @param constructor 整数字面量类型构造器
     * @param option 是否为Option类型
     * @return 整数字面量类型
     */
    @JvmStatic
    fun integerLiteralType(
        attributes: TypeAttributes,
        constructor: IntegerLiteralTypeConstructor,

    ): SimpleType = simpleTypeWithNonTrivialMemberScope(
        attributes,
        constructor,
        emptyList(),

        ErrorUtils.createErrorScope(
            ErrorScopeKind.INTEGER_LITERAL_TYPE_SCOPE,
            throwExceptions = true,
            "unknown integer literal type"
        )
    )

    /**
     * 创建灵活类型
     *
     * 创建包含下界和上界的灵活类型。如果下界和上界相同，则返回下界。
     *
     * 示例：
     * ```kotlin
     * val lowerBound: SimpleType = Int类型
     * val upperBound: SimpleType = Number类型
     * val flexibleType = CangJieTypeFactory.flexibleType(lowerBound, upperBound)
     * // 创建灵活类型，表示类型范围
     * ```
     *
     * @param lowerBound 下界类型
     * @param upperBound 上界类型
     * @return 灵活类型
     */
    @JvmStatic
    fun flexibleType(lowerBound: SimpleType, upperBound: SimpleType): UnwrappedType {
        if (lowerBound == upperBound) return lowerBound
        return FlexibleTypeImpl(lowerBound, upperBound)
    }


    private fun computeMemberScope(
        constructor: TypeConstructor,
        arguments: List<TypeArgument>,
        cangjieTypeRefiner: CangJieTypeRefiner? = null
    ): MemberScope {
        return when (val descriptor = constructor.declarationDescriptor) {
            is TypeParameterDescriptor -> descriptor.defaultType.memberScope
            is HasScopeDescriptor -> {
                val refinerToUse = cangjieTypeRefiner ?: descriptor.module.getCangJieTypeRefiner()
                if (arguments.isEmpty())
                    descriptor.getRefinedUnsubstitutedMemberScopeIfPossible(refinerToUse)
                else {
                    // 创建类型参数到类型实参的映射
                    val substitutorMap = constructor.parameters.zip(arguments).associate { (param, arg) ->
                        param.typeConstructor to arg.type.unwrap()
                    }
                    val substitutor = ComposableTypeSubstitutor.create(substitutorMap)
                    descriptor.getRefinedMemberScopeIfPossible(substitutor, refinerToUse)
                }
            }

            is TypeAliasDescriptor -> ErrorUtils.createErrorScope(
                ErrorScopeKind.SCOPE_FOR_ABBREVIATION_TYPE, throwExceptions = true, descriptor.name.toString()
            )

            else -> {
                if (constructor is IntersectionTypeConstructor) {
                    return constructor.createScopeForCangJieType()
                }

                // Handle TypeVariableConstructor
                if (constructor is TypeVariableConstructor) {
                    // Type variables use the member scope of their supertypes (typically Any)
                    return constructor.builtIns.stdlibTypes.any.unsubstitutedMemberScope
                }

                // Handle IntegerLiteralTypeConstructor and FloatLiteralTypeConstructor
                if (constructor is IntegerLiteralTypeConstructor ||
                    constructor is FloatLiteralTypeConstructor) {
                    // Integer/Float literal types don't have members, return empty scope
                    return MemberScope.Empty
                }

                throw IllegalStateException("Unsupported classifier: $descriptor for constructor: $constructor")
            }
        }
    }

    @JvmStatic
    fun simpleType(
        baseType: SimpleType,
        annotations: TypeAttributes = baseType.attributes,
        constructor: TypeConstructor = baseType.constructor,
        arguments: List<TypeArgument> = baseType.arguments,
    ): SimpleType = simpleType(annotations, constructor, arguments)

    @JvmStatic
    @JvmOverloads

    fun simpleType(
        attributes: TypeAttributes,
        constructor: TypeConstructor,
        arguments: List<TypeArgument>,

        cangjieTypeRefiner: CangJieTypeRefiner? = null
    ): SimpleType {
        if (attributes.isEmpty() && arguments.isEmpty()   && constructor.declarationDescriptor != null) {
            constructor.declarationDescriptor?.defaultType?.let {
                return it
            }

        }

        return simpleTypeWithNonTrivialMemberScope(
            attributes, constructor, arguments,

            computeMemberScope(constructor, arguments, cangjieTypeRefiner)
        ) f@{ refiner ->
            val expandedTypeOrRefinedConstructor = refineConstructor(constructor, refiner, arguments) ?: return@f null
            expandedTypeOrRefinedConstructor.expandedType?.let { return@f it }

            simpleType(
                attributes,
                expandedTypeOrRefinedConstructor.refinedConstructor!!,
                arguments,
                refiner
            )
        }
    }

    @JvmStatic
    fun TypeAliasDescriptor.computeExpandedType(arguments: List<TypeArgument>): SimpleType {
        return TypeAliasExpander(TypeAliasExpansionReportStrategy.DO_NOTHING, false).expand(
            TypeAliasExpansion.create(null, this, arguments), TypeAttributes.Empty
        )
    }


    private fun refineConstructor(
        constructor: TypeConstructor,
        cangjieTypeRefiner: CangJieTypeRefiner,
        arguments: List<TypeArgument>
    ): ExpandedTypeOrRefinedConstructor? {
        val basicDescriptor = constructor.declarationDescriptor
        val descriptor = basicDescriptor?.let { cangjieTypeRefiner.refineDescriptor(it) } ?: return null

//        if (descriptor is TypeAliasDescriptor) {
//            return ExpandedTypeOrRefinedConstructor(descriptor.computeExpandedType(arguments), null)
//        }

        val refinedConstructor = descriptor.typeConstructor.refine(cangjieTypeRefiner)
        return ExpandedTypeOrRefinedConstructor(null, refinedConstructor)
    }


    fun simpleTypeWithNonTrivialMemberScope(
        attributes: TypeAttributes,
        constructor: TypeConstructor,
        arguments: List<TypeArgument>,

        memberScope: MemberScope,
        refinedTypeFactory: RefinedTypeFactory
    ): SimpleType {
        // extend 成员不再通过 memberScope 注入，而是在 TowerResolver.collectExtendMembers 中显式收集
        // 这样可以正确应用可见性检查（需要 LexicalScope，而 descriptors 模块无法访问）
        return SimpleTypeImpl(constructor, arguments, memberScope, refinedTypeFactory)
            .let {
                if (attributes.isEmpty())
                    it
                else
                    SimpleTypeWithAttributes(it, attributes)
            }
    }


    fun simpleTypeWithNonTrivialMemberScope(
        attributes: TypeAttributes,
        constructor: TypeConstructor,
        arguments: List<TypeArgument>,

        memberScope: MemberScope
    ): SimpleType {
        // extend 成员不再通过 memberScope 注入，而是在 TowerResolver.collectExtendMembers 中显式收集
        // 这样可以正确应用可见性检查（需要 LexicalScope，而 descriptors 模块无法访问）
        return SimpleTypeImpl(constructor, arguments, memberScope) { cangjieTypeRefiner ->
            val expandedTypeOrRefinedConstructor =
                refineConstructor(constructor, cangjieTypeRefiner, arguments) ?: return@SimpleTypeImpl null
            expandedTypeOrRefinedConstructor.expandedType?.let { return@SimpleTypeImpl it }

            simpleTypeWithNonTrivialMemberScope(
                attributes,
                expandedTypeOrRefinedConstructor.refinedConstructor!!,
                arguments,

                memberScope
            )
        }.let {
            if (attributes.isEmpty())
                it
            else
                SimpleTypeWithAttributes(it, attributes)
        }
    }

    @JvmStatic

    fun enumTypeWithNonTrivialMemberScope(
        attributes: TypeAttributes,
        constructor: EnumTypeConstructor,
        arguments: List<TypeArgument>,
        memberScope: MemberScope
    ): SimpleType {
        // extend 成员不再通过 memberScope 注入，而是在 TowerResolver.collectExtendMembers 中显式收集
        // 这样可以正确应用可见性检查（需要 LexicalScope，而 descriptors 模块无法访问）
        return EnumType(constructor, arguments, memberScope) { cangjieTypeRefiner ->
            val expandedTypeOrRefinedConstructor =
                refineConstructor(constructor, cangjieTypeRefiner, arguments) ?: return@EnumType null
            expandedTypeOrRefinedConstructor.expandedType?.let { return@EnumType it }

            enumTypeWithNonTrivialMemberScope(
                attributes,
                expandedTypeOrRefinedConstructor.refinedConstructor!! as EnumTypeConstructor,
                arguments,
                memberScope,
            )
        }.let {
            if (attributes.isEmpty())
                it
            else
                SimpleTypeWithAttributes(it, attributes)
        }
    }
}
typealias RefinedTypeFactory = (CangJieTypeRefiner) -> SimpleType?

abstract class DelegatingSimpleTypeImpl(override val delegate: SimpleType) : DelegatingSimpleType() {
    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        if (newAttributes !== attributes)
            SimpleTypeWithAttributes(this, newAttributes)
        else
            this
}


private class SimpleTypeWithAttributes(
    delegate: SimpleType,
    override val attributes: TypeAttributes
) : DelegatingSimpleTypeImpl(delegate) {

    override fun replaceDelegate(delegate: SimpleType) = SimpleTypeWithAttributes(delegate, attributes)

}

class VArrayType(
    val size: Int,
    argument: TypeArgument,
    constructor: TypeConstructor,
    memberScope: MemberScope,
    refinedTypeFactory: RefinedTypeFactory
) : SimpleTypeImpl(
    constructor, listOf(argument), memberScope, refinedTypeFactory
) {

    val typeName = "VArray"


    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): SimpleType {
        return refinedTypeFactory(cangjieTypeRefiner) ?: this

    }


}

open class SimpleTypeImpl(
    override val constructor: TypeConstructor,
    override val arguments: List<TypeArgument>,

    override val memberScope: MemberScope,
    protected val refinedTypeFactory: RefinedTypeFactory
) : SimpleType() {



    override fun replaceAttributes(newAttributes: TypeAttributes) =
        if (newAttributes.isEmpty())
            this
        else SimpleTypeWithAttributes(this, newAttributes)


    override val attributes: TypeAttributes get() = TypeAttributes.Empty


    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): SimpleType {
        return refinedTypeFactory(cangjieTypeRefiner) ?: this
    }

}


