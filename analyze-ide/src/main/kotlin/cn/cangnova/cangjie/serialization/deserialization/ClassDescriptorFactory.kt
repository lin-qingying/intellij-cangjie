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

package cn.cangnova.cangjie.serialization.deserialization

import cn.cangnova.cangjie.builtins.BuiltInsPackageFragment
import cn.cangnova.cangjie.builtins.StandardNames
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.descriptors.impl.ClassDescriptorImpl
import cn.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.resolve.descriptorUtil.builtIns
import cn.cangnova.cangjie.resolve.scopes.GivenFunctionsMemberScope
import cn.cangnova.cangjie.storage.getValue

interface ClassDescriptorFactory {
    fun shouldCreateClass(packageFqName: FqName, name: Name): Boolean

    fun createClass(classId: ClassId): ClassDescriptor?

    // Note: do not rely on this function to return _all_ classes. Some factories can not enumerate all classes that they're able to create
    fun getAllContributedClassesIfPossible(packageFqName: FqName): Collection<ClassDescriptor>
}

class CloneableClassScope(
    storageManager: StorageManager,
    containingClass: ClassDescriptor
) : GivenFunctionsMemberScope(storageManager, containingClass) {
    override fun computeDeclaredFunctions(): List<FunctionDescriptor> = listOf(
        SimpleFunctionDescriptorImpl.create(containingClass, Annotations.EMPTY, CLONE_NAME, CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE).apply {
            initialize(
                null, containingClass.thisAsReceiverParameter, emptyList(), emptyList(), emptyList(), containingClass.builtIns.anyType,
                Modality.OPEN, DescriptorVisibilities.PROTECTED
            )
        }
    )

    companion object {
        val CLONE_NAME = Name.identifier("clone")
    }
}

class CangJieBuiltInClassDescriptorFactory(
    storageManager: StorageManager,
    private val moduleDescriptor: ModuleDescriptor,
    private val computeContainingDeclaration: (ModuleDescriptor) -> DeclarationDescriptor = { module ->
        module.getPackage(CANGJIE_FQ_NAME).fragments.filterIsInstance<BuiltInsPackageFragment>().first()
    }
) : ClassDescriptorFactory {
    private val cloneable by storageManager.createLazyValue {
        ClassDescriptorImpl(
            computeContainingDeclaration(moduleDescriptor),
            CLONEABLE_NAME, Modality.ABSTRACT, ClassKind.INTERFACE, listOf(moduleDescriptor.builtIns.anyType),
            SourceElement.NO_SOURCE, false, storageManager
        ).apply {
            initialize(CloneableClassScope(storageManager, this), emptySet(), null, emptySet())
        }
    }

    override fun shouldCreateClass(packageFqName: FqName, name: Name): Boolean =
        name == CLONEABLE_NAME && packageFqName == CANGJIE_FQ_NAME

    override fun createClass(classId: ClassId): ClassDescriptor? =
        when (classId) {
            CLONEABLE_CLASS_ID -> cloneable
            else -> null
        }

    override fun getAllContributedClassesIfPossible(packageFqName: FqName): Collection<ClassDescriptor> =
        when (packageFqName) {
            CANGJIE_FQ_NAME -> setOf(cloneable)
            else -> emptySet()
        }

    companion object {
        private val CANGJIE_FQ_NAME = StandardNames.BUILT_INS_PACKAGE_FQ_NAME
        private val CLONEABLE_NAME = StandardNames.FqNames.cloneable.shortName()
        val CLONEABLE_CLASS_ID = ClassId.topLevel(StandardNames.FqNames.cloneable.toSafe())
    }
}
