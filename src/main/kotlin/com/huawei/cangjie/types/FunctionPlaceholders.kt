package com.huawei.cangjie.types

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.descriptors.ClassifierDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.error.ErrorTypeKind

val CangJieType?.isFunctionPlaceholder: Boolean
    get() {
        return this != null && constructor is FunctionPlaceholderTypeConstructor
    }
class FunctionPlaceholderTypeConstructor(
    val argumentTypes: List<CangJieType>,
    val hasDeclaredArguments: Boolean,
    private val kotlinBuiltIns: CangJieBuiltIns
) : TypeConstructor {
    private val errorTypeConstructor: TypeConstructor =
        ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.FUNCTION_PLACEHOLDER_TYPE, argumentTypes.toString())

    override fun getParameters(): List<TypeParameterDescriptor> {
        return errorTypeConstructor.parameters
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
        return kotlinBuiltIns
    }

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: CangJieTypeRefiner): TypeConstructor = this
}
