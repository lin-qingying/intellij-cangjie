package com.linqingying.cangjie.resolve.extensions

import com.linqingying.cangjie.extensions.ProjectExtensionDescriptor
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjImportInfo
import com.intellij.openapi.project.Project

interface ExtraImportsProviderExtension {
    companion object : ProjectExtensionDescriptor<ExtraImportsProviderExtension>(
        "com.linqingying.cangjie.extraImportsProviderExtension", ExtraImportsProviderExtension::class.java
    ) {

        private class CompoundExtraImportsProviderExtension(val instances: List<ExtraImportsProviderExtension>) : ExtraImportsProviderExtension {
            override fun getExtraImports(cjFile: CjFile): Collection<CjImportInfo> = instances.flatMap {
                withLinkageErrorLogger(it) { getExtraImports(cjFile) }
            }
        }

        fun getInstance(project: Project): ExtraImportsProviderExtension {
            val instances = getInstances(project)
            return instances.singleOrNull() ?: CompoundExtraImportsProviderExtension(instances)
        }
    }

    fun getExtraImports(cjFile: CjFile): Collection<CjImportInfo>
}
