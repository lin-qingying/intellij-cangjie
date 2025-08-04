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

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.TypeAliasDescriptor
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.descriptors.impl.PrimitiveClassDescriptor
import cn.cangnova.cangjie.descriptors.impl.getRefinedMemberScopeIfPossible
import cn.cangnova.cangjie.descriptors.impl.getRefinedUnsubstitutedMemberScopeIfPossible
import cn.cangnova.cangjie.resolve.constants.FloatLiteralTypeConstructor
import cn.cangnova.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import cn.cangnova.cangjie.resolve.getCangJieTypeRefiner
import cn.cangnova.cangjie.resolve.module
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.error.ErrorScopeKind

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
     * 使用类描述符创建非Option的简单类型。
     * 
     * 示例：
     * ```kotlin
     * val attributes = TypeAttributes.Empty
     * val descriptor: ClassDescriptor = ...
     * val arguments = listOf(...)
     * val simpleType = CangJieTypeFactory.simpleNotNullType(attributes, descriptor, arguments)
     * // 创建非Option的简单类型
     * ```
     * 
     * @param attributes 类型属性
     * @param descriptor 类描述符
     * @param arguments 类型参数列表
     * @return 非空的简单类型
     */
    @JvmStatic
    fun simpleNotNullType(
        attributes: TypeAttributes,
        descriptor: ClassDescriptor,
        arguments: List<TypeProjection>
    ): SimpleType = simpleType(attributes, descriptor.typeConstructor, arguments, option = false)

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
        option: Boolean
    ): SimpleType = simpleTypeWithNonTrivialMemberScope(
        attributes,
        constructor,
        emptyList(),
        option,
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
        option: Boolean
    ): SimpleType = simpleTypeWithNonTrivialMemberScope(
        attributes,
        constructor,
        emptyList(),
        option,
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

    @OptIn(TypeRefinement::class)
    private fun computeMemberScope(
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        cangjieTypeRefiner: CangJieTypeRefiner? = null
    ): MemberScope {
        return when (val descriptor = constructor.declarationDescriptor) {
            is TypeParameterDescriptor -> descriptor.defaultType.memberScope
            is ClassDescriptor -> {
                val refinerToUse = cangjieTypeRefiner ?: descriptor.module.getCangJieTypeRefiner()
                if (arguments.isEmpty())
                    descriptor.getRefinedUnsubstitutedMemberScopeIfPossible(refinerToUse)
                else
                // REVIEW
                    descriptor.getRefinedMemberScopeIfPossible(
                        TypeConstructorSubstitution.create(constructor, arguments),
                        refinerToUse
                    )
            }

            is TypeAliasDescriptor -> ErrorUtils.createErrorScope(
                ErrorScopeKind.SCOPE_FOR_ABBREVIATION_TYPE, throwExceptions = true, descriptor.name.toString()
            )

            else -> {
                if (constructor is IntersectionTypeConstructor) {
                    return constructor.createScopeForCangJieType()
                }

                throw IllegalStateException("Unsupported classifier: $descriptor for constructor: $constructor")
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    @OptIn(TypeRefinement::class)
    fun optionType(
        type: SimpleType,
    ): SimpleType {
        return OptionType(type)
    }
      @JvmStatic
    fun simpleType(
        baseType: SimpleType,
        annotations: TypeAttributes = baseType.attributes,
        constructor: TypeConstructor = baseType.constructor,
        arguments: List<TypeProjection> = baseType.arguments,
        option: Boolean = baseType.isOption
    ): SimpleType = simpleType(annotations, constructor, arguments, option)

    @JvmStatic
    @JvmOverloads
    @OptIn(TypeRefinement::class)
    fun simpleType(
        attributes: TypeAttributes,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        option: Boolean,
        cangjieTypeRefiner: CangJieTypeRefiner? = null
    ): SimpleType {
        if (attributes.isEmpty() && arguments.isEmpty() && !option && constructor.declarationDescriptor != null) {
            return constructor.declarationDescriptor!!.defaultType
        }

        return simpleTypeWithNonTrivialMemberScope(
            attributes, constructor, arguments,
            option,
            computeMemberScope(constructor, arguments, cangjieTypeRefiner)
        ) f@{ refiner ->
            val expandedTypeOrRefinedConstructor = refineConstructor(constructor, refiner, arguments) ?: return@f null
            expandedTypeOrRefinedConstructor.expandedType?.let { return@f it }

            simpleType(
                attributes,
                expandedTypeOrRefinedConstructor.refinedConstructor!!,
                arguments, option,
                refiner
            )
        }
    }

    @JvmStatic
    fun TypeAliasDescriptor.computeExpandedType(arguments: List<TypeProjection>): SimpleType {
        return TypeAliasExpander(TypeAliasExpansionReportStrategy.DO_NOTHING, false).expand(
            TypeAliasExpansion.create(null, this, arguments), TypeAttributes.Empty
        )
    }

    @TypeRefinement
    private fun refineConstructor(
        constructor: TypeConstructor,
        cangjieTypeRefiner: CangJieTypeRefiner,
        arguments: List<TypeProjection>
    ): ExpandedTypeOrRefinedConstructor? {
        val basicDescriptor = constructor.declarationDescriptor
        val descriptor = basicDescriptor?.let { cangjieTypeRefiner.refineDescriptor(it) } ?: return null

//        if (descriptor is TypeAliasDescriptor) {
//            return ExpandedTypeOrRefinedConstructor(descriptor.computeExpandedType(arguments), null)
//        }

        val refinedConstructor = descriptor.typeConstructor.refine(cangjieTypeRefiner)
        return ExpandedTypeOrRefinedConstructor(null, refinedConstructor)
    }

    @JvmStatic
    fun simpleTypeWithNonTrivialMemberScope(
        attributes: TypeAttributes,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        option: Boolean,
        memberScope: MemberScope,
        refinedTypeFactory: RefinedTypeFactory
    ): SimpleType =
        SimpleTypeImpl(constructor, arguments, option, memberScope, refinedTypeFactory)
            .let {
                if (attributes.isEmpty())
                    it
                else
                    SimpleTypeWithAttributes(it, attributes)
            }

    @JvmStatic
    @OptIn(TypeRefinement::class)
    fun simpleTypeWithNonTrivialMemberScope(
        attributes: TypeAttributes,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        option: Boolean,
        memberScope: MemberScope
    ): SimpleType =
        SimpleTypeImpl(constructor, arguments, option, memberScope) { cangjieTypeRefiner ->
            val expandedTypeOrRefinedConstructor =
                refineConstructor(constructor, cangjieTypeRefiner, arguments) ?: return@SimpleTypeImpl null
            expandedTypeOrRefinedConstructor.expandedType?.let { return@SimpleTypeImpl it }

            simpleTypeWithNonTrivialMemberScope(
                attributes,
                expandedTypeOrRefinedConstructor.refinedConstructor!!,
                arguments,
                option,
                memberScope
            )
        }.let {
            if (attributes.isEmpty())
                it
            else
                SimpleTypeWithAttributes(it, attributes)
        }

}
typealias RefinedTypeFactory = (CangJieTypeRefiner) -> SimpleType?

abstract class DelegatingSimpleTypeImpl(override val delegate: SimpleType) : DelegatingSimpleType() {
    override fun replaceAttributes(newAttributes: TypeAttributes): SimpleType =
        if (newAttributes !== attributes)
            SimpleTypeWithAttributes(this, newAttributes)
        else
            this

    override fun makeOptionAsSpecified(isOption: Boolean): SimpleType {
//        if (newOption == isOption) return this
        return delegate.makeOptionAsSpecified(isOption).replaceAttributes(attributes)
    }
}

private class SimpleTypeWithAttributes(
    delegate: SimpleType,
    override val attributes: TypeAttributes
) : DelegatingSimpleTypeImpl(delegate) {
    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = SimpleTypeWithAttributes(delegate, attributes)

}

class VArrayType(
    val size : Int,
    argument : TypeProjection ,

    constructor: TypeConstructor,
    isOption: Boolean,
    memberScope: MemberScope,
    refinedTypeFactory: RefinedTypeFactory
) : SimpleTypeImpl(

    constructor, listOf(argument), isOption, memberScope, refinedTypeFactory
) {

val typeName = "VArray"
    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): SimpleType {
        return refinedTypeFactory(cangjieTypeRefiner) ?: this

    }


}

open class SimpleTypeImpl(
    override val constructor: TypeConstructor,
    override val arguments: List<TypeProjection>,
    override val isOption: Boolean,
    override val memberScope: MemberScope,
    protected val refinedTypeFactory: RefinedTypeFactory
) : SimpleType() {


    override fun makeOptionAsSpecified(isOption: Boolean) = when {
        isOption == isOption -> this
        isOption -> OptionalSimpleType(this )
        else -> NotNullSimpleType(this)
    }

    override fun replaceAttributes(newAttributes: TypeAttributes) =
        if (newAttributes.isEmpty())
            this
        else SimpleTypeWithAttributes(this, newAttributes)


    override val attributes: TypeAttributes get() = TypeAttributes.Empty

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): SimpleType {
        return refinedTypeFactory(cangjieTypeRefiner) ?: this
    }

}

class OptionalSimpleType(delegate: SimpleType) : DelegatingSimpleTypeImpl(delegate) {
    override val isOption: Boolean
        get() = true

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = OptionalSimpleType(delegate)
}

class NotNullSimpleType(delegate: SimpleType) : DelegatingSimpleTypeImpl(delegate) {
    override val isOption: Boolean
        get() = false

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = NotNullSimpleType(delegate)
}
