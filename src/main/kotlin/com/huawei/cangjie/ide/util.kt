package com.huawei.cangjie.ide

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import org.jetbrains.annotations.ApiStatus

val Project.isInDumbMode: Boolean
      get() = DumbService.getInstance(this).isDumb


fun <T> Project.runReadActionInSmartMode(action: () -> T): T {
    if (ApplicationManager.getApplication().isReadAccessAllowed) return action()
    return DumbService.getInstance(this).runReadActionInSmartMode(Computable(action))
}
