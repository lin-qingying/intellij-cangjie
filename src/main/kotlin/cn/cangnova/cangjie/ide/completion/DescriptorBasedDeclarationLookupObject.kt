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

package cn.cangnova.cangjie.ide.completion

import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import javax.swing.Icon

interface DeclarationLookupObject : Iconable {
    val psiElement: PsiElement?
    val name: Name?


    val descriptor: DeclarationDescriptor?
}
interface DescriptorBasedDeclarationLookupObject : DeclarationLookupObject {

    override val descriptor: DeclarationDescriptor?
    val importableFqName: FqName?
    val isDeprecated: Boolean
}

data class PackageLookupObject(val fqName: FqName) : DescriptorBasedDeclarationLookupObject {
    override val psiElement: PsiElement? get() = null
    @Deprecated("Use 'descriptor' available in 'DescriptorBasedDeclarationLookupObject' instead", ReplaceWith("null"))
    override val descriptor: DeclarationDescriptor? get() = null
    override val name: Name get() = fqName.shortName()
    override val importableFqName: FqName get() = fqName
    override val isDeprecated: Boolean get() = false
    override fun getIcon(flags: Int): Icon = AllIcons.Nodes.Package
}

