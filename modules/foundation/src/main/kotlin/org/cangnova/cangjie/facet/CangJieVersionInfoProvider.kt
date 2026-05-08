package org.cangnova.cangjie.facet

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModel

/**
 * 仓颉版本信息提供器。
 *
 * 对位 Kotlin `KotlinVersionInfoProvider`。
 * 仓颉没有 `TargetPlatform` / `IdePlatformKind` 分派面，因此这里显式收敛为单一语言版本通道。
 */
interface CangJieVersionInfoProvider {
    companion object {
        val EP_NAME: ExtensionPointName<CangJieVersionInfoProvider> =
            ExtensionPointName.create("org.cangnova.cangjie.versionInfoProvider")
    }

    fun getCompilerVersion(module: Module): String?

    fun getCompilerVersion(): String?

    fun getLibraryVersionsSequence(
        module: Module,
        rootModel: ModuleRootModel?,
    ): Sequence<String>
}

fun getRuntimeLibraryVersions(
    module: Module,
    rootModel: ModuleRootModel?,
): Sequence<String> =
    CangJieVersionInfoProvider.EP_NAME.extensionList.asSequence()
        .map { provider -> provider.getLibraryVersionsSequence(module, rootModel) }
        .firstOrNull { versions -> versions.any() }
        ?: emptySequence()

fun getLibraryVersion(
    module: Module,
    rootModel: ModuleRootModel?,
): String? = getRuntimeLibraryVersions(module, rootModel).minOrNull()

fun getDefaultVersion(
    explicitVersion: String? = null,
): String? {
    if (explicitVersion != null) {
        return explicitVersion
    }

    return CangJieVersionInfoProvider.EP_NAME.extensionList
        .asSequence()
        .mapNotNull { provider -> provider.getCompilerVersion() }
        .minOrNull()
}

fun getRuntimeLibraryVersion(module: Module): String? {
    val settingsProvider = CangJieFacetSettingsProvider.getInstance(module.project) ?: return null
    settingsProvider.getInitializedSettings(module)
    return getRuntimeLibraryVersions(module, null).toSet().singleOrNull()
}

fun getRuntimeLibraryVersionOrDefault(module: Module): String? =
    getRuntimeLibraryVersion(module) ?: getDefaultVersion()
