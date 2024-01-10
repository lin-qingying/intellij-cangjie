package com.debugger.runconfig

import com.huawei.cangjie.lang.CangJieFileType
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjPsiFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase

class CangJieDebuggerEditorsProvider : XDebuggerEditorsProviderBase() {
    override fun getFileType(): FileType = CangJieFileType

    override fun createExpressionCodeFragment(
        project: Project,
        text: String,
        context: PsiElement?,
        isPhysical: Boolean
    ): PsiFile? {
//        return null
//        val psiFactory = CjPsiFactory(project)
//        return psiFactory.createExpressionCodeFragment(text, context)
        return null
    }
}

