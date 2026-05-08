package org.cangnova.cangjie.ide.base.compilerPreferences

import com.intellij.facet.ui.FacetEditor
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.facet.ui.MultipleFacetSettingsEditor
import org.cangnova.cangjie.facet.CangJieFacetConfiguration
import org.cangnova.cangjie.facet.CangJieFacetEditorProviderService
import org.cangnova.cangjie.ide.base.compilerPreferences.facet.CangJieFacetCompilerPluginsTab
import org.cangnova.cangjie.ide.base.compilerPreferences.facet.CangJieFacetEditorGeneralTab
import org.cangnova.cangjie.ide.base.compilerPreferences.facet.MultipleCangJieFacetEditor

/**
 * 仓颉 facet 编辑器默认服务。
 *
 * 对位 Kotlin `KotlinFacetEditorProviderServiceImpl`。
 * 仓颉没有多平台配置面，只有工具链相关配置入口；facet UI 入口仍应集中在这里。
 */
class CangJieFacetEditorProviderServiceImpl : CangJieFacetEditorProviderService {
    override fun getEditorTabs(
        configuration: CangJieFacetConfiguration,
        editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager,
    ): List<FacetEditorTab> {
        val tabs = ArrayList<FacetEditorTab>(2)
        tabs += CangJieFacetEditorGeneralTab(configuration, editorContext, validatorsManager)
        if (CangJieFacetCompilerPluginsTab.parsePluginOptions(configuration).isNotEmpty()) {
            tabs += CangJieFacetCompilerPluginsTab(configuration, validatorsManager)
        }
        return tabs
    }

    override fun getMultipleConfigurationEditor(
        project: com.intellij.openapi.project.Project,
        editors: Array<out FacetEditor>,
    ): MultipleFacetSettingsEditor = MultipleCangJieFacetEditor(project, editors)
}
