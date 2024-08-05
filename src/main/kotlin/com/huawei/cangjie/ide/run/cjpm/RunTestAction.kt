package com.huawei.cangjie.ide.run.cjpm

import com.huawei.cangjie.ide.run.cjpm.util.executeActionCjpmCommand
import com.huawei.cangjie.psi.CjAnnotated
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
        var filterName = element.text
        val parentElement = element.parent as? CjAnnotated ?: throw RuntimeException("@TestCase without @Test class parent")
        val isTestCase = (parentElement.annotationEntries.any { it.text == "@TestCase" })
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
