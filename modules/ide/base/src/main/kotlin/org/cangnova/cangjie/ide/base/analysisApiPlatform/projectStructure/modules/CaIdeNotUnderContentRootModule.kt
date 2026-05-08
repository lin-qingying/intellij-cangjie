package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule

/**
 * IDE 中的不在 content root 下的临时模块。
 */
internal class CaIdeNotUnderContentRootModule(
    project: Project,
    val item: PsiFileSystemItem,
    private val pathKey: String,
) : CaIdeMutableModule(project, { listOf(item) }, includeLibrariesInScope = false), CaNotUnderContentRootModule {
    override val name: String
        get() = item.name.ifBlank { pathKey.substringAfterLast('/', pathKey) }

    override val originalModule: CaModule?
        get() = null

    override val stableModuleName: String
        get() = "ide-outside:$pathKey"

    override val moduleDescription: String
        get() = "IDE not-under-content-root $pathKey"
}
