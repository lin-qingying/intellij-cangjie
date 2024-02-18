package com.huawei.cangjie.cjpm.project.configurable

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.PlatformUtils

@Suppress("UnstableApiUsage")
abstract class CjConfigurableBase(
    protected open val project: Project,
    @NlsContexts.ConfigurableName displayName: String
) : BoundConfigurable(displayName) {
    // Currently, we have help page only for CLion
    override fun getHelpTopic(): String? = if (PlatformUtils.isCLion()) "cangjiesupport" else null
}
