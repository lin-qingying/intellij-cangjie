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

package com.linqingying.cangjie.ide

import com.linqingying.cangjie.highlighter.unwrapped
import com.linqingying.cangjie.icon.CangJieIcons.ABSTRACT_CLASS
import com.linqingying.cangjie.icon.CangJieIcons.ABSTRACT_EXTENSION_FUNCTION
import com.linqingying.cangjie.icon.CangJieIcons.CLASS
import com.linqingying.cangjie.icon.CangJieIcons.ENUM
import com.linqingying.cangjie.icon.CangJieIcons.EXTENSION_FUNCTION
import com.linqingying.cangjie.icon.CangJieIcons.FIELD_LET
import com.linqingying.cangjie.icon.CangJieIcons.FIELD_MPROP
import com.linqingying.cangjie.icon.CangJieIcons.FIELD_PROP
import com.linqingying.cangjie.icon.CangJieIcons.FIELD_VAR
import com.linqingying.cangjie.icon.CangJieIcons.FILE
import com.linqingying.cangjie.icon.CangJieIcons.FUNCTION
import com.linqingying.cangjie.icon.CangJieIcons.INTERFACE
import com.linqingying.cangjie.icon.CangJieIcons.LAMBDA
import com.linqingying.cangjie.icon.CangJieIcons.PARAMETER
import com.linqingying.cangjie.icon.CangJieIcons.STRUCT
import com.linqingying.cangjie.icon.CangJieIcons.TYPE_ALIAS
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.linqingying.cangjie.psi.psiUtil.isAbstract
import com.linqingying.cangjie.psi.psiUtil.isPrivate
import com.intellij.icons.AllIcons
import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.ui.RowIcon
import com.intellij.util.PlatformIcons
import com.linqingying.utils.toCamelCase
import javax.swing.Icon


class CangJieIconProvider : AbstractCangJieIconProvider() {
//    override fun isMatchingExpected(declaration: CjDeclaration): Boolean {
//        return declaration.hasActualModifier() && declaration.hasMatchingExpected()
//    }
}

abstract class AbstractCangJieIconProvider : IconProvider(), DumbAware {
//    protected abstract fun isMatchingExpected(declaration: CjDeclaration): Boolean

    private fun Icon.addExpectActualMarker(element: PsiElement): Icon {
        return this
//        val declaration = (element as? CjNamedDeclaration) ?: return this
//        val additionalIcon = when {
////            isExpectDeclaration(declaration) -> EXPECT
////            isMatchingExpected(declaration) -> ACTUAL
//            else -> return this
//        }
//        return RowIcon(2).apply {
//            setIcon(this@addExpectActualMarker, 0)
//            setIcon(additionalIcon, 1)
//        }
    }


    override fun getIcon(psiElement: PsiElement, flags: Int): Icon? {
        if (psiElement is CjFile) {

            val mainClass = getSingleClass(psiElement)
            return if (mainClass != null) getIcon(mainClass, flags) else FILE
        }

        val result = psiElement.getBaseIcon()
        if (flags and Iconable.ICON_FLAG_VISIBILITY > 0 && result != null && (psiElement is CjModifierListOwner && psiElement !is CjClassInitializer)) {
            val list = psiElement.modifierList
            val visibilityIcon = getVisibilityIcon(list)

            val withExpectedActual: Icon = try {
                result.addExpectActualMarker(psiElement)
            } catch (indexNotReady: IndexNotReadyException) {
                result
            }

            return createRowIcon(withExpectedActual, visibilityIcon)
        }
        return result
    }

    companion object {
        fun isSingleClassFile(file: CjFile) = getSingleClass(file) != null

        fun getSingleClass(file: CjFile): CjTypeStatement? {
            var targetDeclaration: CjDeclaration? = null
            for (declaration: CjDeclaration in file.declarations) {
                if (!declaration.isPrivate() && declaration !is CjTypeAlias) {
                    if (targetDeclaration != null) return null
                    targetDeclaration = declaration
                }
            }
            return targetDeclaration?.takeIf { it is CjTypeStatement && StringUtil.getPackageName(file.name.toCamelCase()) == it.name } as? CjTypeStatement
        }

        fun getMainClass(file: CjFile): CjTypeStatement? {
            var targetClassOrObject: CjTypeStatement? = null
            for (declaration in file.declarations) {
                if (!declaration.isPrivate() && declaration is CjTypeStatement) {
                    if (targetClassOrObject != null) return null
                    targetClassOrObject = declaration
                }
            }
            return targetClassOrObject?.takeIf { StringUtil.getPackageName(file.name) == it.name }
        }

        private fun createRowIcon(baseIcon: Icon, visibilityIcon: Icon): RowIcon {
            val rowIcon = RowIcon(2)
            rowIcon.setIcon(baseIcon, 0)
            rowIcon.setIcon(visibilityIcon, 1)
            return rowIcon
        }

        fun getVisibilityIcon(list: CjModifierList?): Icon {
            val icon: Icon? = if (list != null) {
                when {
                    list.hasModifier(CjTokens.PRIVATE_KEYWORD) -> AllIcons.Nodes.C_private
                    list.hasModifier(CjTokens.PROTECTED_KEYWORD) -> AllIcons.Nodes.C_protected
                    list.hasModifier(CjTokens.INTERNAL_KEYWORD) -> AllIcons.Nodes.C_plocal
                    else -> null
                }
            } else {
                null
            }

            return icon ?: PlatformIcons.PUBLIC_ICON
        }


        fun PsiElement.getBaseIcon(): Icon? = when (this) {
            is CjPackageDirective -> AllIcons.Nodes.Package
            is CjFile -> FILE

            is CjNamedFunction -> when {
                receiverTypeReference != null ->
                    if (CjPsiUtil.isAbstract(this)) ABSTRACT_EXTENSION_FUNCTION else EXTENSION_FUNCTION

                getStrictParentOfType<CjNamedDeclaration>() is CjClass ->
                    if (CjPsiUtil.isAbstract(this)) PlatformIcons.ABSTRACT_METHOD_ICON else
                        AllIcons.Nodes.Method

                else ->
                    FUNCTION
            }

            is CjConstructor<*> -> AllIcons.Nodes.Method


            is CjFunctionLiteral -> LAMBDA
            is CjInterface -> INTERFACE
            is CjEnum -> ENUM
            is CjStruct -> STRUCT
            is CjClass -> if (isAbstract()) ABSTRACT_CLASS else CLASS
            is CjEnumEntry -> if (getPrimaryConstructorParameterList() == null) ENUM else null


            is CjParameter -> {
                if (CjPsiUtil.getClassIfParameterIsProperty(this) != null) {
                    if (isMutable) FIELD_VAR else FIELD_LET
                } else
                    PARAMETER
            }

            is CjVariable -> if (isVar) FIELD_VAR else FIELD_LET

            is CjProperty -> if (isVar) FIELD_MPROP else FIELD_PROP


            is CjTypeAlias -> TYPE_ALIAS


            else -> getBaseIconUnwrapped()
        }

        private fun PsiElement.getBaseIconUnwrapped(): Icon? = unwrapped?.takeIf { it != this }?.getBaseIcon()
    }
}
