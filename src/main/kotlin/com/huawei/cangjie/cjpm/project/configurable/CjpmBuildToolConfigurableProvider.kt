package com.huawei.cangjie.cjpm.project.configurable

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project


class CjpmBuildToolConfigurableProvider(private val project: Project) : ConfigurableProvider() {
    override fun createConfigurable(): Configurable {
        return CjpmConfigurable(project, isPlaceholder = false)
    }
}