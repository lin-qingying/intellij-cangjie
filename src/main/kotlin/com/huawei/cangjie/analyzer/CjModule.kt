package com.huawei.cangjie.analyzer

import com.intellij.openapi.project.Project

sealed interface CjModule {
    /**
     * [Project] to which the current module belongs.
     *
     * If the current module depends on some other modules, all those modules should have the same [Project] as the current one.
     */
    val project: Project
}


class DefaultCjModule(override val project: Project) : CjModule
