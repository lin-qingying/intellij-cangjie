package com.huawei.cangjie.ide.run.cjc

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class CjcRunConfigurationProducer : LazyRunConfigurationProducer<CjcRunConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory {
        return CjcRunConfigurationType.instance
    }

    override fun isConfigurationFromContext(
        configuration: CjcRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        return false
    }

    override fun setupConfigurationFromContext(
        configuration: CjcRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        configuration.name = "Cjc"
        return true
    }


}
