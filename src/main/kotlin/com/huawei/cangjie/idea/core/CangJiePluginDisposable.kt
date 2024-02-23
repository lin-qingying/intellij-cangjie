package com.huawei.cangjie.idea.core

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

 @Service(Service.Level.PROJECT)
class CangJiePluginDisposable : Disposable {
    @Volatile
    var disposed: Boolean = false

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CangJiePluginDisposable = project.service<CangJiePluginDisposable>()
    }

    override fun dispose() {
        disposed = true
    }
}

@ApiStatus.Internal
fun <T> syncNonBlockingReadAction(project: Project, task: () -> T): T {
    return ReadAction.nonBlocking<T> { task() }
        .expireWith(CangJiePluginDisposable.getInstance(project))
        .executeSynchronously()
}