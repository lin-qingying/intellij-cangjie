package com.huawei.cangjie.highlighter.visitor

import com.huawei.cangjie.highlighter.HighlightingFactory
import com.huawei.cangjie.psi.CjNamedDeclaration
import com.huawei.cangjie.psi.CjVisitorVoid
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement


abstract class AbstractHighlightingVisitor(protected val holder: HighlightInfoHolder) : CjVisitorVoid() {
    protected fun highlightName(element: PsiElement, highlightInfoType: HighlightInfoType, message: String? = null) {
        holder.add(HighlightingFactory.highlightName(element, highlightInfoType, message)?.create())
    }

    protected fun highlightName(
        project: Project,
        textRange: TextRange,
        highlightInfoType: HighlightInfoType,
        message: String? = null
    ) {
        holder.add(HighlightingFactory.highlightName(project, textRange, highlightInfoType, message).create())
    }

    protected fun highlightNamedDeclaration(declaration: CjNamedDeclaration, attributesKey: HighlightInfoType) {
        declaration.nameIdentifier?.let { highlightName(it, attributesKey) }
    }
}
