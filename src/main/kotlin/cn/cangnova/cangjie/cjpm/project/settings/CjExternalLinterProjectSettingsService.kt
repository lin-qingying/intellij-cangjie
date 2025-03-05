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

package cn.cangnova.cangjie.cjpm.project.settings

import cn.cangnova.cangjie.cjpm.toolchain.ExternalLinter
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

val Project.externalLinterSettings: CjExternalLinterProjectSettingsService
    get() = service<CjExternalLinterProjectSettingsService>()
private const val SERVICE_NAME: String = "CjExternalLinterProjectSettings"

@State(name = SERVICE_NAME, storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class CjExternalLinterProjectSettingsService(
    project: Project
) : CjProjectSettingsServiceBase<CjExternalLinterProjectSettingsService.CjExternalLinterProjectSettings>(
    project,
    CjExternalLinterProjectSettings()
) {
    val tool: ExternalLinter get() = state.tool

    val runOnTheFly: Boolean get() = state.runOnTheFly
    val additionalArguments: String get() = state.additionalArguments

    val envs: Map<String, String> get() = state.envs
    override fun noStateLoaded() {
        val cangjieSettings = project.cangjieSettings
        state.tool = cangjieSettings.state.externalLinter
        cangjieSettings.state.externalLinter = ExternalLinter.DEFAULT
        state.additionalArguments = cangjieSettings.state.externalLinterArguments
        cangjieSettings.state.externalLinterArguments = ""
        state.runOnTheFly = cangjieSettings.state.runExternalLinterOnTheFly
        cangjieSettings.state.runExternalLinterOnTheFly = false
    }

    class CjExternalLinterProjectSettings : CjProjectSettingsBase<CjExternalLinterProjectSettings>() {


        @AffectsHighlighting
        var tool by enum(ExternalLinter.DEFAULT)

        @AffectsHighlighting
        var additionalArguments by property("") { it.isEmpty() }


        @AffectsHighlighting
        var envs by map<String, String>()

        @AffectsHighlighting
        var runOnTheFly by property(false)

        override fun copy(): CjExternalLinterProjectSettings {
            val state = CjExternalLinterProjectSettings()
            state.copyFrom(this)
            return state
        }
    }

    class SettingsChangedEvent(
        oldState: CjExternalLinterProjectSettings,
        newState: CjExternalLinterProjectSettings
    ) : SettingsChangedEventBase<CjExternalLinterProjectSettings>(oldState, newState)

    override fun createSettingsChangedEvent(
        oldEvent: CjExternalLinterProjectSettings,
        newEvent: CjExternalLinterProjectSettings
    ): SettingsChangedEventBase<CjExternalLinterProjectSettings> = SettingsChangedEvent(oldEvent, newEvent)
}
