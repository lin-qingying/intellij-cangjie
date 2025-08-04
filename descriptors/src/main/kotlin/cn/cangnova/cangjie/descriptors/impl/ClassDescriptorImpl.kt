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
package cn.cangnova.cangjie.descriptors.impl

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.scopes.InstanceMemberScope
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.resolve.scopes.StaticMemberScope
import cn.cangnova.cangjie.storage.StorageManager
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.ClassTypeConstructorImpl
import cn.cangnova.cangjie.types.TypeConstructor
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner


open class ClassDescriptorImpl(
    containingDeclaration: DeclarationDescriptor,
    name: Name,
    override val modality: Modality,
    override val kind: ClassKind,
    supertypes: MutableCollection<CangJieType?>,
    source: SourceElement,
    isExternal: Boolean,
    storageManager: StorageManager
) : ClassDescriptorBase(storageManager, containingDeclaration, name, source) {

    override val typeConstructor: TypeConstructor =
        ClassTypeConstructorImpl(this, mutableListOf(), supertypes, storageManager)

    override lateinit var unsubstitutedMemberScope: MemberScope
    override lateinit var constructors: MutableSet<ClassConstructorDescriptor>
    override var unsubstitutedPrimaryConstructor: ClassConstructorDescriptor? = null

    override lateinit var endConstructors: MutableSet<ClassConstructorDescriptor>

    init {
        assert(modality != Modality.SEALED) { "Implement getSealedSubclasses() for this class: " + javaClass }


    }

    public override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {
        return unsubstitutedMemberScope
    }

    fun initialize(
        unsubstitutedMemberScope: MemberScope,
        constructors: MutableSet<ClassConstructorDescriptor>,
        primaryConstructor: ClassConstructorDescriptor?,
        endConstructors: MutableSet<ClassConstructorDescriptor>
    ) {
        this.unsubstitutedMemberScope = unsubstitutedMemberScope
        this.constructors = constructors
        this.unsubstitutedPrimaryConstructor = primaryConstructor
        this.endConstructors = endConstructors
    }


    override val staticScope: MemberScope
        get() = StaticMemberScope(unsubstitutedMemberScope)

    override val instanceScope: MemberScope
        get() = InstanceMemberScope(unsubstitutedMemberScope)


    override val visibility: DescriptorVisibility
        get() = DescriptorVisibilities.PUBLIC


    override fun toString(): String {
        return "class " + name
    }

    override val declaredTypeParameters: List<TypeParameterDescriptor>
        get() = mutableListOf()

    override fun getSealedSubclasses(): MutableCollection<ClassDescriptor> {
        return mutableListOf()
    }


    override val annotations: Annotations
        get() = Annotations.EMPTY
}
