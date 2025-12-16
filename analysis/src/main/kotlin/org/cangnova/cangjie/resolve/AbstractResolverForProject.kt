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

package org.cangnova.cangjie.resolve

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.context.ProjectContext
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.caches.ModuleContent
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachmentsImpl
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment

fun createModuleDescriptor(
    projectDescriptor: ProjectDescriptor,
    moduleName: String,
    projectContext: ProjectContext
): ModuleDescriptor {
    return ModuleDescriptorImpl(
        projectDescriptor,
        Name.identifier(moduleName),
        projectContext.storageManager,
        mapOf(AnalysisContextCapability to NotUnderContentRootModuleInfo(projectDescriptor.project))
    )
}

abstract class AbstractResolverForProject<M : AnalysisContext>(

    private val debugName: String,
    protected val projectContext: ProjectContext,
    protected val projectDescriptor: ProjectDescriptor,
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
                contextByDescriptor.remove(it.moduleDescriptor)
                it.moduleDescriptor.isValid = false
            }
            descriptorByModule.clear()
            contextByDescriptor.keys.forEach { it.isValid = false }
            contextByDescriptor.clear()
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
    private val contextByDescriptor = hashMapOf<ModuleDescriptorImpl, M>()

    // Protected by ("projectContext.storageManager.lock")
    private val resolverByModuleDescriptor = hashMapOf<ModuleDescriptor, ResolverForModule>()

    @Suppress("UNCHECKED_CAST")
    private val contextToResolvableInfo: Map<M, M> =
        // 在新的 AnalysisContext 系统中，每个上下文都是独立的，不需要 flatten
        modules.associateWith { it } as Map<M, M>

    override val allModules: Collection<M> by lazy {
        this.contextToResolvableInfo.keys + delegateResolver.allModules
    }

    abstract fun createResolverForModule(descriptor: ModuleDescriptor, context: M): ResolverForModule

    override fun diagnoseUnknownContext(contexts: List<AnalysisContext>): Nothing {
        DiagnoseUnknownContextReporter.report(name, contexts, allModules)

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

    override fun tryGetResolverForModule(context: M): ResolverForModule? {
        checkValid()
        if (!isCorrectContext(context)) {
            return null
        }
        return resolverForModuleDescriptor(doGetDescriptorForModule(context))
    }


    private fun isCorrectContext(context: M): Boolean =
        ((context as? DerivedAnalysisContext)?.originalContext ?: context) in allModules

    private fun recreateModuleDescriptor(module: M): ModuleData {
        val oldDescriptor = descriptorByModule[module]?.moduleDescriptor
        if (oldDescriptor != null) {
            oldDescriptor.isValid = false
            contextByDescriptor.remove(oldDescriptor)
            resolverByModuleDescriptor.remove(oldDescriptor)
            // ModuleDescriptorListener 不再可用，直接跳过
            // projectContext.project.messageBus.syncPublisher(ModuleDescriptorListener.Companion.TOPIC)
            //     .moduleDescriptorInvalidated(oldDescriptor)
        }

        val moduleData = createModuleDescriptor(module)
        descriptorByModule[module] = moduleData

        return moduleData
    }

    private fun createModuleDescriptor(module: M): ModuleData {
        val moduleDescriptor = ModuleDescriptorImpl(
            projectDescriptor,
            Name.identifier(module.contextId),
            projectContext.storageManager,
            mapOf(AnalysisContextCapability to module),
            null // stableName
            // isBuiltInsModule
        )
        contextByDescriptor[moduleDescriptor] = module

        // 自动将模块注册到 ProjectDescriptor 中
        // 这样可以通过 projectDescriptor.getModule() 获取模块
        try {
            projectDescriptor.addModule(moduleDescriptor)
        } catch (e: IllegalArgumentException) {
            // 模块已存在，忽略异常
            // 这种情况可能发生在模块被重新创建时
        }

        setupModuleDescriptor(module, moduleDescriptor)
        val modificationTracker =
            (module as? TrackableAnalysisContext)?.createModificationTracker() ?: fallbackModificationTracker
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

    private fun checkModuleIsCorrect(context: M) {
        if (!isCorrectContext(context)) {
            diagnoseUnknownContext(listOf(context))
        }
    }

    override fun descriptorForModule(context: M): ModuleDescriptorImpl {
        checkValid()
        checkModuleIsCorrect(context)
        return doGetDescriptorForModule(context)
    }

    private fun doGetDescriptorForModule(module: M): ModuleDescriptorImpl {
        val moduleFromThisResolver =
            module.takeIf { it is DerivedAnalysisContext && it.originalContext in contextToResolvableInfo }
                ?: contextToResolvableInfo[module]
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
            "Resolver: ${resolver.name}\n'contextByDescriptor' content:\n[${resolver.renderResolverContexts()}]"
        }
    }

    private fun renderResolverContexts(): String = projectContext.storageManager.compute {
        contextByDescriptor.entries.joinToString(",\n") { (descriptor, context) ->
            """
            {
                moduleDescriptor: $descriptor
                context: $context
            }
            """.trimIndent()
        }
    }

    private fun resolverForModuleDescriptorImpl(descriptor: ModuleDescriptor): ResolverForModule? {
        return projectContext.storageManager.compute {
            checkValid()
            descriptor.assertValid()

            val module = contextByDescriptor[descriptor]
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

                ResolverForModuleComputationTracker.getInstance(projectContext.project)
                    ?.onResolverComputed(module)

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
private object DiagnoseUnknownContextReporter {
    fun report(name: String, contexts: List<AnalysisContext>, allModules: Collection<AnalysisContext>): Nothing {
        val message = "$name does not know how to resolve"
        val error = when {

            name.contains(ResolverForProject.resolverForLibrariesName) -> errorInLibrariesResolver(message)
            name.contains(ResolverForProject.resolverForModulesName) -> {
                when {
                    contexts.isEmpty() -> errorInModulesResolverWithEmptyInfos(message)
                    contexts.size == 1 -> {
                        val contextAsString = contexts.single().toString()
                        when {
                            contextAsString.contains("ScriptDependencies") -> errorInModulesResolverWithScriptDependencies(
                                message
                            )

                            contextAsString.contains("Library") -> errorInModulesResolverWithLibraryInfo(message)
                            else -> errorInModulesResolver(message)
                        }
                    }

                    else -> errorInModulesResolver(message)
                }
            }


            name.contains(ResolverForProject.resolverForSpecialInfoName) -> {
                when {
                    name.contains("ScriptModuleInfo") -> errorInScriptModuleInfoResolver(message)
                    else -> errorInSpecialModuleInfoResolver(message)
                }
            }

            else -> otherError(message)
        }

        throw error.withAttachment("contexts.txt", contexts).withAttachment("allModules.txt", allModules)
    }

    // Do not inline 'error*'-methods, they are needed to avoid Exception Analyzer merging those AssertionErrors

    private fun errorInSdkResolver(message: String) = CangJieExceptionWithAttachmentsImpl(message)
    private fun errorInLibrariesResolver(message: String) = CangJieExceptionWithAttachmentsImpl(message)
    private fun errorInModulesResolver(message: String) = CangJieExceptionWithAttachmentsImpl(message)

    private fun errorInModulesResolverWithEmptyInfos(message: String) = CangJieExceptionWithAttachmentsImpl(message)
    private fun errorInModulesResolverWithScriptDependencies(message: String) = CangJieExceptionWithAttachmentsImpl(message)
    private fun errorInModulesResolverWithLibraryInfo(message: String) = CangJieExceptionWithAttachmentsImpl(message)

    private fun errorInScriptDependenciesInfoResolver(message: String) = CangJieExceptionWithAttachmentsImpl(message)
    private fun errorInScriptModuleInfoResolver(message: String) = CangJieExceptionWithAttachmentsImpl(message)
    private fun errorInSpecialModuleInfoResolver(message: String) = CangJieExceptionWithAttachmentsImpl(message)

    private fun otherError(message: String) = CangJieExceptionWithAttachmentsImpl(message)
}

private class DelegatingPackageFragmentProvider<M : AnalysisContext>(
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
