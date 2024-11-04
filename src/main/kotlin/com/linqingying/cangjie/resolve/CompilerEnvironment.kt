package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.container.StorageComponentContainer
import com.linqingying.cangjie.container.useInstance
import com.linqingying.cangjie.resolve.controlFlow.ControlFlowInformationProviderImpl


object CompilerEnvironment : TargetEnvironment("Compiler") {
    override fun configure(container: StorageComponentContainer) {
        configureCompilerEnvironment(container)
        container.useInstance(ControlFlowInformationProviderImpl.Factory)
    }
}
