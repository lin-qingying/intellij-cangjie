/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.builtins.BuiltInsPackageFragment
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.storage.StorageManager

interface ClassDescriptorFactory {
    fun shouldCreateClass(packageFqName: FqName, name: Name): Boolean
    fun createEnum(classId: ClassId): EnumDescriptor?

    fun createClass(classId: ClassId): ClassDescriptor?

    // Note: do not rely on this function to return _all_ classes. Some factories can not enumerate all classes that they're able to create
    fun getAllContributedClassesIfPossible(packageFqName: FqName): Collection<ClassDescriptor>
}

class CangJieBuiltInClassDescriptorFactory(
    storageManager: StorageManager,
    private val moduleDescriptor: ModuleDescriptor,
    private val computeContainingDeclaration: (ModuleDescriptor) -> DeclarationDescriptor = { module ->
        module.getPackage(CANGJIE_FQ_NAME).fragments.filterIsInstance<BuiltInsPackageFragment>().first()
    }
) : ClassDescriptorFactory {

    override fun shouldCreateClass(packageFqName: FqName, name: Name): Boolean = false

    override fun createEnum(classId: ClassId): EnumDescriptor? = null

    override fun createClass(classId: ClassId): ClassDescriptor? = null

    override fun getAllContributedClassesIfPossible(packageFqName: FqName): Collection<ClassDescriptor> = emptySet()

    companion object {
        private val CANGJIE_FQ_NAME = StandardNames.BASIC_PACKAGE_FQ_NAME
    }
}
