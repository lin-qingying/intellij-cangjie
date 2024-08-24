package com.huawei.cangjie.types

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.error.*
import com.huawei.cangjie.types.util.isUnresolvedType

object ErrorUtils {
    private val errorProperty: PropertyDescriptor = ErrorPropertyDescriptor()
    val errorPropertyGroup: Set<PropertyDescriptor> = setOf(errorProperty)

    private  val errorVariable :VariableDescriptor = ErrorVariableDescriptor()
    val errorVariableGroup: Set<VariableDescriptor> = setOf(errorVariable)

    val errorModule: ModuleDescriptor = ErrorModuleDescriptor
    val errorPropertyType: CangJieType get() = createErrorType(ErrorTypeKind.ERROR_PROPERTY_TYPE)
    val errorVariableType: CangJieType get() = createErrorType(ErrorTypeKind.ERROR_VARIABLE_TYPE)


    // Do not move it into AbstractTypeConstructor.Companion because of cycle in initialization(see KT-13264)
    val errorTypeForLoopInSupertypes: CangJieType = createErrorType(ErrorTypeKind.CYCLIC_SUPERTYPES)

    val errorClass: ErrorClassDescriptor
        get() =
            ErrorClassDescriptor(Name.special(ErrorEntity.ERROR_CLASS.debugText.format("unknown class")))

    //    private fun isErrorClass(candidate: DeclarationDescriptor?): Boolean = candidate is ErrorClassDescriptor
    @JvmStatic
    fun createErrorType(kind: ErrorTypeKind, vararg formatParams: String): ErrorType =
        createErrorTypeWithArguments(kind, emptyList(), *formatParams)

    @JvmStatic
    fun isUninferredTypeVariable(type: CangJieType?): Boolean {
        if (type == null) return false
        val constructor = type.constructor
        return constructor is ErrorTypeConstructor && constructor.kind == ErrorTypeKind.UNINFERRED_TYPE_VARIABLE
    }

    fun containsErrorType(type: CangJieType?): Boolean {
        if (type == null) return false
        if (type.isError) return true
        for (projection in type.arguments) {
            if (!projection.isStarProjection && containsErrorType(projection.type))
                return true
        }
        return false
    }

    fun unresolvedTypeAsItIs(type: CangJieType): String {
        assert(isUnresolvedType(type))
        return (type.constructor as ErrorTypeConstructor).getParam(0)
    }

    @JvmStatic
    fun createErrorType(kind: ErrorTypeKind, typeConstructor: TypeConstructor, vararg formatParams: String): ErrorType =
        createErrorTypeWithArguments(kind, emptyList(), typeConstructor, *formatParams)

    fun createErrorTypeWithArguments(
        kind: ErrorTypeKind,
        arguments: List<TypeProjection>,
        vararg formatParams: String
    ): ErrorType =
        createErrorTypeWithArguments(kind, arguments, createErrorTypeConstructor(kind, *formatParams), *formatParams)

    @JvmStatic
    fun createErrorScope(kind: ErrorScopeKind, vararg formatParams: String): ErrorScope =
        createErrorScope(kind, throwExceptions = false, *formatParams)

    @JvmStatic
    fun createErrorScope(kind: ErrorScopeKind, throwExceptions: Boolean, vararg formatParams: String): ErrorScope =
//        if (throwExceptions) ThrowingScope(kind, *formatParams) else
        ErrorScope(kind, *formatParams)

    fun createErrorTypeConstructor(kind: ErrorTypeKind, vararg formatParams: String): ErrorTypeConstructor =
        ErrorTypeConstructor(kind, *formatParams)

    fun createErrorTypeWithArguments(
        kind: ErrorTypeKind,
        arguments: List<TypeProjection>,
        typeConstructor: TypeConstructor,
        vararg formatParams: String
    ): ErrorType = ErrorType(
        typeConstructor, createErrorScope(ErrorScopeKind.ERROR_TYPE_SCOPE, typeConstructor.toString()),
        kind, arguments, isMarkedOption = false, *formatParams
    )

    private fun isErrorClass(candidate: DeclarationDescriptor?): Boolean = candidate is ErrorClassDescriptor
    /**
     * @return true if any of the types referenced in parameter types (including type parameters and extension receiver) of the function
     * is an error type. Does not check the return type of the function.
     */
    fun containsErrorTypeInParameters(function: FunctionDescriptor): Boolean {
        val receiverParameter = function.extensionReceiverParameter
        if (receiverParameter != null && containsErrorType(receiverParameter.type))
            return true

        for (parameter in function.valueParameters) {
            if (containsErrorType(parameter.type))
                return true
        }

        for (parameter in function.typeParameters) {
            for (upperBound in parameter.upperBounds) {
                if (containsErrorType(upperBound))
                    return true
            }
        }
        return false
    }
    @JvmStatic
    fun isError(candidate: DeclarationDescriptor?): Boolean =
        candidate != null
                && (isErrorClass(candidate) || isErrorClass(candidate.containingDeclaration)
                || candidate === errorModule
                )

}
