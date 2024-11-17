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

package com.linqingying.cangjie.ide.lineMarkers.markers

import com.linqingying.cangjie.highlighter.unwrapped
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.containingClass
import com.linqingying.cangjie.utils.module
import com.intellij.codeInsight.daemon.impl.GutterTooltipBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil


object CangJieGutterTooltipHelper : GutterTooltipBuilder() {
    override fun getLinkProtocol(): String {
        return "kotlinClass"
    }

    override fun shouldSkipAsFirstElement(element: PsiElement): Boolean {
        return element.unwrapped is CjCallableDeclaration
    }

    override fun getLinkReferenceText(element: PsiElement): String? {
        val moduleName = element.module?.name?.let { "$it:" } ?: ""
        val qualifiedName = when (val el = element.unwrapped) {

            is CjTypeStatement -> el.fqName?.asString()
            else -> PsiTreeUtil.getStubOrPsiParentOfType(el, CjClass::class.java)?.fqName?.asString()
        } ?: return null
        return moduleName + qualifiedName
    }

    override fun getContainingElement(element: PsiElement): PsiElement? {
        val unwrapped = element.unwrapped


        var member: CjDeclaration?
        if (unwrapped is CjParameter) {
            member = PsiTreeUtil.getStubOrPsiParentOfType(unwrapped, CjClass::class.java)
        }
        else {
            member = PsiTreeUtil.getStubOrPsiParentOfType(unwrapped, CjDeclaration::class.java)
        }
        if (member == null && unwrapped is CjDeclaration) {
            member = unwrapped.containingClass()
        }
        return member ?: unwrapped?.containingFile
    }

    override fun getLocationString(element: PsiElement): String? {
        val classOrObject = element.unwrapped as? CjTypeStatement ?: return null
        val moduleName = element.module?.name ?: return null
//        val moduleNameRequired = classOrObject.hasActualModifier() || classOrObject.isExpectDeclaration()
//        return if (moduleNameRequired) " [$moduleName]" else null
        return " [$moduleName]"
    }

    override fun getPresentableName(element: PsiElement): String? {
        return (element as? PsiNamedElement)?.name
    }




}
