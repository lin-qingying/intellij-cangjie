package com.linqingying.cangjie.types

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.descriptors.ClassifierDescriptor
import com.linqingying.cangjie.descriptors.TypeParameterDescriptor
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.error.ErrorTypeKind

val CangJieType?.isFunctionPlaceholder: Boolean
    get() {
        return this != null && constructor is FunctionPlaceholderTypeConstructor
    }
class FunctionPlaceholderTypeConstructor(
    val argumentTypes: List<CangJieType>,
    val hasDeclaredArguments: Boolean,
    private val cangjieBuiltIns: CangJieBuiltIns
) : TypeConstructor {
    private val errorTypeConstructor: TypeConstructor =
        ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.FUNCTION_PLACEHOLDER_TYPE, argumentTypes.toString())

    override fun getParameters(): List<TypeParameterDescriptor> {
        return errorTypeConstructor.parameters
    }
    override fun isFinal(): Boolean {
        return errorTypeConstructor.isFinal
    }

    override fun getSupertypes(): Collection<CangJieType> {
        return errorTypeConstructor.supertypes
    }


    override fun isDenotable(): Boolean {
        return errorTypeConstructor.isDenotable
    }

    override fun getDeclarationDescriptor(): ClassifierDescriptor? {
        return errorTypeConstructor.declarationDescriptor
    }

    override fun toString(): String {
        return errorTypeConstructor.toString()
    }

    override fun getBuiltIns(): CangJieBuiltIns {
        return cangjieBuiltIns
    }

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this
}
