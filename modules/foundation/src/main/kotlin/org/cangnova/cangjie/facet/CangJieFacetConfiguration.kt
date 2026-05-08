package org.cangnova.cangjie.facet

import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager

/**
 * 仓颉 facet 配置接口。
 *
 * 对位 Kotlin `KotlinFacetConfiguration`。
 */
interface CangJieFacetConfiguration : FacetConfiguration {
    val settings: CangJieFacetSettings

    override fun createEditorTabs(
        editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager,
    ): Array<FacetEditorTab> {
        settings.initializeIfNeeded(editorContext.module, editorContext.rootModel)

        val tabs = arrayListOf<FacetEditorTab>()
        tabs += CangJieFacetEditorProviderService.getInstance(editorContext.project)
            .getEditorTabs(this, editorContext, validatorsManager)
        CangJieFacetConfigurationExtension.EP_NAME.extensionList
            .flatMapTo(tabs) { extension -> extension.createEditorTabs(editorContext, validatorsManager) }
        return tabs.toTypedArray()
    }
}
