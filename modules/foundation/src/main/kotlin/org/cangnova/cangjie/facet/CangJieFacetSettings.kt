package org.cangnova.cangjie.facet

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModel

/**
 * 仓颉 facet 设置。
 *
 * 对位 Kotlin `KotlinFacetSettings` 的承载位。
 * 仓颉当前没有 Kotlin 那套 target-platform / MPP / module-kind 配置面，
 * 因此这里只保留插件层确实需要的公共开关位，禁止伪造多平台语义。
 */
class CangJieFacetSettings {
    /**
     * 与 Kotlin facet 一样保留“是否复用项目级设置”的显式开关。
     *
     * 这不是多平台语义，只是编辑器层配置归属位。
     */
    var useProjectSettings: Boolean = true

    /**
     * 对位 Kotlin `initializeIfNeeded(...)`。
     *
     * 仓颉当前没有依赖 facet 的延迟初始化载荷，因此这里显式保持空实现。
     */
    fun initializeIfNeeded(
        module: Module,
        rootModel: ModuleRootModel?,
    ) {
    }
}
