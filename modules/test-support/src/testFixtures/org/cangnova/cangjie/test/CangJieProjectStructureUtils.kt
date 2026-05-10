/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.cangnova.cangjie.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.libraries.Library
import com.intellij.testFramework.IndexingTestUtil

/**
 * 对位 Kotlin `ProjectStructureUtils` 的仓颉 project-structure 测试 helper。
 */
fun Module.addDependency(
    library: Library,
    dependencyScope: DependencyScope = DependencyScope.COMPILE,
    exported: Boolean = false,
) = ModuleRootModificationUtil.addDependency(this, library, dependencyScope, exported)
    .also {
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

fun Module.addDependency(
    other: Module,
    dependencyScope: DependencyScope = DependencyScope.COMPILE,
    exported: Boolean = false,
    productionOnTest: Boolean = false,
): Module = apply {
    ModuleRootModificationUtil.addDependency(this, other, dependencyScope, exported, productionOnTest)
    IndexingTestUtil.waitUntilIndexesAreReady(project)
}

