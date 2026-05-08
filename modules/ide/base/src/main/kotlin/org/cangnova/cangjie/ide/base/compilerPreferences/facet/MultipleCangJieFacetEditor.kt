package org.cangnova.cangjie.ide.base.compilerPreferences.facet

import com.intellij.facet.ui.FacetEditor
import com.intellij.facet.ui.MultipleFacetSettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

/**
 * 仓颉 facet 批量编辑器。
 *
 * 对位 Kotlin `MultipleKotlinFacetEditor`。
 * 仓颉当前只批量承载 `useProjectSettings` 与只读版本信息，
 * 不伪造 Kotlin 那种按平台拆分的大量批量绑定控件。
 */
class MultipleCangJieFacetEditor(
    private val project: Project,
    private val editors: Array<out FacetEditor>,
) : MultipleFacetSettingsEditor() {
    private val FacetEditor.tabEditor: CangJieFacetEditorGeneralTab.EditorComponent
        get() = editorTabs.filterIsInstance<CangJieFacetEditorGeneralTab>().first().editor

    private val multiEditorComponent: Lazy<CangJieFacetEditorGeneralTab.EditorComponent> = lazy(LazyThreadSafetyMode.NONE) {
        val firstEditor = editors.first()

        CangJieFacetEditorGeneralTab.EditorComponent(
            module = firstEditor.tabEditor.module,
            configuration = null,
        ).apply {
            initialize()
            useProjectSettingsCheckBox.isEnabled = false
            useProjectSettingsCheckBox.isSelected = editors.all { it.tabEditor.useProjectSettingsCheckBox.isSelected }
            compilerVersionLabel.text = firstEditor.tabEditor.compilerVersionLabel.text
            runtimeLibraryVersionLabel.text = firstEditor.tabEditor.runtimeLibraryVersionLabel.text
        }
    }

    override fun createComponent(): JComponent = multiEditorComponent.value

    override fun disposeUIResources() {
        if (multiEditorComponent.isInitialized()) {
            Disposer.dispose(multiEditorComponent.value)
        }
    }
}
