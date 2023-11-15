package com.huawei.cangjie.idea.project.settings.ui

import com.huawei.cangjie.idea.icons.CangJieIcons
import com.huawei.cangjie.idea.project.tools.projectWizard.wizard.NewProjectWizardModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import javax.swing.Icon
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

class CangJieModuleType : ModuleType<NewProjectWizardModuleBuilder>("CangJieModuleType") {
    override fun createModuleBuilder(): NewProjectWizardModuleBuilder {
        return NewProjectWizardModuleBuilder()
    }

    override fun getName(): String {
        return "CangJieModuleType"
    }

    override fun getDescription(): String {
        return "CangJieModuleType"
    }

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return CangJieIcons.CANGJIE_FILE
    }
}
