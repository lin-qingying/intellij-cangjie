/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.descriptors.extend

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.resolve.scopes.SubstitutingScope
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*

abstract class AbstractExtendDescriptor(

    private val storageManager: StorageManager,
) : ExtendDescriptor {

    override fun <R, D> accept(
        visitor: DeclarationDescriptorVisitor<R, D>,
        data: D?
    ): R? {
        return visitor.visitExtendDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitExtendDescriptor(this, null)
    }


    override val extendTypeConstructor: TypeConstructor
        get() = extendType.constructor


    override fun getMemberScope(
        typeArguments: List<TypeProjection>,

        ): MemberScope {
        assert(typeArguments.size == declaredTypeParameters.size) {
            "Illegal number of type arguments: expected ${declaredTypeParameters.size} but was ${typeArguments.size} for   ${declaredTypeParameters}"
        }
        if (typeArguments.isEmpty()) return unsubstitutedMemberScope

        val substitutor = TypeConstructorSubstitution.create(extendType.constructor, typeArguments).buildSubstitutor()
        return SubstitutingScope(unsubstitutedMemberScope, substitutor)
    }

    override fun getMemberScope(
        typeSubstitution: TypeSubstitution,

        ): MemberScope {
        if (typeSubstitution.isEmpty()) return unsubstitutedMemberScope

        val substitutor = TypeSubstitutor.create(typeSubstitution)
        return SubstitutingScope(unsubstitutedMemberScope, substitutor)
    }

    override val staticScope: MemberScope
        get() = MemberScope.Empty

    override val visibility: DescriptorVisibility
        get() = DescriptorVisibilities.PUBLIC
    override val original: ExtendDescriptor
        get() = this

}

class ExtendDescriptorImpl(
    override val extendType: CangJieType,
    override val superTypes: List<CangJieType>,
    override val declaredTypeParameters: List<TypeParameterDescriptor>,
    override val extendId: String,
    override val containingDeclaration: DeclarationDescriptor,
    storageManager: StorageManager,

    ) : AbstractExtendDescriptor(

    storageManager
) {
    override val unsubstitutedMemberScope: MemberScope
        get() = TODO("Not yet implemented")


}