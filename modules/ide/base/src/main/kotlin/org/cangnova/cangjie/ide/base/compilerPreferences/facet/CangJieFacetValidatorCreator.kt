package org.cangnova.cangjie.ide.base.compilerPreferences.facet

import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorValidator
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.extensions.ExtensionPointName

/**
 * 仓颉 facet 校验器创建器。
 *
 * 对位 Kotlin `KotlinFacetValidatorCreator`。
 * 即使仓颉当前 facet UI 还没有 Kotlin 那样完整的配置面，校验器扩展点的归属位也必须先固定，
 * 防止后续调用方把校验逻辑散落到 editor/provider 外侧。
 */
abstract class CangJieFacetValidatorCreator {
    companion object {
        val EP_NAME: ExtensionPointName<CangJieFacetValidatorCreator> =
            ExtensionPointName.create("org.cangnova.cangjie.facetValidatorCreator")
    }

    abstract fun create(
        editor: CangJieFacetEditorGeneralTab.EditorComponent,
        validatorsManager: FacetValidatorsManager,
        editorContext: FacetEditorContext,
    ): FacetEditorValidator
}
