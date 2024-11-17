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

package com.linqingying.cangjie.descriptors.impl

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.types.TypeProjection
import com.linqingying.cangjie.types.TypeSubstitution
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.descriptors.impl.ModuleAwareClassDescriptor.Companion.getRefinedMemberScopeIfPossible
import com.linqingying.cangjie.descriptors.impl.ModuleAwareClassDescriptor.Companion.getRefinedUnsubstitutedMemberScopeIfPossible

abstract class ModuleAwareClassDescriptor : ClassDescriptor{
    abstract fun getMemberScope(typeSubstitution: TypeSubstitution, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope
    abstract fun getMemberScope(typeArguments: List<TypeProjection>, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope

    abstract fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope

    companion object {
        internal fun ClassDescriptor.getRefinedUnsubstitutedMemberScopeIfPossible(
            cangjieTypeRefiner: CangJieTypeRefiner
        ): MemberScope =
            (this as? ModuleAwareClassDescriptor)?.getUnsubstitutedMemberScope(cangjieTypeRefiner) ?: this.unsubstitutedMemberScope

        internal fun ClassDescriptor.getRefinedMemberScopeIfPossible(
            typeSubstitution: TypeSubstitution,
            cangjieTypeRefiner: CangJieTypeRefiner
        ): MemberScope =
            (this as? ModuleAwareClassDescriptor)?.getMemberScope(typeSubstitution, cangjieTypeRefiner) ?: this.getMemberScope(
                typeSubstitution
            )
    }

}
fun ClassDescriptor.getRefinedUnsubstitutedMemberScopeIfPossible(
    cangjieTypeRefiner: CangJieTypeRefiner
): MemberScope = getRefinedUnsubstitutedMemberScopeIfPossible(cangjieTypeRefiner)

fun ClassDescriptor.getRefinedMemberScopeIfPossible(
    typeSubstitution: TypeSubstitution,
    cangjieTypeRefiner: CangJieTypeRefiner
): MemberScope = getRefinedMemberScopeIfPossible(typeSubstitution, cangjieTypeRefiner)
