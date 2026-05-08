package org.cangnova.cangjie.facet

/**
 * 仓颉 facet 设置提供器默认实现。
 *
 * Kotlin 这里会接 workspace model / facet 实体缓存。
 * 仓颉当前没有独立 facet 实体驱动主语义，因此这里显式保留统一入口，
 * 并把“无 facet 挂载”的情况规范化为默认设置对象，而不是让调用方分散特判。
 */
class CangJieFacetSettingsProviderImpl : CangJieFacetSettingsProvider {
    override fun getSettings(module: com.intellij.openapi.module.Module): CangJieFacetSettings? {
        return CangJieFacet.get(module)?.configuration?.settings
    }

    override fun getInitializedSettings(module: com.intellij.openapi.module.Module): CangJieFacetSettings {
        val settings = getSettings(module) ?: CangJieFacetSettings()
        settings.initializeIfNeeded(module, null)
        return settings
    }
}
