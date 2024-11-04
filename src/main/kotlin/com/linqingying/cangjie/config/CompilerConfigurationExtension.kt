package com.linqingying.cangjie.config

import com.linqingying.cangjie.extensions.ProjectExtensionDescriptor

interface CompilerConfigurationExtension {

    companion object : ProjectExtensionDescriptor<CompilerConfigurationExtension>(
        "com.linqingying.cangjie.compilerConfigurationExtension",
        CompilerConfigurationExtension::class.java
    )

    fun updateConfiguration(configuration: CompilerConfiguration)

    fun updateFileRegistry() {}
}
