package com.linqingying.cangjie.dapDebugger1.runconfig

import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase
import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.lang.CangJieLanguage
import com.linqingying.cangjie.psi.CjPsiFactory

class CangJieDebuggerEditorsProvider : XDebuggerEditorsProviderBase() {
    override fun getFileType(): FileType = CangJieFileType.INSTANCE


    override fun createExpressionCodeFragment(
        project: Project,
        text: String,
        context: PsiElement?,
        isPhysical: Boolean
    ): PsiFile {
        val psiFactory = CjPsiFactory(project)
        return psiFactory.createExpressionCodeFragment(text, context)
    }


}

