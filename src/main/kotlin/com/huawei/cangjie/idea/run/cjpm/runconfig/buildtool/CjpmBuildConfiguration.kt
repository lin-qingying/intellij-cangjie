package com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool

import com.huawei.cangjie.idea.run.cjpm.CjpmCommandConfiguration
import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildConfiguration
import com.huawei.cangjie.idea.run.cjpm.runconfig.buildtool.CjpmBuildManager.isBuildToolWindowAvailable
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.roots.ProjectModelBuildableElement
import com.intellij.openapi.roots.ProjectModelExternalSource


@Suppress("UnstableApiUsage")
open class CjpmBuildConfiguration(
    val configuration: CjpmCommandConfiguration,
    val environment: ExecutionEnvironment
) : ProjectModelBuildableElement {
    open val enabled: Boolean get() = configuration.isBuildToolWindowAvailable

    init {

        require(isBuildConfiguration(configuration))
    }

    override fun getExternalSource(): ProjectModelExternalSource? = null
}
