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

import cn.cangnova.cangjie.configurable.CangJieConfigurable
import cn.cangnova.cangjie.cjpm.toolchain.CjToolchainBase
import cn.cangnova.cangjie.cjpm.toolchain.CjToolchainProvider
import cn.cangnova.cangjie.cjpm.toolchain.CjToolchainServices
import cn.cangnova.cangjie.cjpm.toolchain.ExternalLinter
import cn.cangnova.cangjie.ide.run.cjpm.isUnitTestMode
import cn.cangnova.cangjie.utils.showSettingsDialog
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.io.systemIndependentPath
import com.intellij.util.xmlb.annotations.Transient
import java.nio.file.Paths

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

        private val toolchainsService = ApplicationManager.getApplication().getService(CjToolchainServices::class.java)

        @AffectsHighlighting
        var compileAllTargets by property(true)

        @AffectsCjpmMetadata
        var toolchainHomeDirectory by string()
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
            get() = toolchainHomeDirectory?.let { CjToolchainProvider.getToolchain(Paths.get(it)) }
            set(value) {

                toolchainHomeDirectory = value?.location?.systemIndependentPath

                toolchainHomeDirectory?.let { toolchainsService.putToolchainPath(it) }
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


