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

package org.cangnova.cangjie.serialization.deserialization.descriptors

import org.cangnova.cangjie.descriptors.ClassConstructorDescriptor
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DeserializedDescriptor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.ScopesHolderForClass
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.descriptors.impl.AbstractClassDescriptor
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.Decl
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.serialization.deserialization.DeserializationContext
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner


class DeserializedClassDescriptor(
    outerContext: DeserializationContext,
    val `class`: Decl,

    val metadataVersion: BinaryVersion,
    override val source: SourceElement
) : AbstractClassDescriptor(
    outerContext.storageManager,
    `class`.name
), DeserializedDescriptor {
    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {
        TODO("Not yet implemented")
    }

    override val staticScope: MemberScope
        get() = TODO("Not yet implemented")
    override val constructors: Collection<ClassConstructorDescriptor>
        get() = TODO("Not yet implemented")
    override val endConstructors: Collection<ClassConstructorDescriptor>
        get() = TODO("Not yet implemented")
    override val containingDeclaration: DeclarationDescriptor
        get() = TODO("Not yet implemented")
    override val kind: ClassKind
        get() = TODO("Not yet implemented")
    override val modality: Modality
        get() = TODO("Not yet implemented")
    override val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor?
        get() = TODO("Not yet implemented")
    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() = TODO("Not yet implemented")
    override val sealedSubclasses: Collection<ClassDescriptor>
        get() = TODO("Not yet implemented")
    override val typeConstructor: TypeConstructor
        get() = TODO("Not yet implemented")

}
