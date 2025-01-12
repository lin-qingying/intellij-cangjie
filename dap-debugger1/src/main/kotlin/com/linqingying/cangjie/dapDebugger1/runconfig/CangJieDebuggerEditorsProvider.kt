package com.linqingying.cangjie.dapDebugger1.runconfig

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase
import com.linqingying.cangjie.ide.debugger.CodeFragmentContextTuner
import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.psi.CjPsiFactory

class CangJieDebuggerEditorsProvider : XDebuggerEditorsProviderBase() {
    override fun getFileType(): FileType = CangJieFileType.INSTANCE


    override fun createExpressionCodeFragment(
        project: Project,
        text: String,
        context: PsiElement?,
        isPhysical: Boolean
    ): PsiFile {
        val contextElement = CodeFragmentContextTuner.getInstance().tuneContextElement(context)

        val psiFactory = CjPsiFactory(project)
        return psiFactory.createBlockCodeFragment(text, contextElement)
    }


}

