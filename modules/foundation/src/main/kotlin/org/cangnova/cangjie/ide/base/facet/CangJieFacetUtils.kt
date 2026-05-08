@file:JvmName("CangJieFacetUtils")

package org.cangnova.cangjie.ide.base.facet

import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.facet.CangJieFacet
import org.cangnova.cangjie.name.Name
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

/**
 * 仓颉 IDE 基础模块扩展。
 *
 * 对位 Kotlin 插件 `base/facet/KotlinFacetUtils.kt` 的声明位置：
 * project-structure 依赖的 module 级扩展统一放在插件基础层，而不是散落在 analysis / ide base。
 */

/**
 * 对位 Kotlin `Module.hasKotlinFacet()`。
 *
 * 仓颉 IDE 默认不依赖 facet 承载核心语义；源码/测试/root-model 语义直接落在 workspace model 与源根类型上。
 * 但如果显式挂了 facet，插件层仍应能恢复该声明，而不是永远返回 false。
 */
fun Module.hasCangJieFacet(): Boolean = CangJieFacet.get(this) != null

/**
 * 对位 Kotlin `Module.externalProjectId`。
 *
 * 当前仓颉 IDE 没有单独的 facet/project-link 元数据层，
 * 因此这里显式返回空字符串。
 */
val Module.externalProjectId: String
    get() = ""

/**
 * 对位 Kotlin `Module.kotlinSourceRootType`。
 *
 * Kotlin 这里用 facet 上的 `isTestModule` 决定“整个模块”的 source-root type。
 * 仓颉没有这种模块级 source/test facet 语义，生产/测试只在 source root 级别区分，
 * 因此这里显式返回 null。
 */
val Module.cangjieSourceRootType: JpsModuleSourceRootType<*>?
    get() = null

/**
 * 对位 Kotlin `Module.isMultiPlatformModule`。
 *
 * 仓颉没有 Kotlin multiplatform module 概念。
 */
val Module.isMultiPlatformModule: Boolean
    get() = false

/**
 * 对位 Kotlin `Module.isNewMultiPlatformModule`。
 */
val Module.isNewMultiPlatformModule: Boolean
    get() = false

/**
 * 对位 Kotlin `Module.isHMPPEnabled`。
 *
 * 仓颉没有 HMPP 语义，必须显式返回 false。
 */
val Module.isHMPPEnabled: Boolean
    get() = false

/**
 * 对位 Kotlin `Module.isTestModule`。
 *
 * 仓颉 IDE 的生产/测试区分落在 source-root / `CaSourceModuleKind`，
 * 不存在 Kotlin facet 那种“整个 Module 是 test module”的概念。
 */
val Module.isTestModule: Boolean
    get() = false

/**
 * 对位 Kotlin `Module.isKpmModule`。
 *
 * 仅保留声明位，避免调用方因为缺少属性而自行发明判断逻辑。
 */
var Module.isKpmModule: Boolean
    get() = getUserData(IS_KPM_MODULE_KEY) ?: false
    set(value) {
        putUserData(IS_KPM_MODULE_KEY, value)
    }

/**
 * 对位 Kotlin `Module.refinesFragmentIds`。
 *
 * 仓颉当前没有 fragment refine 体系，因此默认为空；
 * 若将来平台层需要记录额外调试标记，也统一挂在同一声明上。
 */
var Module.refinesFragmentIds: Collection<String>
    get() = getUserData(REFINES_FRAGMENT_IDS_KEY) ?: emptyList()
    set(value) {
        putUserData(REFINES_FRAGMENT_IDS_KEY, value.toList())
    }

/**
 * 仓颉当前没有 Kotlin MPP 的 `additionalVisibleModules` 语义。
 *
 * 这里保留与 Kotlin 同名的显式声明，避免调用方靠“缺少声明”推断语义，
 * 同时明确表达：仓颉 IDE 模块图中不存在这类额外可见模块。
 */
val Module.additionalVisibleModules: List<Module>
    get() = emptyList()

/**
 * 仓颉当前没有 Kotlin MPP 的 `implementedModules` 语义。
 *
 * `dependsOn/refines` 不应被偷换成 Kotlin multiplatform 实现模块关系，
 * 因此这里显式返回空集合，而不是引入错误的跨模块语义。
 */
val Module.implementedModules: List<Module>
    get() = emptyList()

/**
 * 仓颉当前没有 Kotlin MPP 的 `implementingModules` 反向关系。
 */
val Module.implementingModules: List<Module>
    get() = emptyList()

/**
 * 返回仓颉 IDE 视角下的稳定模块名。
 *
 * 当前工作区模块名已经由 `CjWorkspaceModelSync` 稳定生成，
 * 因此这里直接以 IntelliJ module 名构造稳定名，不额外发明 facet/MPP 命名层。
 */
val Module.stableName: Name
    get() = Name.special("<$name>")

private val IS_KPM_MODULE_KEY = Key.create<Boolean>("org.cangnova.cangjie.ide.base.facet.isKpmModule")
private val REFINES_FRAGMENT_IDS_KEY = Key.create<Collection<String>>("org.cangnova.cangjie.ide.base.facet.refinesFragmentIds")
