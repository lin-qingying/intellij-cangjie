package com.linqingying.cangjie.ide.run.cjpm.runconfig

import com.intellij.execution.target.LanguageRuntimeConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent

class CjLanguageRuntimeConfiguration: LanguageRuntimeConfiguration(CjLanguageRuntimeType.TYPE_ID),
    PersistentStateComponent<CjLanguageRuntimeConfiguration.MyState> {
    var cjcPath: String = ""
    var cjcVersion: String = ""

    var cjpmPath: String = ""
    var cjpmVersion: String = ""

    var localBuildArgs: String = ""
    class MyState : BaseState() {
        var cjcPath by string()
        var cjcVersion by string()

        var cjpmPath by string()
        var cjpmVersion by string()

        var localBuildArgs by string()
    }

    override fun getState(): MyState = MyState().also {
        it.cjcPath = this.cjcPath
        it.cjcVersion = this.cjcVersion

        it.cjpmPath = this.cjpmPath
        it.cjpmVersion = this.cjpmVersion

        it.localBuildArgs = this.localBuildArgs
    }

    override fun loadState(state: MyState) {
        this.cjcPath = state.cjcPath.orEmpty()
        this.cjcVersion = state.cjcVersion.orEmpty()

        this.cjpmPath = state.cjpmPath.orEmpty()
        this.cjpmVersion = state.cjpmVersion.orEmpty()

        this.localBuildArgs = state.localBuildArgs.orEmpty()
    }
}
