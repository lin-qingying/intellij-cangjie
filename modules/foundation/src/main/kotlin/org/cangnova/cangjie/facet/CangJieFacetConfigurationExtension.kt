package org.cangnova.cangjie.facet

import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * 仓颉 facet 配置扩展点。
 *
 * 对位 Kotlin `KotlinFacetConfigurationExtension`。
 */
interface CangJieFacetConfigurationExtension {
    companion object {
        val EP_NAME: ExtensionPointName<CangJieFacetConfigurationExtension> =
            ExtensionPointName.create("org.cangnova.cangjie.facetConfigurationExtension")
    }

    fun createEditorTabs(
        editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager,
    ): List<FacetEditorTab>
}
