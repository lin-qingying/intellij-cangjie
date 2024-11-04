package com.linqingying.cangjie.types.error

import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.ClassConstructorDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.ClassDescriptorImpl
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.storage.LockBasedStorageManager
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner

class ErrorClassDescriptor(name: Name) : ClassDescriptorImpl(
    ErrorUtils.errorModule, name, Modality.OPEN, ClassKind.CLASS, emptyList(), SourceElement.NO_SOURCE, false, LockBasedStorageManager.NO_LOCKS

) {
    override fun getMemberScope(typeArguments: List<TypeProjection>, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope =
        ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_CLASS, name.toString(), typeArguments.toString())

    override fun getMemberScope(typeSubstitution: TypeSubstitution, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope =
        ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_CLASS, name.toString(), typeSubstitution.toString())






    init {
        val errorConstructor = ClassConstructorDescriptorImpl.create(this, Annotations.EMPTY, true, SourceElement.NO_SOURCE)
            .apply {
                initialize(
                    emptyList(),
                    DescriptorVisibilities.INTERNAL
                )
            }
        val memberScope = ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_CLASS, errorConstructor.name.toString(), "")
        errorConstructor.returnType = ErrorType(
            ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.ERROR_CLASS),
            memberScope,
            ErrorTypeKind.ERROR_CLASS
        )
        initialize(memberScope, setOf(errorConstructor), errorConstructor, emptySet())
    }



    override fun substitute(substitutor: TypeSubstitutor): ClassDescriptor = this
    override fun toString(): String = name.asString()

}
