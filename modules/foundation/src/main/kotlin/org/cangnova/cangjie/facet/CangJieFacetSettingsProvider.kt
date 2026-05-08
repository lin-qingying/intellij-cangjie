package org.cangnova.cangjie.facet

import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

/**
 * 仓颉 facet 设置提供器。
 *
 * 对位 Kotlin `KotlinFacetSettingsProvider`。
 */
interface CangJieFacetSettingsProvider {
    fun getSettings(module: Module): CangJieFacetSettings?

    fun getInitializedSettings(module: Module): CangJieFacetSettings

    companion object {
        fun getInstance(project: Project): CangJieFacetSettingsProvider? =
            if (project.isDisposed) {
                null
            } else {
                project.service()
            }
    }
}
