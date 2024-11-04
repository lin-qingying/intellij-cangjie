package com.linqingying.cangjie.ide.run

import com.linqingying.cangjie.ide.run.cjpm.RunMainAction
import com.linqingying.cangjie.ide.run.cjpm.RunTestAction
import com.linqingying.cangjie.lexer.CjTokens.IDENTIFIER
import com.linqingying.cangjie.psi.CjAnnotated
import com.linqingying.cangjie.psi.CjMainFunction
import com.linqingying.cangjie.psi.psiUtil.elementType
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

class CjRunLineMarkersProvider : RunLineMarkerContributor(), DumbAware {

    override fun getInfo(element: PsiElement): Info? {
        if (element is CjMainFunction) {
            val action = ActionManager.getInstance().getAction(RunMainAction.ID)
            return Info(AllIcons.RunConfigurations.TestState.Run, { "run main" }, action)
        } else if (isTestCase(element)) {
            // TODO: mem leak ?
            val action = RunTestAction(element)
            return Info(AllIcons.RunConfigurations.TestState.Run, { "run test" }, action)
        }
        return null
    }

    private fun isTestCase(element: PsiElement): Boolean {
        if (element.elementType == IDENTIFIER  && element.parent is CjAnnotated) {
            val annotated: CjAnnotated = element.parent as CjAnnotated
            if (hasTestAnnotation(annotated)) {
                return true
            }
        }
        return false
    }

    private fun hasTestAnnotation(element: CjAnnotated): Boolean {
        return element.annotationEntries.any {
            it.text == "@Test" || it.text == "@TestCase"
        }
    }

}
