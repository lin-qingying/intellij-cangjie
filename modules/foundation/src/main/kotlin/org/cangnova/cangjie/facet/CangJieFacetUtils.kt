@file:JvmName("CangJieFacetUtils")

package org.cangnova.cangjie.facet

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModel

/**
 * 仓颉 facet 顶层工具入口。
 *
 * 对位 Kotlin `org.jetbrains.kotlin.idea.facet.KotlinFacetUtils.kt` 的文件位置。
 * 仓颉没有 Kotlin target-platform 对应的推断和参数复制体系，
 * 因此这里只保留 facet 初始化入口，避免调用方各自发明包装层。
 */
@Suppress("UNUSED_PARAMETER")
fun CangJieFacetSettings.initializeIfNeeded(
    module: Module,
    rootModel: ModuleRootModel?,
    compilerVersion: String? = null,
) {
    this.initializeIfNeeded(module, rootModel)
}
