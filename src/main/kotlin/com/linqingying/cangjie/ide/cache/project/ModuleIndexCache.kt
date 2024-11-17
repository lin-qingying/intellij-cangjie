/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package com.linqingying.cangjie.ide.cache.project

import com.linqingying.cangjie.analyzer.ModuleSourceInfo
import com.linqingying.cangjie.ide.cache.trackers.ModuleModificationTracker
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.MultiMap
import java.util.ArrayDeque

//NOTE: this is an approximation that may contain more module infos than the exact solution

fun ModuleSourceInfo.getDependentModules(): Set<ModuleSourceInfo> {
    val dependents = getDependents(module)

    return dependents.flatMapTo(HashSet<ModuleSourceInfo>()) { it.sourceModuleInfos }
}
enum class SourceType {
    PRODUCTION,
    TEST
}


//NOTE: getDependents adapted from com.intellij.openapi.module.impl.scopes.ModuleWithDependentsScope#buildDependents()
private fun getDependents(module: Module): Set<Module> {
    val result = java.util.HashSet<Module>()
    result.add(module)

    val processedExporting = java.util.HashSet<Module>()

    val index = getModuleIndex(module.project)

    val walkingQueue = ArrayDeque<Module>(10)
    walkingQueue.addLast(module)

    while (true) {
        val current = walkingQueue.pollFirst() ?: break
        processedExporting.add(current)
        result.addAll(index.plainUsages(current))
        for (dependent in index.exportingUsages(current)) {
            result.add(dependent)
            if (processedExporting.add(dependent)) {
                walkingQueue.addLast(dependent)
            }
        }
    }
    return result
}

private interface ModuleIndex {

    fun plainUsages(module: Module): Collection<Module>

    fun exportingUsages(module: Module): Collection<Module>
}

private class ModuleIndexImpl(
    private val plainUsages: MultiMap<Module, Module>,
    private val exportingUsages: MultiMap<Module, Module>,
) : ModuleIndex {
    override fun plainUsages(module: Module): Collection<Module> = plainUsages[module]

    override fun exportingUsages(module: Module): Collection<Module> = exportingUsages[module]
}

private fun getModuleIndex(project: Project): ModuleIndex = CachedValuesManager.getManager(project).getCachedValue(project) {
    val plainUsages: MultiMap<Module, Module> = MultiMap.create()
    val exportingUsages: MultiMap<Module, Module> = MultiMap.create()

    runReadAction {
        for (module in ModuleManager.getInstance(project).modules) {
            ProgressManager.checkCanceled()
            for (orderEntry in ModuleRootManager.getInstance(module).orderEntries) {
                if (orderEntry !is ModuleOrderEntry) continue
                val referenced = orderEntry.module ?: continue
                val map = if (orderEntry.isExported) exportingUsages else plainUsages
                map.putValue(referenced, module)
            }
        }
    }

    CachedValueProvider.Result(
        ModuleIndexImpl(plainUsages = plainUsages, exportingUsages = exportingUsages),
        ModuleModificationTracker.getInstance(project)
    )
}
