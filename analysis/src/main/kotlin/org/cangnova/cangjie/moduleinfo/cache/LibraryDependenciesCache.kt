/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.moduleinfo.cache

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.RootPolicy
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.cangnova.cangjie.moduleinfo.LibraryInfo
import org.cangnova.cangjie.moduleinfo.checkValidity
import org.cangnova.cangjie.utils.safeAs
import org.jetbrains.annotations.TestOnly
import kotlin.collections.forEach
import kotlin.collections.toMap

interface LibraryDependenciesCache {
    companion object {
        fun getInstance(project: Project): LibraryDependenciesCache = project.service()
    }

    fun getLibraryDependencies(library: org.cangnova.cangjie.moduleinfo.LibraryInfo): LibraryDependencies

    /**
     * 库依赖信息
     *
     * 包含库的所有依赖关系。
     *
     * @param library 库信息
     * @param libraries 包含自身在内的所有库依赖列表
     */
    class LibraryDependencies(
        val library: org.cangnova.cangjie.moduleinfo.LibraryInfo,
        val libraries: List<org.cangnova.cangjie.moduleinfo.LibraryInfo>,
    ) {
        /**
         * 不包含自身的库依赖列表
         */
        val librariesWithoutSelf: List<org.cangnova.cangjie.moduleinfo.LibraryInfo> by lazy { libraries - library }

        /**
         * 检查所有库的有效性
         */
        fun checkValidity() {
            library.checkValidity()
            libraries.forEach { it.checkValidity() }
        }
    }
}

/**
 * 库依赖缓存实现
 *
 * 管理和缓存项目中库的依赖关系。
 */
class LibraryDependenciesCacheImpl(private val project: Project) : LibraryDependenciesCache, Disposable {
    companion object {
        fun getInstance(project: Project): LibraryDependenciesCache = project.service()
    }

    private val cache = LibraryDependenciesInnerCache()

    private val moduleDependenciesCache = ModuleDependenciesCache()

    init {
        Disposer.register(this, cache)
        Disposer.register(this, moduleDependenciesCache)
    }

    override fun getLibraryDependencies(library: org.cangnova.cangjie.moduleinfo.LibraryInfo): LibraryDependenciesCache.LibraryDependencies = cache[library]

    override fun dispose() = Unit

    @TestOnly
    fun getCacheContentForTests(): Map<org.cangnova.cangjie.moduleinfo.LibraryInfo, LibraryDependenciesCache.LibraryDependencies> {
        return cache.getCacheContentForTests().toMap()
    }

    /**
     * 计算库的依赖关系
     *
     * @param libraryInfo 要计算依赖的库信息
     * @return 库依赖信息
     */
    private fun computeLibrariesAndSdksUsedWith(libraryInfo: org.cangnova.cangjie.moduleinfo.LibraryInfo): LibraryDependenciesCache.LibraryDependencies {
        val libraryDependencyCandidatesAndSdkInfos = computeLibrariesUsedWithNoFilter(libraryInfo)

        val libraries = libraryDependencyCandidatesAndSdkInfos.libraryDependencyCandidates.flatMap { it.libraries }

        return LibraryDependenciesCache.LibraryDependencies(
            libraryInfo,
            libraries
        )
    }

    /**
     * 计算库的依赖关系（不进行过滤）
     *
     * @param libraryInfo 要计算依赖的库信息
     * @return 库依赖候选项和 SDK 信息
     */
    private fun computeLibrariesUsedWithNoFilter(libraryInfo: org.cangnova.cangjie.moduleinfo.LibraryInfo): LibraryDependencyCandidatesInfos {
        val libraryDependencyCandidatesAndSdkInfos = LibraryDependencyCandidatesInfosBuilder()

        val modulesLibraryIsUsedIn = project.service<org.cangnova.cangjie.moduleinfo.LibraryUsageIndex>().getDependentModules(libraryInfo)

        for (module in modulesLibraryIsUsedIn) {
            checkCanceled()
            libraryDependencyCandidatesAndSdkInfos += moduleDependenciesCache[module]
        }

        return LibraryDependencyCandidatesInfos(
            libraryDependencyCandidatesAndSdkInfos.libraryDependencyCandidates,
        )
    }

    /**
     * 库依赖信息内部缓存
     *
     * 缓存库的依赖关系，监听库信息和模块根变更事件。
     */
    private inner class LibraryDependenciesInnerCache :
        SynchronizedFineGrainedEntityCache<LibraryInfo, LibraryDependenciesCache.LibraryDependencies>(project, doSelfInitialization = false, cleanOnLowMemory = true),
        org.cangnova.cangjie.moduleinfo.LibraryInfoListener,
        ModuleRootListener {

        override fun subscribe() {
            val connection = project.messageBus.connect(this)
            connection.subscribe(_root_ide_package_.org.cangnova.cangjie.moduleinfo.LibraryInfoListener.Companion.TOPIC, this)
            connection.subscribe(ModuleRootListener.TOPIC, this)
        }

        override fun libraryInfosRemoved(libraryInfos: Collection<org.cangnova.cangjie.moduleinfo.LibraryInfo>) {
            fun LibraryDependenciesCache.LibraryDependencies.haveOutdatedLibraries() =
                libraries.any { it in libraryInfos }

            invalidateEntries({ k, v -> k in libraryInfos || v.haveOutdatedLibraries() })
        }

        override fun calculate(key: org.cangnova.cangjie.moduleinfo.LibraryInfo): LibraryDependenciesCache.LibraryDependencies =
            computeLibrariesAndSdksUsedWith(key)

        override fun checkKeyValidity(key: org.cangnova.cangjie.moduleinfo.LibraryInfo) {
            key.checkValidity()
        }

        override fun checkValueValidity(value: LibraryDependenciesCache.LibraryDependencies) {
            value.checkValidity()
        }

        override fun rootsChanged(event: ModuleRootEvent) {
            if (event.isCausedByWorkspaceModelChangesOnly) return
            // 模块根变更时，需要重新计算依赖
            invalidate(writeAccessRequired = true)
        }

        @TestOnly
        @Suppress("deprecation_error")
        fun getCacheContentForTests() = cache
    }

    /**
     * 模块依赖缓存
     *
     * 缓存模块的库依赖候选项。
     */
    private inner class ModuleDependenciesCache :
        SynchronizedFineGrainedEntityCache<Module, LibraryDependencyCandidatesInfos>(project, doSelfInitialization = false),
        org.cangnova.cangjie.moduleinfo.LibraryInfoListener,
        ModuleRootListener {

        override fun subscribe() {
            val connection = project.messageBus.connect(this)
            connection.subscribe(_root_ide_package_.org.cangnova.cangjie.moduleinfo.LibraryInfoListener.Companion.TOPIC, this)
            connection.subscribe(ModuleRootListener.TOPIC, this)
        }

        @RequiresReadLock
        override fun get(key: Module): LibraryDependencyCandidatesInfos {
            ThreadingAssertions.softAssertReadAccess()
            return internalGet(key, hashMapOf(), linkedSetOf(), hashMapOf())
        }

        /**
         * 内部获取方法，处理循环依赖
         */
        private fun internalGet(
            key: Module,
            tmpResults: MutableMap<Module, LibraryDependencyCandidatesInfosBuilder>,
            trace: LinkedHashSet<Module>,
            loops: MutableMap<Module, Set<Module>>
        ): LibraryDependencyCandidatesInfos {
            checkKeyAndDisposeIllegalEntry(key)

            useCache { cache ->
                checkEntitiesIfRequired(cache)

                cache[key]
            }?.let { return it }

            checkCanceled()

            val newValue = computeLibrariesUsedIn(key, tmpResults, trace, loops).build()

            if (isValidityChecksEnabled) {
                checkValueValidity(newValue)
            }

            val existedValue = if (trace.isNotEmpty()) {
                dumpLoopsIfPossible(key, newValue, tmpResults, trace, loops)
            }
            else {
                // it is possible to dump results when all dependencies are resolved hence trace is empty
                useCache { cache ->
                    val existedValue = cache.putIfAbsent(key, newValue)
                    for (entry in tmpResults.entries) {
                        cache.putIfAbsent(entry.key, entry.value.build())
                    }

                    existedValue
                }
            }

            return existedValue ?: newValue
        }

        /**
         * It is possible to dump loops from the subtree from the last module from the loop
         *
         * @param trace is not empty trace
         * @return existed value if applicable
         */
        private fun dumpLoopsIfPossible(
            key: Module,
            newValue: LibraryDependencyCandidatesInfos,
            tmpResults: MutableMap<Module, LibraryDependencyCandidatesInfosBuilder>,
            trace: LinkedHashSet<Module>,
            loops: MutableMap<Module, Set<Module>>,
        ): LibraryDependencyCandidatesInfos? {
            val currentLoop = loops[key] ?: return null
            if (trace.last() in loops) return null

            return useCache { cache ->
                val existedValue = cache.putIfAbsent(key, newValue)
                tmpResults.remove(key)
                for (loopModule in currentLoop) {
                    tmpResults.remove(loopModule)?.let {
                        cache.putIfAbsent(loopModule, it.build())
                    }

                    loops.remove(loopModule)
                }

                existedValue
            }
        }

        /**
         * 计算模块使用的库
         */
        private fun computeLibrariesUsedIn(
            module: Module,
            tmpResults: MutableMap<Module, LibraryDependencyCandidatesInfosBuilder>,
            trace: LinkedHashSet<Module>,
            loops: MutableMap<Module, Set<Module>>
        ): LibraryDependencyCandidatesInfosBuilder {
            checkCanceled()
            check(trace.add(module)) { "recursion detected" }

            val libraryDependencyCandidatesAndSdkInfos = LibraryDependencyCandidatesInfosBuilder()
            tmpResults[module] = libraryDependencyCandidatesAndSdkInfos

            val modulesToVisit = HashSet<Module>()

            val infoCache = LibraryInfoCache.getInstance(project)
            ModuleRootManager.getInstance(module).orderEntries()
                .process(object : RootPolicy<Unit>() {
                    override fun visitModuleOrderEntry(moduleOrderEntry: ModuleOrderEntry, value: Unit) {
                        moduleOrderEntry.module?.let(modulesToVisit::add)
                    }

                    override fun visitLibraryOrderEntry(libraryOrderEntry: LibraryOrderEntry, value: Unit) {
                        checkCanceled()
                        val libraryEx = libraryOrderEntry.library.safeAs<LibraryEx>()?.takeUnless { it.isDisposed } ?: return
                        val candidate = _root_ide_package_.org.cangnova.cangjie.moduleinfo.LibraryDependencyCandidate.Companion.fromLibraryOrNull(infoCache[libraryEx]) ?: return
                        libraryDependencyCandidatesAndSdkInfos += candidate
                    }
                }, Unit)

            // handle circular dependency case
            for (moduleToVisit in modulesToVisit) {
                checkCanceled()
                if (moduleToVisit == module) continue

                if (moduleToVisit !in trace) continue

                // circular dependency found
                val reversedTrace = trace.toList().asReversed()

                val sharedLibraryDependencyCandidatesAndSdkInfos: LibraryDependencyCandidatesInfosBuilder = run {
                    var shared: LibraryDependencyCandidatesInfosBuilder? = null
                    val loop = hashSetOf<Module>()
                    val duplicates = hashSetOf<LibraryDependencyCandidatesInfosBuilder>()
                    for (traceModule in reversedTrace) {
                        loop += traceModule
                        loops[traceModule]?.let { loop += it }
                        loops[traceModule] = loop
                        val traceModuleLibraryDependencyCandidatesAndSdkInfos = tmpResults.getValue(traceModule)
                        if (shared == null && !duplicates.add(traceModuleLibraryDependencyCandidatesAndSdkInfos)) {
                            shared = traceModuleLibraryDependencyCandidatesAndSdkInfos
                        }
                        if (traceModule === moduleToVisit) {
                            break
                        }
                    }

                    shared ?: duplicates.first()
                }

                sharedLibraryDependencyCandidatesAndSdkInfos += libraryDependencyCandidatesAndSdkInfos

                for (traceModule in reversedTrace) {
                    val traceModuleLibraryDependencyCandidatesAndSdkInfos: LibraryDependencyCandidatesInfosBuilder =
                        tmpResults.getValue(traceModule)
                    if (traceModuleLibraryDependencyCandidatesAndSdkInfos === sharedLibraryDependencyCandidatesAndSdkInfos) {
                        if (traceModule === moduleToVisit) {
                            break
                        }
                        continue
                    }
                    sharedLibraryDependencyCandidatesAndSdkInfos += traceModuleLibraryDependencyCandidatesAndSdkInfos
                    tmpResults[traceModule] = sharedLibraryDependencyCandidatesAndSdkInfos

                    loops[traceModule]?.let { loop ->
                        for (loopModule in loop) {
                            if (loopModule == traceModule) continue
                            val value = tmpResults.getValue(loopModule)
                            if (value === sharedLibraryDependencyCandidatesAndSdkInfos) continue
                            sharedLibraryDependencyCandidatesAndSdkInfos += value
                            tmpResults[loopModule] = sharedLibraryDependencyCandidatesAndSdkInfos
                        }
                    }
                    if (traceModule === moduleToVisit) {
                        break
                    }
                }
            }

            // merge
            for (moduleToVisit in modulesToVisit) {
                checkCanceled()
                if (moduleToVisit == module || moduleToVisit in trace) continue

                val moduleToVisitLibraryDependencyCandidatesAndSdkInfos =
                    tmpResults[moduleToVisit] ?: internalGet(moduleToVisit, tmpResults, trace, loops = loops)

                val moduleLibraryDependencyCandidatesAndSdkInfos = tmpResults.getValue(module)

                // We should not include SDK from dependent modules
                // see the traverse way of OrderEnumeratorBase#shouldAddOrRecurse for JdkOrderEntry
                moduleLibraryDependencyCandidatesAndSdkInfos.libraryDependencyCandidates +=
                    moduleToVisitLibraryDependencyCandidatesAndSdkInfos.libraryDependencyCandidates
            }

            trace.remove(module)

            return tmpResults.getValue(module)
        }

        override fun calculate(key: Module): LibraryDependencyCandidatesInfos =
            throw UnsupportedOperationException("calculate(Module) should not be invoked due to custom impl of get()")

        override fun checkKeyValidity(key: Module) {
            key.checkValidity()
        }

        override fun checkValueValidity(value: LibraryDependencyCandidatesInfos) {
            value.libraryDependencyCandidates.forEach { it.libraries.forEach { libraryInfo -> libraryInfo.checkValidity() } }
        }

        override fun rootsChanged(event: ModuleRootEvent) {
            if (event.isCausedByWorkspaceModelChangesOnly) return
            // 模块根变更时，需要重新计算依赖
            invalidate(writeAccessRequired = true)
        }

        override fun libraryInfosRemoved(libraryInfos: Collection<org.cangnova.cangjie.moduleinfo.LibraryInfo>) {
            val infos = libraryInfos.toHashSet()
            invalidateEntries(
                { _, v ->
                    v.libraryDependencyCandidates.any { candidate -> candidate.libraries.any { it in infos } }
                },
                // unable to check entities properly: an event could be not the last
                validityCondition = null
            )
        }
    }
}


private open class LibraryDependencyCandidatesInfos(
    open val libraryDependencyCandidates: Collection<org.cangnova.cangjie.moduleinfo.LibraryDependencyCandidate>,
) {
    override fun toString(): String {
        return "[${Integer.toHexString(System.identityHashCode(this))}] libraryDependencyCandidates: ${
            libraryDependencyCandidates.map { it.libraries.map(_root_ide_package_.org.cangnova.cangjie.moduleinfo.LibraryInfo::name) }
        } "
    }
}

private class LibraryDependencyCandidatesInfosBuilder(
    override val libraryDependencyCandidates: MutableSet<org.cangnova.cangjie.moduleinfo.LibraryDependencyCandidate> = linkedSetOf(),
): LibraryDependencyCandidatesInfos(libraryDependencyCandidates) {
    operator fun plusAssign(other: LibraryDependencyCandidatesInfosBuilder) {
        libraryDependencyCandidates += other.libraryDependencyCandidates
    }

    operator fun plusAssign(other: LibraryDependencyCandidatesInfos) {
        libraryDependencyCandidates += other.libraryDependencyCandidates
    }

    operator fun plusAssign(libraryDependencyCandidate: org.cangnova.cangjie.moduleinfo.LibraryDependencyCandidate) {
        libraryDependencyCandidates += libraryDependencyCandidate
    }



    fun build(): LibraryDependencyCandidatesInfos =
        LibraryDependencyCandidatesInfos(libraryDependencyCandidates.toList() )

    override fun toString(): String {
        return "builder [${Integer.toHexString(System.identityHashCode(this))}] libraryDependencyCandidates: ${
            libraryDependencyCandidates.map { it.libraries.map(_root_ide_package_.org.cangnova.cangjie.moduleinfo.LibraryInfo::name) }
        }  "
    }
}
