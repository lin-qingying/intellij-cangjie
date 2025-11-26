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

package org.cangnova.cangjie.protodebugger.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.DslConfigurableBase
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.intellij.util.EventDispatcher
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreeState
import com.intellij.xdebugger.settings.DebuggerSettingsCategory
import com.intellij.xdebugger.settings.XDebuggerSettings
import org.cangnova.cangjie.messages.DebuggerBundle
import java.util.*

/**
 * 调试器设置管理类
 *
 * 该类继承自XDebuggerSettings，用于管理仓颉语言调试器的各种配置选项。
 * 所有设置都会持久化保存，并在IDE重启后恢复。
 *
 * 主要功能：
 * - 显示选项：配置调试器界面中显示的元素
 * - 格式化设置：控制数值的显示格式
 * - 行为配置：控制调试器运行时行为
 *
 * 使用场景：
 * - 调试器个性化：根据用户偏好调整调试器外观和行为
 * - 性能优化：通过开关某些功能来提升调试性能
 * - 兼容性配置：针对不同平台和调试场景进行配置
 */
class DebuggerSettings : XDebuggerSettings<DebuggerSettings>("CangJie") {

    // ==================== 格式化设置 ====================

    private var hexFormattingEnabled = false
    private var hexAsSecondaryFormattingEnabled = true
    private val hexFormattingSettingsDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    // ==================== 栈帧装饰设置 ====================

    private var showFrameModuleName = false
    private val frameDecorationSettingsDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    // ==================== 调试器行为配置 ====================

    private var emulateTerminal = false
    private val emulateTerminalDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var disableASLR = false
    private val disableASLRDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    // ==================== 十六进制格式化 ====================

    @OptionTag("HEX_FORMATTING_ENABLED")
    fun isHexFormattingEnabled(): Boolean {
        return hexFormattingEnabled
    }

    fun setHexFormattingEnabled(value: Boolean) {
        if (hexFormattingEnabled != value) {
            hexFormattingEnabled = value
            fireHexFormattingSettingsChanged()
        }
    }

    @OptionTag("HEX_AS_SECONDARY_FORMATTING_ENABLED")
    fun isHexAsSecondaryFormattingEnabled(): Boolean {
        return hexAsSecondaryFormattingEnabled
    }

    fun setHexAsSecondaryFormattingEnabled(value: Boolean) {
        if (hexAsSecondaryFormattingEnabled != value) {
            hexAsSecondaryFormattingEnabled = value
            fireHexFormattingSettingsChanged()
        }
    }

    private fun fireHexFormattingSettingsChanged() {
        hexFormattingSettingsDispatcher.getMulticaster().settingChanged()
    }

    fun addHexFormattingSettingsListener(listener: SettingListener, disposable: Disposable) {
        hexFormattingSettingsDispatcher.addListener(listener, disposable)
    }

    // ==================== 栈帧装饰 ====================

    @OptionTag("SHOW_FRAME_MODULE_NAME")
    fun isShowFrameModuleName(): Boolean {
        return showFrameModuleName
    }

    fun setShowFrameModuleName(showFrameModuleName: Boolean) {
        if (showFrameModuleName != this.showFrameModuleName) {
            this.showFrameModuleName = showFrameModuleName
            fireFrameDecorationSettingsChanged()
        }
    }

    fun fireFrameDecorationSettingsChanged() {
        frameDecorationSettingsDispatcher.getMulticaster().settingChanged()
    }

    fun addFrameDecorationSettingsListener(listener: SettingListener, disposable: Disposable) {
        frameDecorationSettingsDispatcher.addListener(listener, disposable)
    }

    // ==================== 终端模拟配置 ====================

    @OptionTag("EMULATE_TERMINAL")
    fun isEmulateTerminal(): Boolean {
        return emulateTerminal
    }

    fun setEmulateTerminal(value: Boolean) {
        if (emulateTerminal != value) {
            emulateTerminal = value
            fireEmulateTerminalChanged()
        }
    }

    private fun fireEmulateTerminalChanged() {
        emulateTerminalDispatcher.getMulticaster().settingChanged()
    }

    fun addEmulateTerminalListener(listener: SettingListener, disposable: Disposable) {
        emulateTerminalDispatcher.addListener(listener, disposable)
    }

    // ==================== ASLR 配置 ====================

    @OptionTag("DISABLE_ASLR")
    fun isDisableASLR(): Boolean {
        return disableASLR
    }

    fun setDisableASLR(value: Boolean) {
        if (disableASLR != value) {
            disableASLR = value
            fireDisableASLRChanged()
        }
    }

    private fun fireDisableASLRChanged() {
        disableASLRDispatcher.getMulticaster().settingChanged()
    }

    fun addDisableASLRListener(listener: SettingListener, disposable: Disposable) {
        disableASLRDispatcher.addListener(listener, disposable)
    }

    // ==================== XDebuggerSettings 实现 ====================

    override fun getState(): DebuggerSettings {
        return this
    }

    override fun loadState(state: DebuggerSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun createConfigurables(category: DebuggerSettingsCategory): MutableCollection<out Configurable> {
        return if (category == DebuggerSettingsCategory.DATA_VIEWS) {
            mutableListOf(DebuggerSettingsConfigurable(this))
        } else {
            mutableListOf()
        }
    }

    // ==================== 工具方法 ====================

    fun interface SettingListener : EventListener {
        fun settingChanged()
    }

    companion object {
        fun updateCurrentDebugSession(e: AnActionEvent?) {
            if (DebuggerUIUtil.isInDetachedTree(e)) {
                val tree = XDebuggerTree.getTree(e)
                tree?.rebuildAndRestore(XDebuggerTreeState.saveState(tree))
            }
            val session = DebuggerUIUtil.getSession(e!!)
            session?.rebuildViews()
        }

        fun getInstance(): DebuggerSettings {
            return getInstance(DebuggerSettings::class.java)
        }
    }
}

/**
 * 调试器设置配置面板
 */
class DebuggerSettingsConfigurable(val settings: DebuggerSettings) :
    DslConfigurableBase(), SearchableConfigurable {

    override fun createPanel() = panel {
        group(DebuggerBundle.message("debug.settings.formatting.separator")) {
            lateinit var hex: Cell<JBCheckBox>
            row {
                hex = checkBox(DebuggerBundle.message("debug.settings.enableHexNumberFormatting.checkbox"))
                    .bindSelected(
                        object : MutableProperty<Boolean> {
                            override fun get(): Boolean = settings.isHexFormattingEnabled()
                            override fun set(value: Boolean) {
                                settings.setHexFormattingEnabled(value)
                            }
                        }
                    )
            }

            indent {
                row {
                    checkBox(DebuggerBundle.message("debug.settings.enableHexNumberFormatting.asSecondary.checkbox"))
                        .bindSelected(
                            object : MutableProperty<Boolean> {
                                override fun get(): Boolean = settings.isHexAsSecondaryFormattingEnabled()
                                override fun set(value: Boolean) {
                                    settings.setHexAsSecondaryFormattingEnabled(value)
                                }
                            })
                        .comment(DebuggerBundle.message("debug.settings.enableHexNumberFormatting.asSecondary.checkbox.hint"))
                        .enabledIf(hex.selected)
                }
            }
        }

        group(DebuggerBundle.message("debug.settings.frames.separator")) {
            row {
                checkBox(DebuggerBundle.message("debug.settings.showFrameModule.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isShowFrameModuleName()
                        override fun set(value: Boolean) {
                            settings.setShowFrameModuleName(value)
                        }
                    }
                )
            }
        }

        group(DebuggerBundle.message("debug.settings.behavior.separator")) {
            row {
                checkBox(DebuggerBundle.message("debug.settings.emulateTerminal.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isEmulateTerminal()
                        override fun set(value: Boolean) {
                            settings.setEmulateTerminal(value)
                        }
                    }
                ).comment(DebuggerBundle.message("debug.settings.emulateTerminal.checkbox.description"))
            }
//            row {
//                checkBox(DebuggerBundle.message("debug.settings.disableASLR.checkbox")).bindSelected(
//                    object : MutableProperty<Boolean> {
//                        override fun get(): Boolean = settings.isDisableASLR()
//                        override fun set(value: Boolean) {
//                            settings.setDisableASLR(value)
//                        }
//                    }
//                ).comment(DebuggerBundle.message("debug.settings.disableASLR.checkbox.description"))
//            }
        }
    }

    override fun getDisplayName(): String {
        return DebuggerBundle.message("debug.settings.name.c.cpp")
    }

    override fun getHelpTopic(): String {
        return "reference.idesettings.debugger.cpp"
    }

    override fun getId(): String = "Debugger.ObjectiveC"
}