package com.huawei.cangjie.descriptors.impl

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.resolve.scopes.MemberScope
import com.huawei.cangjie.types.TypeProjection
import com.huawei.cangjie.types.TypeSubstitution
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.descriptors.impl.ModuleAwareClassDescriptor.Companion.getRefinedMemberScopeIfPossible
import com.huawei.cangjie.descriptors.impl.ModuleAwareClassDescriptor.Companion.getRefinedUnsubstitutedMemberScopeIfPossible

abstract class ModuleAwareClassDescriptor : ClassDescriptor{
    abstract fun getMemberScope(typeSubstitution: TypeSubstitution, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope
    abstract fun getMemberScope(typeArguments: List<TypeProjection>, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope

    abstract fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope

    companion object {
        internal fun ClassDescriptor.getRefinedUnsubstitutedMemberScopeIfPossible(
            kotlinTypeRefiner: CangJieTypeRefiner
        ): MemberScope =
            (this as? ModuleAwareClassDescriptor)?.getUnsubstitutedMemberScope(kotlinTypeRefiner) ?: this.unsubstitutedMemberScope

        internal fun ClassDescriptor.getRefinedMemberScopeIfPossible(
            typeSubstitution: TypeSubstitution,
            kotlinTypeRefiner: CangJieTypeRefiner
        ): MemberScope =
            (this as? ModuleAwareClassDescriptor)?.getMemberScope(typeSubstitution, kotlinTypeRefiner) ?: this.getMemberScope(
                typeSubstitution
            )
    }

}
fun ClassDescriptor.getRefinedUnsubstitutedMemberScopeIfPossible(
    kotlinTypeRefiner: CangJieTypeRefiner
): MemberScope = getRefinedUnsubstitutedMemberScopeIfPossible(kotlinTypeRefiner)

fun ClassDescriptor.getRefinedMemberScopeIfPossible(
    typeSubstitution: TypeSubstitution,
    kotlinTypeRefiner: CangJieTypeRefiner
): MemberScope = getRefinedMemberScopeIfPossible(typeSubstitution, kotlinTypeRefiner)
