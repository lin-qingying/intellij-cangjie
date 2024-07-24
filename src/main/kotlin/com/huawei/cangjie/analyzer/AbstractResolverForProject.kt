package com.huawei.cangjie.analyzer

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.cjpm.project.model.currentCjpmProject
import com.huawei.cangjie.context.ProjectContext
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.impl.ModuleDescriptorImpl
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.utils.checkWithAttachment
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker

fun createModuleDescriptor(projectContext: ProjectContext, project: Project): ModuleDescriptor {

    return ModuleDescriptorImpl(
        Name.identifier(project.currentCjpmProject?.presentableName!!),
        projectContext.storageManager,
        CangJieBuiltIns(projectContext.storageManager)
    )
}

abstract class AbstractResolverForProject<M : ModuleInfo>(

    private val debugName: String,
    protected val projectContext: ProjectContext,
    modules: Collection<M>,
    protected val fallbackModificationTracker: ModificationTracker? = null,
    private val delegateResolver: ResolverForProject<M> = EmptyResolverForProject(),
//    private val packageOracleFactory: PackageOracleFactory = PackageOracleFactory.OptimisticFactory
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
//        DiagnoseUnknownModuleInfoReporter.report(name, infos, allModules)

        throw RuntimeException("Unknown module info")
    }

    private fun checkValid() {
        if (disposed) {
            reportInvalidResolver()
        }
    }

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
            projectContext.project.messageBus.syncPublisher(ModuleDescriptorListener.TOPIC)
                .moduleDescriptorInvalidated(oldDescriptor)
        }

        val moduleData = createModuleDescriptor(module)
        descriptorByModule[module] = moduleData

        return moduleData
    }

    private fun createModuleDescriptor(module: M): ModuleData {
        val moduleDescriptor = ModuleDescriptorImpl(
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
//        moduleDescriptor.setDependencies(
//            LazyModuleDependencies(
//                projectContext.storageManager,
//                module,
//                sdkDependency(module),
//                this
//            )
//        )
//
//        val content = modulesContent(module)
//        moduleDescriptor.initialize(
//            DelegatingPackageFragmentProvider(
//                this, moduleDescriptor, content,
//                packageOracleFactory.createOracle(module)
//            )
//        )
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

                ResolverForModuleComputationTracker.getInstance(projectContext.project)?.onResolverComputed(module)

                createResolverForModule(descriptor, module)
            }
        }
    }

    final override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule {
        val moduleResolver = resolverForModuleDescriptorImpl(descriptor)

        // Please, attach exceptions from here to EA-214260 (see `resolverForModuleDescriptorImpl` comment)
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
