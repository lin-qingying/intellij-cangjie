package com.huawei.cangjie.idea.run.cjpm

import com.huawei.cangjie.idea.icons.CangJieIcons
import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.util.PlatformUtils


class CjpmCommandConfigurationType : ConfigurationTypeBase("CjpmCommandConfigurationType",
    "Cjpm",
    "Cjpm",
    NotNullLazyValue.createValue { CangJieIcons.CANGJIE_FILE }

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


//
//class CjpmCommandConfigurationType : SimpleConfigurationType("CjpmCommandConfigurationType",
//    "Cjpm",
//    "Cjpm",
//    NotNullLazyValue.createValue { CangJieIcons.CANGJIE_FILE }
//
//) {
//
//
//    override fun createTemplateConfiguration(project: Project): CjpmCommandConfiguration {
//
//        return CjpmCommandConfiguration(project, this, "Cjpm")
//    }
//
//    override fun isDumbAware(): Boolean = true
//
//    override fun isEditableInDumbMode(): Boolean = true
//
//
//    companion object {
//        val instance: CjpmCommandConfigurationType
//            get() = findConfigurationType(CjpmCommandConfigurationType::class.java)
//    }
//
//
//}
//
//
//
//
//
//
//
