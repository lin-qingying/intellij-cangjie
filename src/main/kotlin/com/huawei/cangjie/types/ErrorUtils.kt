package com.huawei.cangjie.types

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.types.error.*

object ErrorUtils {
//    private val errorVariable: VariableDescriptor = ErrorVariableDescriptor()
//    val errorVariableGroup: Set<VariableDescriptor> = setOf(errorVariable)

    val errorModule: ModuleDescriptor = ErrorModuleDescriptor
    val errorVariableType: CangJieType = createErrorType(ErrorTypeKind.ERROR_PROPERTY_TYPE)

    // Do not move it into AbstractTypeConstructor.Companion because of cycle in initialization(see KT-13264)
    val errorTypeForLoopInSupertypes: CangJieType = createErrorType(ErrorTypeKind.CYCLIC_SUPERTYPES)

    val errorClass: ErrorClassDescriptor =
        ErrorClassDescriptor(Name.special(ErrorEntity.ERROR_CLASS.debugText.format("unknown class")))

    //    private fun isErrorClass(candidate: DeclarationDescriptor?): Boolean = candidate is ErrorClassDescriptor
    @JvmStatic
    fun createErrorType(kind: ErrorTypeKind, vararg formatParams: String): ErrorType =
        createErrorTypeWithArguments(kind, emptyList(), *formatParams)

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
        kind, arguments, isMarkedNullable = false, *formatParams
    )

    private fun isErrorClass(candidate: DeclarationDescriptor?): Boolean = candidate is ErrorClassDescriptor

    @JvmStatic
    fun isError(candidate: DeclarationDescriptor?): Boolean =
        candidate != null
                && (isErrorClass(candidate) || isErrorClass(candidate.containingDeclaration)
                || candidate === errorModule
                )

}
