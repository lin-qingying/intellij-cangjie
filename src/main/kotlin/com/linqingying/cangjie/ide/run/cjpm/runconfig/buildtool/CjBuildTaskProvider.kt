package com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.ide.run.cjpm.CjpmCommandConfiguration
import com.linqingying.cangjie.ide.run.cjpm.runconfig.buildtool.CjpmBuildManager.createBuildEnvironment

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key
import com.intellij.task.ProjectTaskManager
import java.util.concurrent.CompletableFuture


abstract class CjBuildTaskProvider<T : CjBuildTaskProvider.BuildTask<T>> : BeforeRunTaskProvider<T>() {
    override fun getName(): String = CangJieBundle.message("build")
    abstract class BuildTask<T : BuildTask<T>>(providerId: Key<T>) : BeforeRunTask<T>(providerId) {
        init {
            isEnabled = true
        }
    }

    override fun isSingleton(): Boolean = true
    protected fun doExecuteTask(
        buildConfiguration: CjpmCommandConfiguration,
        environment: ExecutionEnvironment
    ): Boolean {


        val buildEnvironment = createBuildEnvironment(buildConfiguration, environment) ?: return false
        val buildableElement = CjpmBuildConfiguration(buildConfiguration, buildEnvironment)

        val result = CompletableFuture<Boolean>()
        ProjectTaskManager.getInstance(environment.project).build(buildableElement).onProcessed {
            result.complete(!it.hasErrors() && !it.isAborted)
        }
        return result.get()
    }
}
