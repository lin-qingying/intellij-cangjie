package com.linqingying.cangjie.ide.liveTemplates

import com.linqingying.cangjie.lang.CangJieLanguage
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
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
            || PsiTreeUtil.getParentOfType(element, CjImportDirective::class.java) != null
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
    open class Generic :
        CangJieTemplateContextType(CangJieLiveTemplatesBundle.message("template.context.type.generic")) {
        override fun isInContext(element: PsiElement): Boolean {
            return true
        }

        override fun isCommentInContext(): Boolean {
       return true
        }
    }


    class TopLevel :
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
                if (e is CjVariable || e is CjNamedFunction || e is CjClassOrStruct) {
                    return false
                }

                e = e.parent
            }
            return true
        }
    }


    class Class :
        CangJieTemplateContextType(CangJieLiveTemplatesBundle.message("template.context.type.class")) {
        override fun isInContext(element: PsiElement): Boolean {
            return getParentClassOrStruct(
                element,
                CjClassOrStruct::class.java
            ) != null
        }
    }



    class Statement :
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


    class Expression :
        CangJieTemplateContextType(CangJieLiveTemplatesBundle.message("template.context.type.expression")) {
        override fun isInContext(element: PsiElement): Boolean {
            return (element.parent is CjExpression && element.parent !is CjConstantExpression &&
                    element.parent.parent !is CjDotQualifiedExpression
                    && element.parent !is CjParameter)
        }
    }


    class Comment :
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
                if (e is CjVariable || e is CjNamedFunction) {
                    return null
                }
                e = e.parent
            }
            return e as T?
        }
    }

}
