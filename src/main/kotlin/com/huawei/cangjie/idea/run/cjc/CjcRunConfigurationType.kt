package com.huawei.cangjie.idea.run.cjc

import com.huawei.cangjie.idea.icons.CangJieIcons
import com.huawei.cangjie.idea.run.cjpm.CangJieRunConfigurationOptions
import com.huawei.cangjie.idea.run.cjpm.CjpmRunConfigurationType
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue

class CjcRunConfigurationType : SimpleConfigurationType("CjcRunConfigurationType",
    "Cjc",
    "Cjc",
    NotNullLazyValue.createValue { CangJieIcons.CANGJIE_FILE }

) {
    override fun createTemplateConfiguration(project: Project): CjcRunConfiguration {
        return CjcRunConfiguration(project, this, "Cjc")
    }

    companion object {
        val instance: CjcRunConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(CjcRunConfigurationType::class.java)
    }
}


class CjcRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<CangJieRunConfigurationOptions>(project, factory, name) {
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        TODO("Not yet implemented")
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        TODO("Not yet implemented")
    }

}

