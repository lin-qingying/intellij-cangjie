package com.linqingying.cangjie.extensions

import com.intellij.openapi.project.Project
import com.linqingying.cangjie.config.CompilerConfiguration
import com.linqingying.cangjie.psi.CjFile

interface CollectAdditionalSourcesExtension {
    companion object : ProjectExtensionDescriptor<CollectAdditionalSourcesExtension>(
        "com.linqingying.cangjie.collectAdditionalSourcesExtension",
        CollectAdditionalSourcesExtension::class.java
    )

    fun collectAdditionalSourcesAndUpdateConfiguration(
        knownSources: Collection<CjFile>,
        configuration: CompilerConfiguration,
        project: Project
    ): Collection<CjFile>
}
