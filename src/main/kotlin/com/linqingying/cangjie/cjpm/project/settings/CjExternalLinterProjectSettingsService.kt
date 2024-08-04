package com.linqingying.cangjie.cjpm.project.settings

import com.linqingying.cangjie.cjpm.toolchain.ExternalLinter
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
