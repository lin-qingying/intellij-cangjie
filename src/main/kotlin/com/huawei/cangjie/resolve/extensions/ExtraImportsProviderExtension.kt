package com.huawei.cangjie.resolve.extensions

import com.huawei.cangjie.extensions.ProjectExtensionDescriptor
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.psi.CjImportInfo
import com.intellij.openapi.project.Project

interface ExtraImportsProviderExtension {
    companion object : ProjectExtensionDescriptor<ExtraImportsProviderExtension>(
        "com.huawei.cangjie.extraImportsProviderExtension", ExtraImportsProviderExtension::class.java
    ) {

        private class CompoundExtraImportsProviderExtension(val instances: List<ExtraImportsProviderExtension>) : ExtraImportsProviderExtension {
            override fun getExtraImports(ktFile: CjFile): Collection<CjImportInfo> = instances.flatMap {
                withLinkageErrorLogger(it) { getExtraImports(ktFile) }
            }
        }

        fun getInstance(project: Project): ExtraImportsProviderExtension {
            val instances = getInstances(project)
            return instances.singleOrNull() ?: CompoundExtraImportsProviderExtension(instances)
        }
    }

    fun getExtraImports(ktFile: CjFile): Collection<CjImportInfo>
}
