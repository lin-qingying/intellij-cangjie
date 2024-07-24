package com.huawei.cangjie.analyzer

import com.huawei.cangjie.container.ComponentProvider
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.PackageFragmentProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker

interface TrackableModuleInfo : ModuleInfo {
    fun createModificationTracker(): ModificationTracker
}
fun ModuleInfo.flatten(): List<ModuleInfo> = when (this) {
//    is CombinedModuleInfo -> listOf(this) + containedModules
    else -> listOf(this)
}
//interface PackageOracleFactory {
//    fun createOracle(moduleInfo: ModuleInfo): PackageOracle
//
//    object OptimisticFactory : PackageOracleFactory {
//        override fun createOracle(moduleInfo: ModuleInfo) = PackageOracle.Optimistic
//    }
//}
/**
 * Special-purpose module info that allows implementors to provide different behavior compared to the [originalModule]'s.
 * E.g. may be used to resolve common code as if it were target-specific, or to change the dependencies visible to the code.
 *
 * Resolvers should accept a derived module info, iff the [originalModule] is accepted.
 */
interface DerivedModuleInfo : ModuleInfo {
    val originalModule: ModuleInfo
}

class ResolverForModule(
    val packageFragmentProvider: PackageFragmentProvider,
    val componentProvider: ComponentProvider
)

class EmptyResolverForProject<M : ModuleInfo> : ResolverForProject<M>() {
    override val name: String
        get() = "Empty resolver"

    override fun tryGetResolverForModule(moduleInfo: M): ResolverForModule? = null
    override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule =
        throw IllegalStateException("$descriptor is not contained in this resolver")

    override fun descriptorForModule(moduleInfo: M) = diagnoseUnknownModuleInfo(listOf(moduleInfo))
    override val allModules: Collection<M> = listOf()
    override fun diagnoseUnknownModuleInfo(infos: List<ModuleInfo>) =
        throw IllegalStateException("Should not be called for $infos")

//    override fun moduleInfoForModuleDescriptor(moduleDescriptor: ModuleDescriptor): M {
//        throw IllegalStateException("$moduleDescriptor is not contained in this resolver")
//    }
}

abstract class ResolverForProject<M : ModuleInfo> {
    abstract val allModules: Collection<M>

    abstract val name: String
    abstract fun descriptorForModule(moduleInfo: M): ModuleDescriptor
    abstract fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule

    abstract fun diagnoseUnknownModuleInfo(infos: List<ModuleInfo>): Nothing

    abstract fun tryGetResolverForModule(moduleInfo: M): ResolverForModule?

}

interface ResolverForModuleComputationTracker {

    fun onResolverComputed(moduleInfo: ModuleInfo)

    companion object {
        fun getInstance(project: Project): ResolverForModuleComputationTracker? =
            project.getComponent(ResolverForModuleComputationTracker::class.java) ?: null
    }
}
