package org.cangnova.cangjie.ide.base.compilerPreferences.facet

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import org.cangnova.cangjie.facet.CangJieVersionInfoProvider
import org.cangnova.cangjie.toolchain.api.CjProjectSdkConfig

/**
 * 基于模块依赖的仓颉版本信息提供器。
 *
 * 对位 Kotlin `KotlinVersionInfoProviderByModuleDependencies`。
 * 仓颉没有多平台运行时库矩阵，IDE 中的标准库/编译器版本统一以项目工具链为准。
 */
class CangJieVersionInfoProviderByModuleDependencies : CangJieVersionInfoProvider {
    override fun getCompilerVersion(module: Module): String? {
        return CjProjectSdkConfig.getInstance(module.project).getProjectSdk()?.version?.toString()
            ?: getCompilerVersion()
    }

    override fun getCompilerVersion(): String? {
        return PluginManagerCore.getPlugin(PluginId.getId("cn.cangnova.cangjie"))?.version
    }

    override fun getLibraryVersionsSequence(
        module: Module,
        rootModel: ModuleRootModel?,
    ): Sequence<String> {
        if (module.isDisposed) {
            return emptySequence()
        }

        val sdkVersion = CjProjectSdkConfig.getInstance(module.project).getProjectSdk()?.version?.toString()
        val orderEntries = (rootModel ?: ModuleRootManager.getInstance(module)).orderEntries

        return sequence {
            if (sdkVersion != null && orderEntries.isNotEmpty()) {
                yield(sdkVersion)
            }
        }
    }
}
