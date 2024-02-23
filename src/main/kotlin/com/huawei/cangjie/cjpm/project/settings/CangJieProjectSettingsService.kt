package com.huawei.cangjie.cjpm.project.settings

import com.huawei.cangjie.cjpm.project.configurable.CjProjectConfigurable
import com.huawei.cangjie.cjpm.toolchain.CjToolchainBase
import com.huawei.cangjie.cjpm.toolchain.CjToolchainProvider
import com.huawei.cangjie.cjpm.toolchain.ExternalLinter
import com.huawei.cangjie.utils.showSettingsDialog
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.io.systemIndependentPath
import com.intellij.util.xmlb.annotations.Transient
import java.nio.file.Paths

private const val SERVICE_NAME: String = "CangJieProjectSettings"

@State(name = SERVICE_NAME, storages = [
    Storage(StoragePathMacros.WORKSPACE_FILE),
    Storage("misc.xml", deprecated = true)
])
class CangJieProjectSettingsService(
    project: Project,

) : CjProjectSettingsServiceBase<CangJieProjectSettingsService.CangJieProjectSettings>(project, CangJieProjectSettings()){

    val toolchain: CjToolchainBase? get() = state.toolchain
    val useOffline: Boolean get() = state.useOffline
    val compileAllTargets: Boolean get() = state.compileAllTargets


    val autoUpdateEnabled: Boolean get() = state.autoUpdateEnabled
    class CangJieProjectSettings  : CjProjectSettingsBase<CangJieProjectSettings>() {
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
                toolchainHomeDirectory = value?.sdkHome?.systemIndependentPath
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
        project.showSettingsDialog<CjProjectConfigurable>()
    }

}


val Project.cangjieSettings: CangJieProjectSettingsService
    get() = service<CangJieProjectSettingsService>()
