package com.huawei.cangjie.debugger.settings

import com.huawei.cangjie.debugger.CjDebuggerBundle
import com.huawei.cangjie.debugger.DebuggerKind
import com.huawei.cangjie.debugger.LLDBRenderers
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SimpleConfigurable
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.xdebugger.settings.XDebuggerSettings






class CjDebuggerSettings: XDebuggerSettings<CjDebuggerSettings>("CangJie")  {

    var lldbRenderers: LLDBRenderers = LLDBRenderers.DEFAULT

    var debuggerKind: DebuggerKind = DebuggerKind.LLDB

    var downloadAutomatically: Boolean = false

    var breakOnPanic: Boolean = true
    var skipStdlibInStepping: Boolean = false

    var decorateMsvcTypeNames: Boolean = true
//    private fun createSteppingConfigurable(): Configurable {
//        return SimpleConfigurable.create(
//            STEPPING_ID,
//            CjDebuggerBundle.message("settings.cangjie.debugger.title"),
//            RsDebuggerSteppingSettingsConfigurableUi::class.java,
//            Companion::getInstance
//        )
//    }


    override fun getState(): CjDebuggerSettings = this

    override fun loadState(p0: CjDebuggerSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        fun getInstance(): CjDebuggerSettings = getInstance(CjDebuggerSettings::class.java)

    }
}