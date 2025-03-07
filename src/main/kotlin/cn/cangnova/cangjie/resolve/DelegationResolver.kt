/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.CallableMemberDescriptor
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.psi.CjPureTypeStatement
import cn.cangnova.cangjie.psi.CjTypeReference
import cn.cangnova.cangjie.resolve.lazy.DelegationFilter
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.isError

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
