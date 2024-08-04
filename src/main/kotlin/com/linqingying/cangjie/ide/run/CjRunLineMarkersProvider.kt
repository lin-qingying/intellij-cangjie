package com.linqingying.cangjie.ide.run

//import com.linqingying.cangjie.ide.run.cjpm.RunTestAction
//import com.linqingying.cangjie.psi.CjAnnotated
import com.linqingying.cangjie.ide.run.cjpm.RunMainAction
import com.linqingying.cangjie.ide.run.cjpm.RunTestAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement


class CjRunLineMarkersProvider : RunLineMarkerContributor(), DumbAware {

    override fun getInfo(element: PsiElement): Info? {


        if (element.text == "main") {
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


        return element.text == "@" && (element.nextSibling.text == "Test" || element.nextSibling.text == "TestCase")


    }


}
