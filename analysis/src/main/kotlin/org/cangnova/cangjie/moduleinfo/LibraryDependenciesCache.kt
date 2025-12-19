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

package org.cangnova.cangjie.moduleinfo

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.RootPolicy
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.util.Disposer
import com.intellij.platform.backend.workspace.WorkspaceModelChangeListener
import com.intellij.platform.backend.workspace.WorkspaceModelTopics
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SdkEntity
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.platform.workspace.storage.VersionedStorageChange
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModule
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import kotlin.sequences.forEach

interface LibraryDependenciesCache {
    companion object {
        fun getInstance(project: Project): LibraryDependenciesCache = project.service()
    }

    fun getLibraryDependencies(library: LibraryInfo): LibraryDependencies

    class LibraryDependencies(
        val library: LibraryInfo,
        val libraries: List<LibraryInfo>,

        val sourcesOnlyDependencies: List<LibraryInfo>,
    ) {
        val librariesWithoutSelf: List<LibraryInfo> by lazy { libraries - library }

        fun checkValidity() {
            library.checkValidity()
            libraries.forEach { it.checkValidity() }

            sourcesOnlyDependencies.forEach { it.checkValidity() }
        }
    }
}
class LibraryDependenciesCacheImpl(private val project: Project) : LibraryDependenciesCache, Disposable {
    companion object {
        fun getInstance(project: Project): LibraryDependenciesCache = project.service()

        /**
         * @see filterForBuiltins
         */
        @ApiStatus.Internal
        fun LibraryInfo.isSpecialKotlinCoreLibrary(project: Project): Boolean {
            return !IdeBuiltInsLoadingState.isFromClassLoader && isCoreKotlinLibrary(project)
        }
    }

    private val cache = LibraryDependenciesInnerCache()

    private val moduleDependenciesCache = ModuleDependenciesCache()

    init {
        Disposer.register(this, cache)
        Disposer.register(this, moduleDependenciesCache)
    }

    override fun getLibraryDependencies(library: LibraryInfo): LibraryDependenciesCache.LibraryDependencies = cache[library]

    override fun dispose() = Unit

    @TestOnly
    fun getCacheContentForTests(): Map<LibraryInfo, LibraryDependenciesCache.LibraryDependencies> {
        return cache.getCacheContentForTests().toMap()
    }

    private fun computeLibrariesAndSdksUsedWith(libraryInfo: LibraryInfo): LibraryDependenciesCache.LibraryDependencies {
        val libraryDependencyCandidatesAndSdkInfos = computeLibrariesAndSdksUsedWithNoFilter(libraryInfo)

        val additionalDependenciesForLibrarySources: List<LibraryInfo>
        val libraryDependenciesFilter: LibraryDependenciesFilter

        when {
            // Maven is Gradle Metadata unaware and needs special handling. See KTIJ-15758, KTIJ-23874
            project.isMavenized -> {
                libraryDependenciesFilter = StrictEqualityForPlatformSpecificCandidatesFilter
                additionalDependenciesForLibrarySources =
                    stdlibJvmDependencies(libraryInfo, libraryDependencyCandidatesAndSdkInfos.libraryDependencyCandidates)
            }

            else -> {
                libraryDependenciesFilter =
                    DefaultLibraryDependenciesFilter union SharedNativeLibraryToNativeInteropFallbackDependenciesFilter
                additionalDependenciesForLibrarySources = emptyList()
            }
        }

        val libraries = libraryDependenciesFilter(
            libraryInfo.platform,
            libraryDependencyCandidatesAndSdkInfos.libraryDependencyCandidates
        ).flatMap { it.libraries }

        return LibraryDependencies(
            libraryInfo,
            libraries,
            libraryDependencyCandidatesAndSdkInfos.sdkInfos.toList(),
            additionalDependenciesForLibrarySources
        )
    }

    /**
     * Workaround for separate publishing of standard library sources (KTIJ-23874).
     * Common parts of the standard library have to be provided as a dependency, but only in the context of a search scope for sources.
     */
    private fun stdlibJvmDependencies(
        libraryInfo: LibraryInfo,
        allDependencyCandidates: Collection<LibraryDependencyCandidate>,
    ): List<LibraryInfo> {
        if (!libraryInfo.platform.isJvm()) return emptyList()

        val stdlibCache = KotlinStdlibCache.getInstance(libraryInfo.project)
        if (!stdlibCache.isStdlib(libraryInfo)) return emptyList()

        return allDependencyCandidates.flatMap { candidate ->
            candidate.libraries.filter { library -> stdlibCache.isStdlibDependency(library) }
        }
    }

    //NOTE: used LibraryRuntimeClasspathScope as reference
    private fun computeLibrariesAndSdksUsedWithNoFilter(libraryInfo: LibraryInfo): LibraryDependencyCandidatesAndSdkInfos {
        val libraryDependencyCandidatesAndSdkInfos = LibraryDependencyCandidatesAndSdkInfosBuilder()

        val modulesLibraryIsUsedIn = project.service<LibraryUsageIndex>().getDependentModules(libraryInfo)

        for (module in modulesLibraryIsUsedIn) {
            checkCanceled()
            libraryDependencyCandidatesAndSdkInfos += moduleDependenciesCache[module]
        }

        val filteredLibraries = filterForBuiltins(libraryInfo, libraryDependencyCandidatesAndSdkInfos.libraryDependencyCandidates)

        libraryDependencyCandidatesAndSdkInfos.sdkInfos.takeIf { it.isEmpty() }?.apply {
            val scriptConfigurationManager = ScriptDependencyAware.getInstance(project)
            scriptConfigurationManager.getScriptDependingOn(libraryInfo.getLibraryRoots())
                ?.let { script -> scriptConfigurationManager.getScriptSdk(script) }
                ?.let { sdk -> add(SdkInfo(project, sdk)) }
        }

        return LibraryDependencyCandidatesAndSdkInfos(filteredLibraries, libraryDependencyCandidatesAndSdkInfos.sdkInfos)
    }

    /*
    * When built-ins are created from module dependencies (as opposed to loading them from classloader)
    * we must resolve Kotlin standard library containing some built-ins declarations in the same
    * resolver for project as JDK. This comes from the following requirements:
    * - JvmBuiltins need JDK and standard library descriptors -> resolver for project should be able to
    *   resolve them
    * - Builtins are created in BuiltinsCache -> module descriptors should be resolved under lock of the
    *   SDK resolver to prevent deadlocks
    * This means we have to maintain dependencies of the standard library manually or effectively drop
    * resolver for SDK otherwise. Libraries depend on superset of their actual dependencies because of
    * the inability to get real dependencies from IDEA model. So moving stdlib with all dependencies
    * down is a questionable option.
    */
    private fun filterForBuiltins(
        libraryInfo: LibraryInfo,
        dependencyLibraries: MutableSet<LibraryDependencyCandidate>
    ): MutableSet<LibraryDependencyCandidate> {
        return if (libraryInfo.isSpecialKotlinCoreLibrary(project)) {
            dependencyLibraries.filterTo(mutableSetOf()) { dep ->
                dep.libraries.any { it.isCoreKotlinLibrary(project) }
            }
        } else {
            dependencyLibraries
        }
    }

    private inner class LibraryDependenciesInnerCache :
        SynchronizedFineGrainedEntityCache<LibraryInfo, LibraryDependenciesCache.LibraryDependencies>(project, doSelfInitialization = false, cleanOnLowMemory = true),
        LibraryInfoListener,
        ModuleRootListener {

        override fun subscribe() {
            val connection = project.messageBus.connect(this)
            connection.subscribe(LibraryInfoListener.TOPIC, this)
            connection.subscribe(WorkspaceModelTopics.CHANGED, ModelChangeListener())
            connection.subscribe(WorkspaceModelTopics.CHANGED, SdkChangeListener())
            connection.subscribe(ModuleRootListener.TOPIC, this)
        }

        override fun libraryInfosRemoved(libraryInfos: Collection<LibraryInfo>) {
            fun LibraryDependencies.haveOutdatedLibraries() =
                libraries.any { it in libraryInfos } || sourcesOnlyDependencies.any { it in libraryInfos }

            invalidateEntries({ k, v -> k in libraryInfos || v.haveOutdatedLibraries() })
        }

        override fun calculate(key: LibraryInfo): LibraryDependenciesCache.LibraryDependencies =
            computeLibrariesAndSdksUsedWith(key)

        override fun checkKeyValidity(key: LibraryInfo) {
            key.checkValidity()
        }

        override fun checkValueValidity(value: LibraryDependenciesCache.LibraryDependencies) {
            value.checkValidity()
        }

        override fun rootsChanged(event: ModuleRootEvent) {
            if (event.isCausedByWorkspaceModelChangesOnly) return

            // SDK could be changed (esp in tests) out of message bus subscription
            val sdks = project.allSdks()
            invalidateEntries(
                { _, value -> value.sdk.any { it.sdk !in sdks } },
                // unable to check entities properly: an event could be not the last
                validityCondition = null
            )
        }

        inner class ModelChangeListener : ModuleEntityChangeListener(project) {
            override fun entitiesChanged(outdated: List<com.intellij.openapi.module.Module>) {
                invalidate(writeAccessRequired = true)
            }
        }

        inner class SdkChangeListener: SdkEntityChangeListener(project) {
            override fun entitiesChanged(outdated: List<Sdk>) {
                invalidateEntries(
                    { _, value -> value.sdk.any { it.sdk in outdated } },
                    // unable to check entities properly: an event could be not the last
                    validityCondition = null
                )
            }
        }

        @TestOnly
        @Suppress("deprecation_error")
        fun getCacheContentForTests() = cache
    }

    private inner class ModuleDependenciesCache :
        SynchronizedFineGrainedEntityCache< Module, LibraryDependencyCandidatesAndSdkInfos>(project, doSelfInitialization = false),
        WorkspaceModelChangeListener,
        LibraryInfoListener,
        ModuleRootListener {

        override fun subscribe() {
            val connection = project.messageBus.connect(this)
            connection.subscribe(WorkspaceModelTopics.CHANGED, this)
            connection.subscribe(LibraryInfoListener.TOPIC, this)
            connection.subscribe(ModuleRootListener.TOPIC, this)
        }

        @RequiresReadLock
        override fun get(key: com.intellij.openapi.module.Module): LibraryDependencyCandidatesAndSdkInfos {
            ThreadingAssertions.softAssertReadAccess()
            return internalGet(key, hashMapOf(), linkedSetOf(), hashMapOf())
        }

        private fun internalGet(
            key: com.intellij.openapi.module.Module,
            tmpResults: MutableMap<com.intellij.openapi.module.Module, LibraryDependencyCandidatesAndSdkInfosBuilder>,
            trace: LinkedHashSet<com.intellij.openapi.module.Module>,
            loops: MutableMap<com.intellij.openapi.module.Module, Set<com.intellij.openapi.module.Module>>
        ): LibraryDependencyCandidatesAndSdkInfos {
            checkKeyAndDisposeIllegalEntry(key)

            useCache { cache ->
                checkEntitiesIfRequired(cache)

                cache[key]
            }?.let { return it }

            checkCanceled()

            val newValue = computeLibrariesAndSdksUsedIn(key, tmpResults, trace, loops).build()

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
            key: com.intellij.openapi.module.Module,
            newValue: LibraryDependencyCandidatesAndSdkInfos,
            tmpResults: MutableMap<com.intellij.openapi.module.Module, LibraryDependencyCandidatesAndSdkInfosBuilder>,
            trace: LinkedHashSet<com.intellij.openapi.module.Module>,
            loops: MutableMap<com.intellij.openapi.module.Module, Set<com.intellij.openapi.module.Module>>,
        ): LibraryDependencyCandidatesAndSdkInfos? {
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

        private fun computeLibrariesAndSdksUsedIn(
            module: com.intellij.openapi.module.Module,
            tmpResults: MutableMap<com.intellij.openapi.module.Module, LibraryDependencyCandidatesAndSdkInfosBuilder>,
            trace: LinkedHashSet<com.intellij.openapi.module.Module>,
            loops: MutableMap<com.intellij.openapi.module.Module, Set<com.intellij.openapi.module.Module>>
        ): LibraryDependencyCandidatesAndSdkInfosBuilder {
            checkCanceled()
            check(trace.add(module)) { "recursion detected" }

            val libraryDependencyCandidatesAndSdkInfos = LibraryDependencyCandidatesAndSdkInfosBuilder()
            tmpResults[module] = libraryDependencyCandidatesAndSdkInfos

            val modulesToVisit = HashSet<com.intellij.openapi.module.Module>()

            val infoCache = LibraryInfoCache.getInstance(project)
            ModuleRootManager.getInstance(module).orderEntries()
                .process(object : RootPolicy<Unit>() {
                    override fun visitModuleOrderEntry(moduleOrderEntry: ModuleOrderEntry, value: Unit) {
                        moduleOrderEntry.module?.let(modulesToVisit::add)
                    }

                    override fun visitLibraryOrderEntry(libraryOrderEntry: LibraryOrderEntry, value: Unit) {
                        checkCanceled()
                        val libraryEx = libraryOrderEntry.library.safeAs<LibraryEx>()?.takeUnless { it.isDisposed } ?: return
                        val candidate = LibraryDependencyCandidate.fromLibraryOrNull(infoCache[libraryEx]) ?: return
                        libraryDependencyCandidatesAndSdkInfos += candidate
                    }

                    override fun visitJdkOrderEntry(jdkOrderEntry: JdkOrderEntry, value: Unit) {
                        checkCanceled()
                        jdkOrderEntry.jdk?.let { jdk ->
                            libraryDependencyCandidatesAndSdkInfos += SdkInfo(project, jdk)
                        }
                    }
                }, Unit)

            // handle circular dependency case
            for (moduleToVisit in modulesToVisit) {
                checkCanceled()
                if (moduleToVisit == module) continue

                if (moduleToVisit !in trace) continue

                // circular dependency found
                val reversedTrace = trace.toList().asReversed()

                val sharedLibraryDependencyCandidatesAndSdkInfos: LibraryDependencyCandidatesAndSdkInfosBuilder = run {
                    var shared: LibraryDependencyCandidatesAndSdkInfosBuilder? = null
                    val loop = hashSetOf<com.intellij.openapi.module.Module>()
                    val duplicates = hashSetOf<LibraryDependencyCandidatesAndSdkInfosBuilder>()
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
                    val traceModuleLibraryDependencyCandidatesAndSdkInfos: LibraryDependencyCandidatesAndSdkInfosBuilder =
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

        override fun calculate(key: com.intellij.openapi.module.Module): LibraryDependencyCandidatesAndSdkInfos =
            throw UnsupportedOperationException("calculate(Module) should not be invoked due to custom impl of get()")

        override fun checkKeyValidity(key: com.intellij.openapi.module.Module) {
            key.checkValidity()
        }

        override fun checkValueValidity(value: LibraryDependencyCandidatesAndSdkInfos) {
            value.libraryDependencyCandidates.forEach { it.libraries.forEach { libraryInfo -> libraryInfo.checkValidity() } }
        }

        override fun rootsChanged(event: ModuleRootEvent) {
            if (event.isCausedByWorkspaceModelChangesOnly) return

            // SDK could be changed (esp in tests) out of message bus subscription
            val sdks = project.allSdks()

            invalidateEntries(
                { _, candidates -> candidates.sdkInfos.any { it.sdk !in sdks } },
                // unable to check entities properly: an event could be not the last
                validityCondition = null
            )
        }

        override fun beforeChanged(event: VersionedStorageChange) {
            val storageBefore = event.storageBefore
            val moduleChanges = event.getChanges<ModuleEntity>()
            val sdkChanges = event.getChanges<SdkEntity>()

            if (moduleChanges.isEmpty() && sdkChanges.isEmpty()) return

            val outdatedModules = mutableSetOf<com.intellij.openapi.module.Module>()
            for (change in moduleChanges) {
                val moduleEntity = change.oldEntity ?: continue
                collectOutdatedModules(moduleEntity, storageBefore, outdatedModules)
            }

            val outdatedSdks = mutableSetOf<Sdk>()
            for (sdkChange in sdkChanges) {
                val sdk = sdkChange.oldEntity?.findSdkBridge(storageBefore)
                outdatedSdks.addIfNotNull(sdk)
            }
            if (outdatedModules.isNotEmpty()) {
                invalidateKeys(outdatedModules)
            }

            if (outdatedSdks.isNotEmpty()) {
                invalidateEntries(
                    { _, candidates -> candidates.sdkInfos.any { it.sdk in outdatedSdks } },
                    // unable to check entities properly: an event could be not the last
                    validityCondition = null
                )
            }
        }

        private fun collectOutdatedModules(moduleEntity: ModuleEntity, storage: EntityStorage, outdatedModules: MutableSet<Module>) {
            val module = moduleEntity.findModule(storage) ?: return

            if (!outdatedModules.add(module)) return

            storage.referrers(moduleEntity.symbolicId, ModuleEntity::class.java).forEach {
                collectOutdatedModules(it, storage, outdatedModules)
            }
        }

        override fun libraryInfosRemoved(libraryInfos: Collection<LibraryInfo>) {
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
