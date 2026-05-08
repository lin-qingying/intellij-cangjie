package org.cangnova.cangjie.facet

import com.intellij.facet.FacetType
import com.intellij.facet.FacetTypeId
import com.intellij.facet.FacetTypeRegistry
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.util.NlsSafe
import com.intellij.icons.AllIcons
import javax.swing.Icon

/**
 * 仓颉 facet 类型。
 *
 * 对位 Kotlin `KotlinFacetType`。
 */
abstract class CangJieFacetType<C : CangJieFacetConfiguration> :
    FacetType<CangJieFacet, C>(TYPE_ID, ID, NAME) {
    companion object {
        const val ID = "cangjie-language"
        val TYPE_ID = FacetTypeId<CangJieFacet>(ID)

        @NlsSafe
        const val NAME = "CangJie"

        val INSTANCE: CangJieFacetType<*>
            get() = FacetTypeRegistry.getInstance().findFacetType(TYPE_ID) as CangJieFacetType<*>
    }

    override fun isSuitableModuleType(moduleType: ModuleType<*>): Boolean = true

    override fun getIcon(): Icon = AllIcons.FileTypes.Text
}
