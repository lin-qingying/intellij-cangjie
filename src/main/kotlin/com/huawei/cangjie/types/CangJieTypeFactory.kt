package com.huawei.cangjie.types

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.TypeAliasDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.descriptors.impl.basic.BasicTypeDescriptor
import com.huawei.cangjie.resolve.constants.IntegerLiteralTypeConstructor
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.error.ErrorScopeKind
import com.huawei.cangjie.descriptors.impl.getRefinedMemberScopeIfPossible
import com.huawei.cangjie.descriptors.impl.getRefinedUnsubstitutedMemberScopeIfPossible
import com.huawei.cangjie.resolve.descriptorUtil.getCangJieTypeRefiner
import com.huawei.cangjie.resolve.descriptorUtil. module
private class ExpandedTypeOrRefinedConstructor(val expandedType: SimpleType?, val refinedConstructor: TypeConstructor?)

object CangJieTypeFactory {
    @JvmStatic
    fun simpleNotNullType(
        attributes: TypeAttributes,
        descriptor: ClassDescriptor,
        arguments: List<TypeProjection>
    ): SimpleType = simpleType(attributes, descriptor.typeConstructor, arguments, nullable = false)

    @JvmStatic

    fun basicType(descriptor: BasicTypeDescriptor): BasicType {
        return BasicType(
            descriptor.typeConstructor,
            descriptor.basicMemberScope

        )
    }

    @JvmStatic
    fun integerLiteralType(
        attributes: TypeAttributes,
        constructor: IntegerLiteralTypeConstructor,
        nullable: Boolean
    ): SimpleType = simpleTypeWithNonTrivialMemberScope(
        attributes,
        constructor,
        emptyList(),
        nullable,
        ErrorUtils.createErrorScope(
            ErrorScopeKind.INTEGER_LITERAL_TYPE_SCOPE,
            throwExceptions = true,
            "unknown integer literal type"
        )
    )

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
            is TypeParameterDescriptor -> descriptor.getDefaultType().memberScope
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
    fun simpleType(
        attributes: TypeAttributes,
        constructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean,
        cangjieTypeRefiner: CangJieTypeRefiner? = null
    ): SimpleType {
        if (attributes.isEmpty() && arguments.isEmpty() && !nullable && constructor.declarationDescriptor != null) {
            return constructor.declarationDescriptor!!.defaultType
        }

        return simpleTypeWithNonTrivialMemberScope(
            attributes, constructor, arguments,
            nullable,
            computeMemberScope(constructor, arguments, cangjieTypeRefiner)
        ) f@{ refiner ->
            val expandedTypeOrRefinedConstructor = refineConstructor(constructor, refiner, arguments) ?: return@f null
            expandedTypeOrRefinedConstructor.expandedType?.let { return@f it }

            simpleType(
                attributes,
                expandedTypeOrRefinedConstructor.refinedConstructor!!,
                arguments, nullable,
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
        nullable: Boolean,
        memberScope: MemberScope,
        refinedTypeFactory: RefinedTypeFactory
    ): SimpleType =
        SimpleTypeImpl(constructor, arguments, nullable, memberScope, refinedTypeFactory)
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
        nullable: Boolean,
        memberScope: MemberScope
    ): SimpleType =
        SimpleTypeImpl(constructor, arguments, nullable, memberScope) { cangjieTypeRefiner ->
            val expandedTypeOrRefinedConstructor =
                refineConstructor(constructor, cangjieTypeRefiner, arguments) ?: return@SimpleTypeImpl null
            expandedTypeOrRefinedConstructor.expandedType?.let { return@SimpleTypeImpl it }

            simpleTypeWithNonTrivialMemberScope(
                attributes,
                expandedTypeOrRefinedConstructor.refinedConstructor!!,
                arguments,
                nullable,
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

    override fun makeNullableAsSpecified(newNullability: Boolean): SimpleType {
//        if (newNullability == isMarkedOption) return this
        return delegate.makeNullableAsSpecified(newNullability).replaceAttributes(attributes)
    }
}

private class SimpleTypeWithAttributes(
    delegate: SimpleType,
    override val attributes: TypeAttributes
) : DelegatingSimpleTypeImpl(delegate) {
    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = SimpleTypeWithAttributes(delegate, attributes)

}

private class SimpleTypeImpl(
    override val constructor: TypeConstructor,
    override val arguments: List<TypeProjection>,
    override val isMarkedOption: Boolean,
    override val memberScope: MemberScope,
    private val refinedTypeFactory: RefinedTypeFactory
) : SimpleType() {
    override fun makeNullableAsSpecified(newNullability: Boolean) = when {
        newNullability == isMarkedOption -> this
        newNullability -> NullableSimpleType(this)
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

private class NullableSimpleType(delegate: SimpleType) : DelegatingSimpleTypeImpl(delegate) {
    override val isMarkedOption: Boolean
        get() = true

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = NullableSimpleType(delegate)
}

private class NotNullSimpleType(delegate: SimpleType) : DelegatingSimpleTypeImpl(delegate) {
    override val isMarkedOption: Boolean
        get() = false

    @TypeRefinement
    override fun replaceDelegate(delegate: SimpleType) = NotNullSimpleType(delegate)
}
