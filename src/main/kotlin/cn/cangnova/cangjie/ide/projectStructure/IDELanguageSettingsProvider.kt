/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.ide.projectStructure

import cn.cangnova.cangjie.analyzer.CangJieModuleInfo
import cn.cangnova.cangjie.analyzer.LanguageSettingsProvider
import cn.cangnova.cangjie.analyzer.ModuleInfo
import cn.cangnova.cangjie.config.LanguageVersionSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project



internal class IDELanguageSettingsProvider : LanguageSettingsProvider {
    override fun getLanguageVersionSettings(moduleInfo: ModuleInfo, project: Project): LanguageVersionSettings {
        return when (moduleInfo) {
            is CangJieModuleInfo -> moduleInfo.module.languageVersionSettings
//            is LanguageSettingsOwner -> moduleInfo.languageVersionSettings
//            is CjpmLibraryInfo -> LanguageVersionSettingsProvider.getInstance(project).librarySettings
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
