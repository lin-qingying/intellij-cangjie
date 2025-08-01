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

package cn.cangnova.cangjie.descriptors.impl

import cn.cangnova.cangjie.descriptors.DeclarationDescriptorVisitor
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import cn.cangnova.cangjie.descriptors.SourceElement
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.name.FqName

abstract class PackageFragmentDescriptorImpl(
    module: ModuleDescriptor,
    final override val fqName: FqName
) : DeclarationDescriptorNonRootImpl(module, Annotations.EMPTY, fqName.shortNameOrSpecial(), SourceElement.NO_SOURCE),
    PackageFragmentDescriptor {
    // Not inlined in order to not capture ref on 'module'
    private val debugString: String = "package $fqName of $module"
    override val declarationProvider: DeclarationProvider = DeclarationProvider.EMPTY

    override val containingDeclaration: ModuleDescriptor
        get() = super.containingDeclaration as ModuleDescriptor

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R {
        return visitor.visitPackageFragmentDescriptor(this, data!!)

    }


    override val source: SourceElement = SourceElement.NO_SOURCE
    override fun toString(): String = debugString
}