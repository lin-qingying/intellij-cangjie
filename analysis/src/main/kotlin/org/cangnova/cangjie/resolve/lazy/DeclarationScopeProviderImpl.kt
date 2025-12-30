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

package org.cangnova.cangjie.resolve.lazy

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.descriptors.ClassDescriptorWithResolutionScopes
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.CjStubbedPsiUtil.getContainingDeclaration
import org.cangnova.cangjie.psi.psiUtil.CjStubbedPsiUtil.getPsiOrStubParent
import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfoFactory
import org.cangnova.cangjie.resolve.scopes.LexicalScope

open class DeclarationScopeProviderImpl(
    private val lazyDeclarationResolver: LazyDeclarationResolver,
    private val fileScopeProvider: FileScopeProvider
) : DeclarationScopeProvider {

    override fun getResolutionScopeForDeclaration(elementOfDeclaration: PsiElement): LexicalScope {
        var cjDeclaration = getPsiOrStubParent(elementOfDeclaration, CjDeclaration::class.java, false)
        require(elementOfDeclaration !is CjDeclaration || cjDeclaration === elementOfDeclaration) {
            "For CjDeclaration element getParentOfType() should return itself."
        }
        requireNotNull(cjDeclaration) { "Should be contained inside declaration." }

        var parentDeclaration = getContainingDeclaration(cjDeclaration)
        if (cjDeclaration is CjPropertyAccessor) {
            parentDeclaration = parentDeclaration?.let { getContainingDeclaration(it, CjDeclaration::class.java) }
        }

        if (parentDeclaration == null) {
            return fileScopeProvider.getFileResolutionScope(elementOfDeclaration.containingFile as CjFile)
        }

        if (parentDeclaration is CjTypeStatement) {
            val parentClassDescriptor = lazyDeclarationResolver.getClassDescriptor(
                parentDeclaration,
                NoLookupLocation.MATCH_GET_DECLARATION_SCOPE
            ) as ClassDescriptorWithResolutionScopes

            if (cjDeclaration is CjProperty ||
                cjDeclaration is CjVariable<*>
            ) {
                return parentClassDescriptor.scopeForInitializerResolution
            }

            return parentClassDescriptor.scopeForMemberDeclarationResolution
        }

        error(
            "Don't call this method for local declarations: $cjDeclaration\n" +
                    getElementTextWithContext(cjDeclaration)
        )
    }

    override fun getOuterDataFlowInfoForDeclaration(elementOfDeclaration: PsiElement): DataFlowInfo =
        DataFlowInfoFactory.EMPTY
}
