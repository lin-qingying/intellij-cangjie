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

package org.cangnova.cangjie.ide


import org.cangnova.cangjie.icon.CangJieIcons.ABSTRACT_CLASS
import org.cangnova.cangjie.icon.CangJieIcons.ABSTRACT_EXTENSION_FUNCTION
import org.cangnova.cangjie.icon.CangJieIcons.CLASS
import org.cangnova.cangjie.icon.CangJieIcons.ENUM
import org.cangnova.cangjie.icon.CangJieIcons.EXTENSION_FUNCTION
import org.cangnova.cangjie.icon.CangJieIcons.FIELD_LET
import org.cangnova.cangjie.icon.CangJieIcons.FIELD_MPROP
import org.cangnova.cangjie.icon.CangJieIcons.FIELD_PROP
import org.cangnova.cangjie.icon.CangJieIcons.FIELD_VAR
import org.cangnova.cangjie.icon.CangJieIcons.FILE
import org.cangnova.cangjie.icon.CangJieIcons.FUNCTION
import org.cangnova.cangjie.icon.CangJieIcons.INTERFACE
import org.cangnova.cangjie.icon.CangJieIcons.LAMBDA
import org.cangnova.cangjie.icon.CangJieIcons.PARAMETER
import org.cangnova.cangjie.icon.CangJieIcons.STRUCT
import org.cangnova.cangjie.icon.CangJieIcons.TYPE_ALIAS
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.psiUtil.isAbstract
import org.cangnova.cangjie.psi.psiUtil.isPrivate
import org.cangnova.cangjie.utils.toCamelCase
import com.intellij.icons.AllIcons
import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.ui.RowIcon
import com.intellij.util.PlatformIcons
import org.cangnova.cangjie.decompiler.psi.file.CjDecompiledFile

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

    }


    override fun getIcon(psiElement: PsiElement, flags: Int): Icon? {
        if(psiElement is CjDecompiledFile){
            return FILE
        }
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


            is CjParameter -> {
                if (CjPsiUtil.getClassIfParameterIsProperty(this) != null) {
                    if (isMutable) FIELD_VAR else FIELD_LET
                } else
                    PARAMETER
            }

            is CjVariable<*> -> if (isVar) FIELD_VAR else FIELD_LET

            is CjProperty -> if (isVar) FIELD_MPROP else FIELD_PROP


            is CjTypeAlias -> TYPE_ALIAS


            else -> getBaseIconUnwrapped()
        }

        private fun PsiElement.getBaseIconUnwrapped(): Icon? =  takeIf { it != this }?.getBaseIcon()
    }
}
