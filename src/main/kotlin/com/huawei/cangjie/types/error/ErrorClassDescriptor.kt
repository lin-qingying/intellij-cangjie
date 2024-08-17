package com.huawei.cangjie.types.error

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.ClassDescriptorImpl
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.storage.LockBasedStorageManager
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeRefiner

class ErrorClassDescriptor(name: Name) : ClassDescriptorImpl(
    ErrorUtils.errorModule, name, Modality.OPEN, ClassKind.CLASS, emptyList(), SourceElement.NO_SOURCE, false, LockBasedStorageManager.NO_LOCKS

) {
    override fun getMemberScope(typeArguments: List<TypeProjection>, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope =
        ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_CLASS, name.toString(), typeArguments.toString())

    override fun getMemberScope(typeSubstitution: TypeSubstitution, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope =
        ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_CLASS, name.toString(), typeSubstitution.toString())










    override fun substitute(substitutor: TypeSubstitutor): ClassDescriptor = this
    override fun toString(): String = name.asString()

}
