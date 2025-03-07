package cn.cangnova.cangjie.debugger.lang

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.jetbrains.cidr.execution.debugger.CidrDebuggerEditorsExtensionBase
import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.CjPsiFactory
import cn.cangnova.cangjie.psi.psiUtil.ancestorOrSelf


class CjDebuggerEditorsExtension : CidrDebuggerEditorsExtensionBase() {
    override fun getContext(project: Project, sourcePosition: XSourcePosition): PsiElement? =
        super.getContext(project, sourcePosition)?.ancestorOrSelf<CjElement>()

    override fun createExpressionCodeFragment(project: Project, text: String, context: PsiElement, mode: EvaluationMode): PsiFile =
        if (context is CjElement) {

            val psiFactory = CjPsiFactory(project)

            psiFactory.createBlockCodeFragment(text, context)


        } else {
            super.createExpressionCodeFragment(project, text, context, mode)
        }
}
