package com.huawei.cangjie.idea.run.cjpm

import com.huawei.cangjie.idea.run.cjpm.util.executeActionCjpmCommand
import com.huawei.cangjie.psi.CjClass
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.NonNls


class RunTestAction(element: PsiElement) : DumbAwareAction() {
    private val configurationName: String
    private val filter: String

    init {
        var name = element.containingFile.name.replace(".", "_")
        val parentElement = element.nextSibling
        var filterName = "@" + parentElement.text

        val isTestCase = parentElement.text == "@TestCase"
        if (isTestCase) {
            // identifier => func(annotated) => class body ==> class(annotated)
            val clazz = parentElement.parent?.parent as? CjClass
            filterName = clazz?.name + "." + filterName
        }
        name += filterName

        configurationName = name
        filter = filterName
    }

    override fun actionPerformed(e: AnActionEvent) {
        executeActionCjpmCommand(e, configurationName, "test", "--filter=${filter}")
    }


    companion object {
        val ID: @NonNls String = "CjpmRunTest"
    }
}
