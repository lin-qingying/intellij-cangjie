package com.huawei.cangjie.idea.run.cjpm

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.idea.icons.CangJieIcons
import com.huawei.cangjie.lang.sdk.CangJieSdkManager
import com.huawei.cangjie.lang.sdk.CangJieSdkType
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.util.SystemInfo.isWindows
import com.intellij.xdebugger.XDebugProcessStarter
import org.jdom.Element


class CjpmRunConfigurationType : SimpleConfigurationType("CjpmRunConfigurationType",
    "Cjpm",
    "Cjpm",
    NotNullLazyValue.createValue { CangJieIcons.CANGJIE_FILE }

) {


    override fun createTemplateConfiguration(project: Project): CjpmRunConfiguration {

        return CjpmRunConfiguration(project, this, "Cjpm")
    }

    override fun isDumbAware(): Boolean = true

    override fun isEditableInDumbMode(): Boolean = true


    companion object {
        val instance: CjpmRunConfigurationType
            get() = findConfigurationType(CjpmRunConfigurationType::class.java)
    }


}







