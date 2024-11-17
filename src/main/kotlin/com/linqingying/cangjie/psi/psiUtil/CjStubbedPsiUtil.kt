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

package com.linqingying.cangjie.psi.psiUtil

import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjElementImplStub
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayFactory

object CjStubbedPsiUtil {
    @JvmStatic
    fun getContainingDeclaration(element: PsiElement): CjDeclaration? {
        return getPsiOrStubParent(element, CjDeclaration::class.java, true)
    }
    @JvmStatic
    fun <T : CjDeclaration> getContainingDeclaration(element: PsiElement, declarationClass: Class<T>): T? {
        return getPsiOrStubParent(element, declarationClass, true)
    }

    @JvmStatic
    fun <T : CjElement> getPsiOrStubParent(
        element: PsiElement,
        declarationClass: Class<T>,
        strict: Boolean
    ): T? {
        if (!strict && declarationClass.isInstance(element)) {
            return element as T
        }
        if (element is CjElementImplStub<*>) {
            val stub = element.stub
            if (stub != null) {
                return stub.getParentStubOfType(declarationClass)
            }
        }
        return PsiTreeUtil.getParentOfType(element, declarationClass, strict)
    }
@JvmStatic
    fun <T : CjElement> getStubOrPsiChild(
    element: CjElementImplStub<*>,
    types: TokenSet,
    factory: ArrayFactory<T?>
    ): T? {
        val typeElements = element.getStubOrPsiChildren(types, factory)
        if (typeElements.isEmpty()) {
            return null
        }
        return typeElements[0]
    }
}
