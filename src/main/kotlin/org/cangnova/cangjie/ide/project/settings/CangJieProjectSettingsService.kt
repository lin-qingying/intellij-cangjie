/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.ide.project.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Transient
import org.cangnova.cangjie.configurable.CangJieConfigurable
import org.cangnova.cangjie.toolchain.CjToolchainBase
import org.cangnova.cangjie.toolchain.CjToolchainProvider
import org.cangnova.cangjie.toolchain.ExternalLinter
import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import org.cangnova.cangjie.utils.isUnitTestMode
import org.cangnova.cangjie.utils.showSettingsDialog

private const val SERVICE_NAME: String = "CangJieProjectSettings"
val Project.toolchain: CjToolchainBase?
    get() {
        val toolchain = cangjieSettings.state.toolchain
        return when {
            toolchain != null -> toolchain
            isUnitTestMode -> CjToolchainBase.suggest()
            else -> null
        }
    }

@State(
    name = SERVICE_NAME, storages = [
        Storage(StoragePathMacros.WORKSPACE_FILE),
        Storage("misc.xml", deprecated = true)
    ]
)
@Service(Service.Level.PROJECT)
class CangJieProjectSettingsService(
    project: Project,

    ) : CjProjectSettingsServiceBase<CangJieProjectSettingsService.CangJieProjectSettings>(
    project,
    CangJieProjectSettings()
) {

    val toolchain: CjToolchainBase? get() = state.toolchain
    val useOffline: Boolean get() = state.useOffline
    val compileAllTargets: Boolean get() = state.compileAllTargets


    val autoUpdateEnabled: Boolean get() = state.autoUpdateEnabled

    class CangJieProjectSettings : CjProjectSettingsBase<CangJieProjectSettings>() {

        private val sdkRegistry = CjSdkRegistry.getInstance()

        @AffectsHighlighting
        var compileAllTargets by property(true)

        @AffectsCjpmMetadata
        var sdkId by string()
        var externalLinter by enum(ExternalLinter.DEFAULT)

        var externalLinterArguments by property("") { it.isEmpty() }
        var autoUpdateEnabled by property(true)

        var runExternalLinterOnTheFly by property(false)
        override fun copy(): CangJieProjectSettings {
            val state = CangJieProjectSettings()
            state.copyFrom(this)
            return state
        }

        var useOffline by property(false)

        @get:Transient
        @set:Transient
        var toolchain: CjToolchainBase?
            get() = sdkId?.let { id ->
                sdkRegistry.getSdk(id)?.homePath?.let { CjToolchainProvider.getToolchain(it) }
            }
            set(value) {
                val path = value?.location
                if (path != null) {
                    // 注册SDK并保存ID
                    val sdk = sdkRegistry.getSdkByPath(path) ?: sdkRegistry.registerSdkPath(path)
                    sdkId = sdk?.id
                } else {
                    sdkId = null
                }
            }

        @get:Transient
        @set:Transient
        var sdk: CjSdk?
            get() = sdkId?.let { sdkRegistry.getSdk(it) }
            set(value) {
                sdkId = value?.id
            }

    }

    class SettingsChangedEvent(
        oldState: CangJieProjectSettings,
        newState: CangJieProjectSettings
    ) : SettingsChangedEventBase<CangJieProjectSettings>(oldState, newState)

    override fun createSettingsChangedEvent(
        oldEvent: CangJieProjectSettings,
        newEvent: CangJieProjectSettings
    ): SettingsChangedEvent = SettingsChangedEvent(oldEvent, newEvent)

    fun configureToolchain() {
        project.showSettingsDialog<CangJieConfigurable>()
    }

}


val Project.cangjieSettings: CangJieProjectSettingsService
    get() = service<CangJieProjectSettingsService>()


