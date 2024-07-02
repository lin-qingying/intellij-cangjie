package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.TypeProjection
import com.huawei.cangjie.types.TypeSubstitution
import com.huawei.cangjie.types.checker.CangJieTypeRefiner

abstract class ModuleAwareClassDescriptor : ClassDescriptor{
    protected abstract fun getMemberScope(typeSubstitution: TypeSubstitution, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope
    protected abstract fun getMemberScope(typeArguments: List<TypeProjection>, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope

    protected abstract fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope

}