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

package com.linqingying.cangjie.ide.codeinsight

import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.psi.CjNamedFunction
import com.linqingying.cangjie.renderer.DescriptorRenderer.Companion.SHORT_NAMES_IN_TYPES
import com.linqingying.cangjie.resolve.caches.unsafeResolveToDescriptor
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil

class CjFunctionPsiElementCellRenderer : PsiTargetPresentationRenderer<PsiElement>() {
    override fun getElementText(element: PsiElement): String {
        if (element is CjNamedFunction) {
            val descriptor: DeclarationDescriptor =
                element.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL)
            return SHORT_NAMES_IN_TYPES.render(descriptor) //NON-NLS
        }
        return super.getElementText(element)
    }

    override fun getPresentation(element: PsiElement): TargetPresentation {
        return TargetPresentation.builder(getElementText(element))
            .containerText(SymbolPresentationUtil.getSymbolContainerText(element))
            .icon(getIcon(element))
            .presentation()
    }
}
