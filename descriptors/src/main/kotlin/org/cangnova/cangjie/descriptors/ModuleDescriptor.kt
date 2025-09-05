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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

class ModuleCapability<T>(val name: String) {
    override fun toString() = name
}

interface ModuleDescriptor : DeclarationDescriptor{
    val isValid: Boolean
    fun getPackage(fqName: FqName): PackageViewDescriptor
    val builtIns: CangJieBuiltIns
    fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName>
    override val containingDeclaration: DeclarationDescriptor?
        get() = null
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return visitor.visitModuleDeclaration(this, data!!)

    }
    val expectedByModules: List<ModuleDescriptor>
    /**
     * Stable name of *CangJie* module. Can be used for ABI (e.g. for mangling of declarations)
     */
    val stableName: Name?
    fun assertValid()
    fun shouldSeeInternalsOf(targetModule: ModuleDescriptor): Boolean
    fun shouldProtectedsOf(targetModule: ModuleDescriptor): Boolean

    fun <T> getCapability(capability: ModuleCapability<T>): T?
}
