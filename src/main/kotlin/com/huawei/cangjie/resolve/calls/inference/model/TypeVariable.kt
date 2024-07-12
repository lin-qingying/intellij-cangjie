package com.huawei.cangjie.resolve.calls.inference.model

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.name.SpecialNames
import com.huawei.cangjie.resolve.builtIns
import com.huawei.cangjie.resolve.descriptorUtil.hasOnlyInputTypesAnnotation
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.checker.NewTypeVariableConstructor
import com.huawei.cangjie.types.model.TypeVariableMarker
import com.huawei.cangjie.types.model.TypeVariableTypeConstructorMarker

class TypeVariableTypeConstructor(
    private val builtIns: CangJieBuiltIns,
    val debugName: String,
    override val originalTypeParameter: TypeParameterDescriptor?
) : NewTypeVariableConstructor, TypeVariableTypeConstructorMarker {
    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()
    override fun getSupertypes(): Collection<CangJieType> = emptyList()

    //    override fun isFinal(): Boolean = false
    override fun isDenotable(): Boolean = false
    override fun getDeclarationDescriptor(): ClassifierDescriptor? = null

    override fun getBuiltIns() = builtIns

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this

    override fun toString() = "TypeVariable($debugName)"

    var isContainedInInvariantOrContravariantPositions: Boolean = false
}

fun TypeConstructor.typeForTypeVariable(): SimpleType {
    require(this is TypeVariableTypeConstructor)
    return CangJieTypeFactory.simpleTypeWithNonTrivialMemberScope(
        TypeAttributes.Empty, this, arguments = emptyList(),
        nullable = false, memberScope = builtIns.any.unsubstitutedMemberScope
    )
}

class TypeVariableFromCallableDescriptor(
    val originalTypeParameter: TypeParameterDescriptor
) : NewTypeVariable(
    originalTypeParameter.builtIns,
    SpecialNames.safeIdentifier(originalTypeParameter.name).identifier,
    originalTypeParameter
) {
    override fun hasOnlyInputTypesAnnotation(): Boolean = originalTypeParameter.hasOnlyInputTypesAnnotation()
}

sealed class NewTypeVariable(
    builtIns: CangJieBuiltIns,
    name: String,
    originalTypeParameter: TypeParameterDescriptor? = null
) : TypeVariableMarker {
    val freshTypeConstructor = TypeVariableTypeConstructor(builtIns, name, originalTypeParameter)

    // member scope is used if we have receiver with type TypeVariable(T)
    // todo add to member scope methods from supertypes for type variable
    val defaultType: SimpleType = freshTypeConstructor.typeForTypeVariable()
    abstract fun hasOnlyInputTypesAnnotation(): Boolean

    override fun toString() = freshTypeConstructor.toString()
}
