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

package org.cangnova.cangjie.types.error

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.DefaultBuiltIns

object ErrorModuleDescriptor: ModuleDescriptor {
    override val isValid: Boolean = false
    override fun getPackage(fqName: FqName): PackageViewDescriptor  = throw IllegalStateException("Should not be called!")
    override val builtIns: CangJieBuiltIns  by lazy { DefaultBuiltIns }
    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> = emptyList()
    override val expectedByModules: List<ModuleDescriptor> = emptyList()

    override fun assertValid() = throw InvalidModuleException("ERROR_MODULE is not a valid module")
    override fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean =  false
    override fun shouldProtectedsOf(targetModule: ModuleDescriptor): Boolean  = false
    override fun <T> getCapability(capability: ModuleCapability<T>): T? = null

    override val original: DeclarationDescriptor = this
    override val containingDeclaration: DeclarationDescriptor? = null
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R?  = null

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {

    }
    override val stableName: Name = Name.special(ErrorEntity.ERROR_MODULE.debugText)

    override val annotations: Annotations
        get() = Annotations.EMPTY
    override val name: Name = stableName
}
