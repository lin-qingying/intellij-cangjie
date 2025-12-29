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

package org.cangnova.cangjie.ide.liveTemplates

import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore

abstract class CangJieTemplateContextType(@NlsContexts.Label presentableName: String) :
    TemplateContextType(presentableName) {

    protected abstract fun isInContext(element: PsiElement): Boolean
    open fun isCommentInContext(): Boolean {
        return false
    }


    @Deprecated("Deprecated in Java")
    @Suppress("deprecation")
    override fun isInContext(file: PsiFile, offset: Int): Boolean {

        if (!PsiUtilCore.getLanguageAtOffset(file, offset)
                .isKindOf(CangJieLanguage)
        ) {
            return false
        }
        var element = file.findElementAt(offset)
        if (element == null) {
            element = file.findElementAt(offset - 1)
        }
        if (element is PsiWhiteSpace) {
            return false
        } else if (PsiTreeUtil.getParentOfType<PsiComment?>(
                element,
                PsiComment::class.java, false
            ) != null
        ) {
            return isCommentInContext()
        } else if (PsiTreeUtil.getParentOfType(element, CjPackageDirective::class.java) != null
            || PsiTreeUtil.getParentOfType(element, CjImportItem::class.java) != null
        ) {
            return false
        } else if (element is LeafPsiElement) {
            val elementType = element.elementType
            if (elementType === CjTokens.IDENTIFIER) {
                val parent = element.getParent()
                if (parent is CjReferenceExpression) {
                    val parentOfParent = parent.parent
                    val qualifiedExpression: CjQualifiedExpression? = PsiTreeUtil.getParentOfType(
                        element,
                        CjQualifiedExpression::class.java
                    )
                    if (qualifiedExpression != null && qualifiedExpression.selectorExpression === parentOfParent) {
                        return false
                    }
                }
            }
        }
        return element != null && isInContext(element)
    }
    internal class Generic :
        CangJieTemplateContextType(CangJieLiveTemplatesBundle.message("template.context.type.generic")) {
        override fun isInContext(element: PsiElement): Boolean {
            return true
        }

        override fun isCommentInContext(): Boolean {
            return true
        }
    }


    internal class TopLevel :
        CangJieTemplateContextType(CangJieLiveTemplatesBundle.message("template.context.type.top.level")) {
        override fun isInContext(element: PsiElement): Boolean {
            var e: PsiElement? = element
            while (e != null) {
                if (e is CjModifierList) {
                    // skip property/function/class or object which is owner of modifier list
                    e = e.parent
                    if (e != null) {
                        e = e.parent
                    }
                    continue
                }
                if (e is CjVariable<*>  || e is CjNamedFunction || e is CjTypeStatement) {
                    return false
                }

                e = e.parent
            }
            return true
        }
    }


    internal class Class :
        CangJieTemplateContextType(CangJieLiveTemplatesBundle.message("template.context.type.class")) {
        override fun isInContext(element: PsiElement): Boolean {
            return getParentClassOrStruct(
                element,
                CjTypeStatement::class.java
            ) != null
        }
    }



    internal class Statement :
        CangJieTemplateContextType(CangJieLiveTemplatesBundle.message("template.context.type.statement")) {
        override fun isInContext(element: PsiElement): Boolean {
            val parentStatement = PsiTreeUtil.findFirstParent(
                element
            ) { e: PsiElement ->
                e is CjExpression && CjPsiUtil.isStatementContainer(
                    e.parent
                )
            } ?: return false

            // We are in the leftmost position in parentStatement
            return element.textOffset == parentStatement.textOffset
        }
    }


    internal class Expression :
        CangJieTemplateContextType(CangJieLiveTemplatesBundle.message("template.context.type.expression")) {
        override fun isInContext(element: PsiElement): Boolean {
            return (element.parent is CjExpression && element.parent !is CjConstantExpression &&
                    element.parent.parent !is CjDotQualifiedExpression
                    && element.parent !is CjParameter)
        }
    }


    internal class Comment :
        CangJieTemplateContextType(CangJieLiveTemplatesBundle.message("template.context.type.comment")) {
        override fun isInContext(element: PsiElement): Boolean {
            return false
        }

        override fun isCommentInContext(): Boolean {
            return true
        }
    }


    companion object {
        private fun <T : PsiElement?> getParentClassOrStruct(
            element: PsiElement,
            klass: java.lang.Class<out T>
        ): T? {
            var e: PsiElement? = element
            while (e != null && !klass.isInstance(e)) {
                if (e is CjModifierList) {
                    // skip property/function/class or object which is owner of modifier list
                    e = e.parent
                    if (e != null) {
                        e = e.parent
                    }
                    continue
                }
                if (e is CjVariable<*> || e is CjNamedFunction) {
                    return null
                }
                e = e.parent
            }
            return e as T?
        }
    }

}
