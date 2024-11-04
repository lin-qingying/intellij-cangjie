package com.linqingying.cangjie.extensions

import com.linqingying.cangjie.config.CompilerConfiguration
import com.linqingying.cangjie.psi.CjFile

interface ProcessSourcesBeforeCompilingExtension {

    companion object : ProjectExtensionDescriptor<ProcessSourcesBeforeCompilingExtension>(
        "com.linqingying.cangjie.processSourcesBeforeCompilingExtension",
        ProcessSourcesBeforeCompilingExtension::class.java
    )

    fun processSources(
        sources: Collection<CjFile>,
        configuration: CompilerConfiguration
    ): Collection<CjFile>
}
