package org.cangnova.cangjie.dapDebugger.runconfig

import org.cangnova.cangjie.ide.debugger.CodeFragmentContextTuner
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProviderBase

import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.CjPsiFactory

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

