package com.huawei.cangjie.configurable.state


import com.huawei.cangjie.configurable.LanguageOption
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "PluginLanguageState", storages = [Storage("PluginLanguageState.xml")])
class PluginLanguageState : PersistentStateComponent<PluginLanguageState> {
    var language: LanguageOption = LanguageOption.CHINESE

    override fun getState(): PluginLanguageState {
        return this
    }

    override fun loadState(state: PluginLanguageState) {
        this.language = state.language
    }

    companion object {
        val instance: PluginLanguageState
            get() =  ApplicationManager.getApplication()
                .getService(PluginLanguageState::class.java)
    }
}
