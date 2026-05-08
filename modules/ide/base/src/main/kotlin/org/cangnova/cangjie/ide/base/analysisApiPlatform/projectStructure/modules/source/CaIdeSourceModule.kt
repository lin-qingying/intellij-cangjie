package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.modules.source

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleId
import org.cangnova.cangjie.ide.base.facet.stableName
import org.cangnova.cangjie.projectStructure.CaSourceModuleKind

/**
 * IDE 中的项目源码模块。
 */
internal class CaIdeSourceModule(
    project: Project,
    entityId: ModuleId,
    kind: CaSourceModuleKind,
) : CaIdeSourceModuleBase(
    project = project,
    entityId = entityId,
    kind = kind,
) {
    override val stableModuleName: String
        get() = "${openapiModule.stableName.asString()}:${if (kind == CaSourceModuleKind.TEST) "test" else "production"}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is CaIdeSourceModule &&
            entityId == other.entityId &&
            kind == other.kind
    }

    override fun hashCode(): Int {
        return 31 * entityId.hashCode() + kind.hashCode()
    }
}
