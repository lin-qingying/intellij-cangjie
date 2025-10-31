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

package org.cangnova.cangjie.ide.project

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.io.systemIndependentPath
import com.intellij.util.xmlb.annotations.Transient
import org.cangnova.cangjie.configurable.CangJieConfigurable
import org.cangnova.cangjie.project.AffectsHighlighting
import org.cangnova.cangjie.project.AffectsMetadata
import org.cangnova.cangjie.project.CjProjectSettingsBase
import org.cangnova.cangjie.project.CjProjectSettingsServiceBase
import org.cangnova.cangjie.project.SettingsChangedEventBase

import org.cangnova.cangjie.toolchain.api.CjSdk
import org.cangnova.cangjie.toolchain.api.CjSdkRegistry
import org.cangnova.cangjie.utils.showSettingsDialog
import org.cangnova.cangjie.utils.toPath
import org.cangnova.cangjie.project.cangjieSettingsData
import org.cangnova.cangjie.project.CjSettingsListener

private const val SERVICE_NAME: String = "CangJieProjectSettings"


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
)  {

    init {
        // 初始化时同步设置到 CangJieSettingsData
        syncToCangJieSettingsData()
    }

    val toolchain: CjSdk? get() = state.toolchain

    fun configureToolchain() {
        project.showSettingsDialog<CangJieConfigurable>()
    }

    /**
     * 同步当前设置到 CangJieSettingsData
     */
    private fun syncToCangJieSettingsData() {
        project.cangjieSettingsData.updateFromProjectSettings(
            autoUpdateEnabled = state.autoUpdateEnabled,
            useOffline = state.useOffline,
            compileAllTargets = state.compileAllTargets
        )
    }

    override fun notifySettingsChanged(event: SettingsChangedEventBase<CangJieProjectSettings>) {
        super.notifySettingsChanged(event)
        // 设置变更时同步到 CangJieSettingsData
        syncToCangJieSettingsData()
    }
    class CangJieProjectSettings : CjProjectSettingsBase<CangJieProjectSettings>() {


        @AffectsHighlighting
        var compileAllTargets by property(true)

        @AffectsMetadata
        var toolchainHomeDirectory by string()

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
        var toolchain: CjSdk?
            get() = toolchainHomeDirectory?.let { CjSdkRegistry.getInstance().registerSdkPath(it.toPath()) }
            set(value) {

                toolchainHomeDirectory = value?.homePath?.systemIndependentPath

                toolchainHomeDirectory?.let {  CjSdkRegistry.getInstance() .registerSdkPath(it.toPath()) }
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



}
val Project.cangjieSettings:  CangJieProjectSettingsService
    get() = service< CangJieProjectSettingsService>()





