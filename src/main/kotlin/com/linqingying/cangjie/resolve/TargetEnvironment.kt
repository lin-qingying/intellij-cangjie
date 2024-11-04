package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.container.StorageComponentContainer
import com.linqingying.cangjie.container.useImpl
import com.linqingying.cangjie.container.useInstance
import com.linqingying.cangjie.resolve.lazy.BasicAbsentDescriptorHandler
import com.linqingying.cangjie.resolve.lazy.CompilerLocalDescriptorResolver

abstract class TargetEnvironment(private val name: String) {
    abstract fun configure(container: StorageComponentContainer)

    override fun toString() = name

    companion object {
        fun configureCompilerEnvironment(container: StorageComponentContainer) {
            container.useInstance(BodyResolveCache.ThrowException)
            container.useImpl<CompilerLocalDescriptorResolver>()
            container.useImpl<BasicAbsentDescriptorHandler>()
//            container.useImpl<MainFunctionDetector.Factory.Ordinary>()
        }
    }
}
