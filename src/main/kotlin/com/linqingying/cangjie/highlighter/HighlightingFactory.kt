package com.linqingying.cangjie.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement


object HighlightingFactory {
    fun highlightName(element: PsiElement, highlightInfoType: HighlightInfoType, message: String? = null): HighlightInfo.Builder? {
        val project = element.project
        if (!element.textRange.isEmpty) {
            return highlightName(project, element.textRange, highlightInfoType, message)
        }
        return null
    }

    fun highlightName(project: Project, textRange: TextRange, highlightInfoType: HighlightInfoType, message: String? = null): HighlightInfo.Builder? {
        if (project.isNameHighlightingEnabled) {
            val builder = HighlightInfo.newHighlightInfo(highlightInfoType)
            if (message != null) {
                builder.descriptionAndTooltip(message)
            }
            val annotation = builder
                .range(textRange)
            return annotation
        }
        return null
    }
}
