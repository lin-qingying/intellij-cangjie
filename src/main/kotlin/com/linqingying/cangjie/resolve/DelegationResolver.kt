package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.psi.CjPureTypeStatement
import com.linqingying.cangjie.psi.CjTypeReference
import com.linqingying.cangjie.resolve.lazy.DelegationFilter
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.isError

//委托
class DelegationResolver<T : CallableMemberDescriptor> private constructor(
    private val classOrObject: CjPureTypeStatement,
    private val ownerDescriptor: ClassDescriptor,
    private val existingMembers: Collection<CallableDescriptor>,
    private val trace: BindingTrace,
    private val memberExtractor: MemberExtractor<T>,
    private val typeResolver: TypeResolver,
    private val delegationFilter: DelegationFilter,
    private val languageVersionSettings: LanguageVersionSettings
) {

//
//    private fun generateDelegatedMembers(): Collection<T> {
//        val delegatedMembers = hashSetOf<T>()
//        for (delegationSpecifier in classOrObject.superTypeListEntries) {
//            if (delegationSpecifier !is CjDelegatedSuperTypeEntry) {
//                continue
//            }
//            val typeReference = delegationSpecifier.typeReference ?: continue
//            val delegatedInterfaceType = typeResolver.resolve(typeReference)
//            if (delegatedInterfaceType == null || delegatedInterfaceType.isError) {
//                continue
//            }
//            val delegatesForInterface = generateDelegatesForInterface(delegatedMembers, delegatedInterfaceType)
//            delegatedMembers.addAll(delegatesForInterface)
//        }
//        return delegatedMembers
//    }
    interface TypeResolver {
        fun resolve(reference: CjTypeReference): CangJieType?
    }

    interface MemberExtractor<out T : CallableMemberDescriptor> {
        fun getMembersByType(type: CangJieType): Collection<T>
    }

    companion object {

//        fun <T : CallableMemberDescriptor> generateDelegatedMembers(
//            classOrObject: CjPureTypeStatement,
//            ownerDescriptor: ClassDescriptor,
//            existingMembers: Collection<CallableDescriptor>,
//            trace: BindingTrace,
//            memberExtractor: MemberExtractor<T>,
//            typeResolver: TypeResolver,
//            delegationFilter: DelegationFilter,
//            languageVersionSettings: LanguageVersionSettings
//        ): Collection<T> =
//            DelegationResolver(
//                classOrObject,
//                ownerDescriptor,
//                existingMembers,
//                trace,
//                memberExtractor,
//                typeResolver,
//                delegationFilter,
//                languageVersionSettings
//            )
//                .generateDelegatedMembers()
    }
}
