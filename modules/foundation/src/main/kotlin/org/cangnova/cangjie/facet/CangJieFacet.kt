package org.cangnova.cangjie.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetManager
import com.intellij.openapi.module.Module

/**
 * 仓颉 facet 声明位。
 *
 * 对位 Kotlin `KotlinFacet`。
 * 仓颉 IDE 目前不依赖 facet 承载核心语言语义，但插件层仍需要保留同层声明，
 * 防止 project-structure / 配置 / IDE 扩展各自发明替代入口。
 */
open class CangJieFacet(
    module: Module,
    name: String,
    configuration: CangJieFacetConfiguration,
) : Facet<CangJieFacetConfiguration>(CangJieFacetType.INSTANCE, module, name, configuration, null) {
    companion object {
        fun get(module: Module): CangJieFacet? {
            if (module.isDisposed) return null
            return FacetManager.getInstance(module).getFacetByType(CangJieFacetType.TYPE_ID)
        }
    }
}
