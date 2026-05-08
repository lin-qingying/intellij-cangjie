package org.cangnova.cangjie.facet

import com.intellij.facet.Facet
import com.intellij.facet.ui.FacetEditor
import com.intellij.facet.ui.MultipleFacetSettingsEditor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

/**
 * 仓颉 facet 类型默认实现。
 *
 * 对位 Kotlin `KotlinFacetTypeImpl`。
 */
class CangJieFacetTypeImpl : CangJieFacetType<CangJieFacetConfiguration>() {
    override fun createDefaultConfiguration(): CangJieFacetConfiguration = CangJieFacetConfigurationImpl()

    override fun createFacet(
        module: Module,
        name: String,
        configuration: CangJieFacetConfiguration,
        underlyingFacet: Facet<*>?,
    ): CangJieFacet = CangJieFacet(module, name, configuration)

    override fun createMultipleConfigurationsEditor(
        project: Project,
        editors: Array<out FacetEditor>,
    ): MultipleFacetSettingsEditor {
        return CangJieFacetEditorProviderService.getInstance(project).getMultipleConfigurationEditor(project, editors)
    }
}
