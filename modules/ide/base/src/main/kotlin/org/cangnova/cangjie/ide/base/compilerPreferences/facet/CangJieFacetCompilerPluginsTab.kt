package org.cangnova.cangjie.ide.base.compilerPreferences.facet

import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import org.cangnova.cangjie.facet.CangJieFacetConfiguration
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 仓颉 facet 编译器插件页。
 *
 * 对位 Kotlin `KotlinFacetCompilerPluginsTab`。
 * 仓颉当前 facet 设置层还没有 Kotlin `pluginOptions` 那样的持久化入口，
 * 因而这里保留声明与选择逻辑，但当前不会暴露实际插件项。
 */
class CangJieFacetCompilerPluginsTab(
    private val configuration: CangJieFacetConfiguration,
    private val validatorsManager: FacetValidatorsManager,
) : FacetEditorTab() {
    companion object {
        fun parsePluginOptions(configuration: CangJieFacetConfiguration): List<String> = emptyList()
    }

    override fun getDisplayName(): String = "Compiler Plugins"

    override fun createComponent(): JComponent = JPanel()

    override fun isModified(): Boolean = false

    override fun reset() {
    }

    override fun apply() {
    }

    override fun disposeUIResources() {
    }
}
