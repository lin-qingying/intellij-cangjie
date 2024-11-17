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

package com.linqingying.cangjie.ide.navigationToolbar

import com.linqingying.cangjie.ide.AbstractCangJieIconProvider
import com.linqingying.cangjie.ide.CangJieIconProvider
import com.linqingying.cangjie.lang.CangJieLanguage
import com.linqingying.cangjie.psi.CjFile
import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension
import com.intellij.lang.Language
import com.intellij.psi.PsiElement


abstract class AbstractNavBarModelExtensionCompatBase : StructureAwareNavBarModelExtension(){

    protected abstract fun adjustElementImpl(psiElement: PsiElement?): PsiElement?
    override fun adjustElement(psiElement: PsiElement): PsiElement? =
        adjustElementImpl(psiElement)
    override val language: Language
        get() = CangJieLanguage
    override fun acceptParentFromModel(psiElement: PsiElement?): Boolean {
        if (psiElement is CjFile) {
            return AbstractCangJieIconProvider.getSingleClass(psiElement) == null
        }
        return true
    }
}
