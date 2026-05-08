package org.cangnova.cangjie.facet

import org.jdom.Element

/**
 * 仓颉 facet 默认配置实现。
 *
 * 对位 Kotlin `KotlinFacetConfigurationImpl`。
 */
class CangJieFacetConfigurationImpl : CangJieFacetConfiguration {
    override var settings: CangJieFacetSettings = CangJieFacetSettings()
        private set

    @Suppress("OVERRIDE_DEPRECATION")
    override fun readExternal(element: Element) {
        settings = CangJieFacetSettings().apply {
            useProjectSettings = element.getAttributeValue(USE_PROJECT_SETTINGS_ATTRIBUTE)?.toBooleanStrictOrNull() ?: true
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun writeExternal(element: Element) {
        element.setAttribute(USE_PROJECT_SETTINGS_ATTRIBUTE, settings.useProjectSettings.toString())
    }

    private companion object {
        const val USE_PROJECT_SETTINGS_ATTRIBUTE = "useProjectSettings"
    }
}
