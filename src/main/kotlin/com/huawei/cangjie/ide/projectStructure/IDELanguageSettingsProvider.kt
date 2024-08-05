package com.huawei.cangjie.ide.projectStructure

import com.huawei.cangjie.analyzer.CangJieModuleInfo
import com.huawei.cangjie.analyzer.LanguageSettingsProvider
import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.config.LanguageVersionSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project



internal class IDELanguageSettingsProvider : LanguageSettingsProvider {
    override fun getLanguageVersionSettings(moduleInfo: ModuleInfo, project: Project): LanguageVersionSettings {
        return when (moduleInfo) {
            is CangJieModuleInfo -> moduleInfo.module.languageVersionSettings
//            is LanguageSettingsOwner -> moduleInfo.languageVersionSettings
//            is LibraryInfo -> LanguageVersionSettingsProvider.getInstance(project).librarySettings
//            is PlatformModuleInfo -> moduleInfo.platformModule.module.languageVersionSettings
            else -> project.languageVersionSettings
        }
    }

//    override fun getTargetPlatform(moduleInfo: ModuleInfo, project: Project): TargetPlatformVersion {
//        return when (moduleInfo) {
//            is ModuleSourceInfo -> {
//                val jdkPlatform = moduleInfo.module.platform.subplatformsOfType<JdkPlatform>().firstOrNull()
//                jdkPlatform?.targetVersion ?: TargetPlatformVersion.NoVersion
//            }
//            is LanguageSettingsOwner -> moduleInfo.targetPlatformVersion
//            else -> TargetPlatformVersion.NoVersion
//        }
    }
