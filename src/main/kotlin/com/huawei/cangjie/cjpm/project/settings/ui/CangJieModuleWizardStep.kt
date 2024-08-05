package com.huawei.cangjie.ide.project.settings.ui

import com.huawei.cangjie.ide.project.tools.projectWizard.wizard.CangJieModuleType
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import javax.swing.JComponent
import javax.swing.JLabel


class CangJieModuleWizardStep : ModuleBuilder() {
    override fun getModuleType(): ModuleType<*> {
        return CangJieModuleType()
    }

    override fun createWizardSteps(
        wizardContext: WizardContext,
        modulesProvider: ModulesProvider
    ): Array<out ModuleWizardStep> {
        return arrayOf(object : ModuleWizardStep() {
            override fun getComponent(): JComponent {
                return JLabel("Put your content here")
            }

            override fun updateDataModel() {}
        })
    }
}

