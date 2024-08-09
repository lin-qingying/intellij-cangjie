package com.linqingying.cangjie.ide.run.cjpm

import com.linqingying.cangjie.icon.CangJieIcons
import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue


class CjpmCommandConfigurationType : ConfigurationTypeBase("CjpmCommandConfigurationType",
    "Cjpm",
    "Cjpm",
    NotNullLazyValue.createValue { CangJieIcons.CANGJIE_16 }

) {
    val factory: ConfigurationFactory get() = configurationFactories.single()

    init {
        addFactory(CjpmConfigurationFactory(this))
    }

    companion object {
        val instance: CjpmCommandConfigurationType
            get() = ConfigurationTypeUtil.findConfigurationType(CjpmCommandConfigurationType::class.java)
    }
}

class CjpmConfigurationFactory(type: CjpmCommandConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return CjpmCommandConfiguration(project, this, "Cjpm")
    }

    companion object {
        const val ID: String = "Cjpm Command"
    }
}
