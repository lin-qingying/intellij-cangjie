package org.cangnova.cangjie.facet

import com.intellij.facet.ui.FacetEditor
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.facet.ui.MultipleFacetSettingsEditor
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * 仓颉 facet 编辑器服务。
 *
 * 对位 Kotlin `KotlinFacetEditorProviderService`。
 */
interface CangJieFacetEditorProviderService {
    fun getEditorTabs(
        configuration: CangJieFacetConfiguration,
        editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager,
    ): List<FacetEditorTab>

    fun getMultipleConfigurationEditor(
        project: Project,
        editors: Array<out FacetEditor>,
    ): MultipleFacetSettingsEditor

    companion object {
        @JvmStatic
        fun getInstance(project: Project): CangJieFacetEditorProviderService = project.service()
    }
}
