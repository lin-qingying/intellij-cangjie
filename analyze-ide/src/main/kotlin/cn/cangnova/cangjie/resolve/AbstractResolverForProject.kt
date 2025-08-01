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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.descriptors.ModuleDescriptorListener
import cn.cangnova.cangjie.descriptors.ModuleInfo
import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.cjpm.project.model.currentCjpmProject
import cn.cangnova.cangjie.context.ProjectContext
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import cn.cangnova.cangjie.resolve.caches.ModuleContent
import cn.cangnova.cangjie.utils.CangJieExceptionWithAttachments
import cn.cangnova.cangjie.utils.checkWithAttachment
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import kotlin.collections.get

fun createModuleDescriptor(projectContext: ProjectContext, project: Project): ModuleDescriptor {

    return ModuleDescriptorImpl(
        projectContext.project,
        Name.identifier(project.currentCjpmProject?.presentableName!!),
        projectContext.storageManager,
        CangJieBuiltIns(projectContext.project, projectContext.storageManager)
    )
}

abstract class AbstractResolverForProject<M : ModuleInfo>(

    private val debugName: String,
    protected val projectContext: ProjectContext,
    modules: Collection<M>,
    protected val fallbackModificationTracker: ModificationTracker? = null,
    private val delegateResolver: ResolverForProject<M> = EmptyResolverForProject(),
    private val packageOracleFactory: PackageOracleFactory = PackageOracleFactory.OptimisticFactory
) : ResolverForProject<M>(), Disposable {
    @Volatile
    protected var disposed = false

    // Protected by ("projectContext.storageManager.lock")
    protected val descriptorByModule = hashMapOf<M, ModuleData>()
    override val name: String
        get() = "Resolver for '$debugName'"

    override fun dispose() {
        projectContext.storageManager.compute {
            disposed = true
            descriptorByModule.values.forEach {
                moduleInfoByDescriptor.remove(it.moduleDescriptor)
                it.moduleDescriptor.isValid = false
            }
            descriptorByModule.clear()
            moduleInfoByDescriptor.keys.forEach { it.isValid = false }
            moduleInfoByDescriptor.clear()
        }
    }

    protected class ModuleData(
        val moduleDescriptor: ModuleDescriptorImpl,
        val modificationTracker: ModificationTracker?
    ) {
        val modificationCount: Long = modificationTracker?.modificationCount ?: Long.MIN_VALUE

        fun isOutOfDate(): Boolean {
            val currentModCount = modificationTracker?.modificationCount
            return currentModCount != null && currentModCount > modificationCount
        }
    }

    internal fun isResolverForModuleDescriptorComputed(descriptor: ModuleDescriptor) =
        projectContext.storageManager.compute {
            descriptor in resolverByModuleDescriptor
        }

    // Protected by ("projectContext.storageManager.lock")
    private val moduleInfoByDescriptor = hashMapOf<ModuleDescriptorImpl, M>()

    // Protected by ("projectContext.storageManager.lock")
    private val resolverByModuleDescriptor = hashMapOf<ModuleDescriptor, ResolverForModule>()

    @Suppress("UNCHECKED_CAST")
    private val moduleInfoToResolvableInfo: Map<M, M> =
        modules.flatMap { module -> module.flatten().map { modulePart -> modulePart to module } }.toMap() as Map<M, M>

    override val allModules: Collection<M> by lazy {
        this.moduleInfoToResolvableInfo.keys + delegateResolver.allModules
    }

    abstract fun createResolverForModule(descriptor: ModuleDescriptor, moduleInfo: M): ResolverForModule
    abstract fun builtInsForModule(module: M): CangJieBuiltIns

    override fun diagnoseUnknownModuleInfo(infos: List<ModuleInfo>): Nothing {
        DiagnoseUnknownModuleInfoReporter.report(name, infos, allModules)

    }

    private fun checkValid() {
        if (disposed) {
            reportInvalidResolver()
        }
    }

    abstract fun modulesContent(module: M): ModuleContent<M>

    protected open fun reportInvalidResolver() {
        throw InvalidResolverException("$name is invalidated")
    }

    override fun tryGetResolverForModule(moduleInfo: M): ResolverForModule? {
        checkValid()
        if (!isCorrectModuleInfo(moduleInfo)) {
            return null
        }
        return resolverForModuleDescriptor(doGetDescriptorForModule(moduleInfo))
    }


    private fun isCorrectModuleInfo(moduleInfo: M): Boolean =
        ((moduleInfo as? DerivedModuleInfo)?.originalModule ?: moduleInfo) in allModules

    private fun recreateModuleDescriptor(module: M): ModuleData {
        val oldDescriptor = descriptorByModule[module]?.moduleDescriptor
        if (oldDescriptor != null) {
            oldDescriptor.isValid = false
            moduleInfoByDescriptor.remove(oldDescriptor)
            resolverByModuleDescriptor.remove(oldDescriptor)
            projectContext.project.messageBus.syncPublisher(ModuleDescriptorListener.Companion.TOPIC)
                .moduleDescriptorInvalidated(oldDescriptor)
        }

        val moduleData = createModuleDescriptor(module)
        descriptorByModule[module] = moduleData

        return moduleData
    }

    private fun createModuleDescriptor(module: M): ModuleData {
        val moduleDescriptor = ModuleDescriptorImpl(
            projectContext.project,
            module.name,
            projectContext.storageManager,
            builtInsForModule(module),
//            module.platform,
//            module.capabilities + getAdditionalCapabilities(),
//            module.stableName,
        )
        moduleInfoByDescriptor[moduleDescriptor] = module
        setupModuleDescriptor(module, moduleDescriptor)
        val modificationTracker =
            (module as? TrackableModuleInfo)?.createModificationTracker() ?: fallbackModificationTracker
        return ModuleData(moduleDescriptor, modificationTracker)
    }

    private fun setupModuleDescriptor(module: M, moduleDescriptor: ModuleDescriptorImpl) {
        checkValid()
        moduleDescriptor.setDependencies(
            LazyModuleDependencies(
                projectContext.storageManager,
                module,
                /*  sdkDependency(module)*/null,
                this
            )
        )

        val content = modulesContent(module)
        moduleDescriptor.initialize(
            DelegatingPackageFragmentProvider(
                this, moduleDescriptor, content,
                packageOracleFactory.createOracle(module)
            )
        )
    }

    private fun checkModuleIsCorrect(moduleInfo: M) {
        if (!isCorrectModuleInfo(moduleInfo)) {
            diagnoseUnknownModuleInfo(listOf(moduleInfo))
        }
    }

    override fun descriptorForModule(moduleInfo: M): ModuleDescriptorImpl {
        checkValid()
        checkModuleIsCorrect(moduleInfo)
        return doGetDescriptorForModule(moduleInfo)
    }

    private fun doGetDescriptorForModule(module: M): ModuleDescriptorImpl {
        val moduleFromThisResolver =
            module.takeIf { it is DerivedModuleInfo && it.originalModule in moduleInfoToResolvableInfo }
                ?: moduleInfoToResolvableInfo[module]
                ?: return delegateResolver.descriptorForModule(module) as ModuleDescriptorImpl

        return projectContext.storageManager.compute {
            var moduleData = descriptorByModule.getOrPut(moduleFromThisResolver) {
                createModuleDescriptor(moduleFromThisResolver)
            }
            if (moduleData.isOutOfDate()) {
                moduleData = recreateModuleDescriptor(moduleFromThisResolver)
            }
            moduleData.moduleDescriptor
        }
    }

    private fun renderResolversChainContents(): String {
        val resolversChain = generateSequence(this) { it.delegateResolver as? AbstractResolverForProject<M> }

        return resolversChain.joinToString("\n\n") { resolver ->
            "Resolver: ${resolver.name}\n'moduleInfoByDescriptor' content:\n[${resolver.renderResolverModuleInfos()}]"
        }
    }

    private fun renderResolverModuleInfos(): String = projectContext.storageManager.compute {
        moduleInfoByDescriptor.entries.joinToString(",\n") { (descriptor, moduleInfo) ->
            """
            {
                moduleDescriptor: $descriptor
                moduleInfo: $moduleInfo
            }
            """.trimIndent()
        }
    }

    private fun resolverForModuleDescriptorImpl(descriptor: ModuleDescriptor): ResolverForModule? {
        return projectContext.storageManager.compute {
            checkValid()
            descriptor.assertValid()

            val module = moduleInfoByDescriptor[descriptor]
            if (module == null) {
                if (delegateResolver is EmptyResolverForProject<*>) {
                    return@compute null
                }
                return@compute (delegateResolver as AbstractResolverForProject<M>).resolverForModuleDescriptorImpl(
                    descriptor
                )
            }
            resolverByModuleDescriptor.getOrPut(descriptor) {
                checkModuleIsCorrect(module)

                ResolverForModuleComputationTracker.Companion.getInstance(projectContext.project)?.onResolverComputed(module)

                createResolverForModule(descriptor, module)
            }
        }
    }

    final override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule {
        val moduleResolver = resolverForModuleDescriptorImpl(descriptor)


        checkWithAttachment(
            moduleResolver != null,
            lazyMessage = { "$descriptor is not contained in resolver $name" },
            attachments = {
                it.withAttachment(
                    "resolverContents.txt",
                    "Expected module descriptor: $descriptor\n\n${renderResolversChainContents()}"
                )
            }
        )

        return moduleResolver
    }
}

class InvalidResolverException(message: String) : IllegalStateException(message)
private object DiagnoseUnknownModuleInfoReporter {
    fun report(name: String, infos: List<ModuleInfo>, allModules: Collection<ModuleInfo>): Nothing {
        val message = "$name does not know how to resolve"
        val error = when {

            name.contains(ResolverForProject.Companion.resolverForLibrariesName) -> errorInLibrariesResolver(message)
            name.contains(ResolverForProject.Companion.resolverForModulesName) -> {
                when {
                    infos.isEmpty() -> errorInModulesResolverWithEmptyInfos(message)
                    infos.size == 1 -> {
                        val infoAsString = infos.single().toString()
                        when {
                            infoAsString.contains("ScriptDependencies") -> errorInModulesResolverWithScriptDependencies(message)
                            infoAsString.contains("Library") -> errorInModulesResolverWithLibraryInfo(message)
                            else -> errorInModulesResolver(message)
                        }
                    }

                    else -> errorInModulesResolver(message)
                }
            }


            name.contains(ResolverForProject.Companion.resolverForSpecialInfoName) -> {
                when {
                    name.contains("ScriptModuleInfo") -> errorInScriptModuleInfoResolver(message)
                    else -> errorInSpecialModuleInfoResolver(message)
                }
            }

            else -> otherError(message)
        }

        throw error.withAttachment("infos.txt", infos).withAttachment("allModules.txt", allModules)
    }

    // Do not inline 'error*'-methods, they are needed to avoid Exception Analyzer merging those AssertionErrors

    private fun errorInSdkResolver(message: String) = CangJieExceptionWithAttachments(message)
    private fun errorInLibrariesResolver(message: String) = CangJieExceptionWithAttachments(message)
    private fun errorInModulesResolver(message: String) = CangJieExceptionWithAttachments(message)

    private fun errorInModulesResolverWithEmptyInfos(message: String) = CangJieExceptionWithAttachments(message)
    private fun errorInModulesResolverWithScriptDependencies(message: String) = CangJieExceptionWithAttachments(message)
    private fun errorInModulesResolverWithLibraryInfo(message: String) = CangJieExceptionWithAttachments(message)

    private fun errorInScriptDependenciesInfoResolver(message: String) = CangJieExceptionWithAttachments(message)
    private fun errorInScriptModuleInfoResolver(message: String) = CangJieExceptionWithAttachments(message)
    private fun errorInSpecialModuleInfoResolver(message: String) = CangJieExceptionWithAttachments(message)

    private fun otherError(message: String) = CangJieExceptionWithAttachments(message)
}

private class DelegatingPackageFragmentProvider<M : ModuleInfo>(
    private val resolverForProject: AbstractResolverForProject<M>,
    private val module: ModuleDescriptor,
    moduleContent: ModuleContent<M>,
    private val packageOracle: PackageOracle
) : PackageFragmentProviderOptimized {
    private val syntheticFilePackages = moduleContent.syntheticFiles.map { it.packageFqName }.toSet()

    @Suppress("OverridingDeprecatedMember", "OVERRIDE_DEPRECATION")
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        if (certainlyDoesNotExist(fqName)) return emptyList()

        @Suppress("DEPRECATION")
        return resolverForProject.resolverForModuleDescriptor(module).packageFragmentProvider.getPackageFragments(fqName)
    }

    override fun collectPackageFragments(
        fqName: FqName,
        packageFragments: MutableCollection<PackageFragmentDescriptor>
    ) {
        if (certainlyDoesNotExist(fqName)) return

        resolverForProject.resolverForModuleDescriptor(module)
            .packageFragmentProvider
            .collectPackageFragmentsOptimizedIfPossible(fqName, packageFragments)
    }

    override fun isEmpty(fqName: FqName): Boolean {
        if (certainlyDoesNotExist(fqName)) return true

        return resolverForProject.resolverForModuleDescriptor(module).packageFragmentProvider.isEmpty(fqName)
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        if (certainlyDoesNotExist(fqName)) return emptyList()

        return resolverForProject.resolverForModuleDescriptor(module).packageFragmentProvider.getSubPackagesOf(
            fqName,
            nameFilter
        )
    }

    private fun certainlyDoesNotExist(fqName: FqName): Boolean {
        if (resolverForProject.isResolverForModuleDescriptorComputed(module)) return false // let this request get cached inside delegate

        return !packageOracle.packageExists(fqName) && fqName !in syntheticFilePackages
    }

    override fun toString(): String {
        return "DelegatingProvider for $module in ${resolverForProject.name}"
    }
}
