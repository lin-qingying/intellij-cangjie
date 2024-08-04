package com.linqingying.cangjie.dapDebugger.runconfig

import com.linqingying.cangjie.lang.CangJieFileType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider

class CangJieDebuggerEditorsProvider : XDebuggerEditorsProvider() {
    override fun getFileType(): FileType = CangJieFileType


    @Deprecated("Deprecated in Java")
    override fun createDocument(
        project: Project,
        text: String,
        sourcePosition: XSourcePosition?,
        mode: EvaluationMode
    ): Document {
        return PsiDocumentManager.getInstance(project).getDocument(
            PsiFileFactory.getInstance(project).createFileFromText(
                PlainTextLanguage.INSTANCE, text
            )
        ) ?: error("Unable to create plain text document for expression")
    }


//    override fun createExpressionCodeFragment(
//        project: Project,
//        text: String,
//        context: PsiElement?,
//        isPhysical: Boolean
//    ): PsiFile? {
////        return null
//        val psiFactory = CjPsiFactory(project)
//        return psiFactory.createExpressionCodeFragment(text, context)
////        return null
//    }
}

