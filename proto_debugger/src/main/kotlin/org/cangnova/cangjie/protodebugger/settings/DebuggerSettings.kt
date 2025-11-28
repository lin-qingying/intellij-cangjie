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
import lldbprotobuf.Model.DynamicValueType
import org.cangnova.cangjie.messages.DebuggerBundle
import java.util.*

/**
 * 调试器启动模式
 *
 * 定义调试器的启动方式，不同模式对终端行为和缓冲有不同影响：
 *
 * ### LAUNCH 模式（启动模式）
 * - 调试器直接启动目标程序
 * - 在 Windows 上：完全缓冲（程序退出后才能看到输出）
 * - 在 Linux/macOS 上：行缓冲（实时输出，由 LLDB 的 PTY 支持）
 *
 * ### ATTACH 模式（附加模式）
 * - 先启动程序，然后调试器附加上去
 * - 在 Windows 上：
 *   - 必须配合"模拟终端"选项才能实现行缓冲
 *   - 插件会创建 PTY 并注入到进程，实现实时输出
 * - 在 Linux/macOS 上：行缓冲（由 LLDB 自带的 PTY 支持）
 *
 * ### 平台差异说明
 *
 * **Windows 环境：**
 * - LLDB 在 Windows 上不支持 PTY
 * - LAUNCH 模式：输出完全缓冲，无法实时查看
 * - ATTACH 模式 + 模拟终端：插件实现行缓冲，可以实时输出
 *   - 插件会创建命名管道或 ConPTY
 *   - 将标准输出/错误重定向到管道
 *   - 实现类似 PTY 的行缓冲效果
 *
 * **Linux/macOS 环境：**
 * - LLDB 原生支持 PTY
 * - LAUNCH 模式：LLDB 自动创建 PTY，行缓冲输出
 * - ATTACH 模式：同样由 LLDB 管理 PTY，行缓冲输出
 * - 模拟终端选项对非 Windows 系统影响较小
 *
 * ### 使用建议
 *
 * - **Windows 用户需要实时输出**：选择 ATTACH 模式 + 开启"模拟终端"
 * - **Linux/macOS 用户**：任意模式均可，默认即有行缓冲
 * - **调试启动问题**：使用 LAUNCH 模式，方便捕获启动阶段错误
 * - **调试运行中程序**：使用 ATTACH 模式，不影响程序正常运行
 */
enum class LaunchMode {
    /** 启动模式：调试器直接启动程序 */
    LAUNCH,

    /** 附加模式：先启动程序，调试器再附加 */
    ATTACH
}

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
 * - 变量过滤：控制变量视图中显示的变量类型
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

    /**
     * 启动模式配置
     *
     * 控制调试器如何启动目标程序。
     * 详细说明请参考 LaunchMode 枚举的文档。
     */
    private var launchMode = LaunchMode.LAUNCH
    private val launchModeDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var emulateTerminal = false
    private val emulateTerminalDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var disableASLR = false
    private val disableASLRDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    // ==================== 变量过滤设置 ====================

    private var includeArguments = true
    private var includeStatics = true
    private var includeLocals = true
    private var inScopeOnly = true
    private var includeRuntimeSupportValues = true
    private var useDynamic = DynamicValueType.DYNAMIC_VALUE_NONE
    private var includeRecognizedArguments = true

    private val variableFilterSettingsDispatcher: EventDispatcher<SettingListener> =
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

    // ==================== 启动模式配置 ====================

    /**
     * 获取启动模式
     *
     * @return 当前配置的启动模式（LAUNCH 或 ATTACH）
     */
    @OptionTag("LAUNCH_MODE")
    fun getLaunchMode(): LaunchMode {
        return launchMode
    }

    /**
     * 设置启动模式
     *
     * @param value 新的启动模式
     */
    fun setLaunchMode(value: LaunchMode) {
        if (launchMode != value) {
            launchMode = value
            fireLaunchModeChanged()
        }
    }

    private fun fireLaunchModeChanged() {
        launchModeDispatcher.getMulticaster().settingChanged()
    }

    fun addLaunchModeListener(listener: SettingListener, disposable: Disposable) {
        launchModeDispatcher.addListener(listener, disposable)
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

    // ==================== 变量过滤设置 ====================

    @OptionTag("INCLUDE_ARGUMENTS")
    fun isIncludeArguments(): Boolean = includeArguments

    fun setIncludeArguments(value: Boolean) {
        if (includeArguments != value) {
            includeArguments = value
            fireVariableFilterSettingsChanged()
        }
    }

    @OptionTag("INCLUDE_STATICS")
    fun isIncludeStatics(): Boolean = includeStatics

    fun setIncludeStatics(value: Boolean) {
        if (includeStatics != value) {
            includeStatics = value
            fireVariableFilterSettingsChanged()
        }
    }

    @OptionTag("INCLUDE_LOCALS")
    fun isIncludeLocals(): Boolean = includeLocals

    fun setIncludeLocals(value: Boolean) {
        if (includeLocals != value) {
            includeLocals = value
            fireVariableFilterSettingsChanged()
        }
    }

    @OptionTag("IN_SCOPE_ONLY")
    fun isInScopeOnly(): Boolean = inScopeOnly

    fun setInScopeOnly(value: Boolean) {
        if (inScopeOnly != value) {
            inScopeOnly = value
            fireVariableFilterSettingsChanged()
        }
    }

    @OptionTag("INCLUDE_RUNTIME_SUPPORT_VALUES")
    fun isIncludeRuntimeSupportValues(): Boolean = includeRuntimeSupportValues

    fun setIncludeRuntimeSupportValues(value: Boolean) {
        if (includeRuntimeSupportValues != value) {
            includeRuntimeSupportValues = value
            fireVariableFilterSettingsChanged()
        }
    }

    @OptionTag("USE_DYNAMIC")
    fun getUseDynamic(): DynamicValueType = useDynamic

    fun setUseDynamic(value: DynamicValueType) {
        if (useDynamic != value) {
            useDynamic = value
            fireVariableFilterSettingsChanged()
        }
    }

    @OptionTag("INCLUDE_RECOGNIZED_ARGUMENTS")
    fun isIncludeRecognizedArguments(): Boolean = includeRecognizedArguments

    fun setIncludeRecognizedArguments(value: Boolean) {
        if (includeRecognizedArguments != value) {
            includeRecognizedArguments = value
            fireVariableFilterSettingsChanged()
        }
    }

    private fun fireVariableFilterSettingsChanged() {
        variableFilterSettingsDispatcher.getMulticaster().settingChanged()
    }

    fun addVariableFilterSettingsListener(listener: SettingListener, disposable: Disposable) {
        variableFilterSettingsDispatcher.addListener(listener, disposable)
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

        group(DebuggerBundle.message("debug.settings.variables.separator")) {
            row {
                checkBox(DebuggerBundle.message("debug.settings.includeArguments.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isIncludeArguments()
                        override fun set(value: Boolean) {
                            settings.setIncludeArguments(value)
                        }
                    }
                )
            }
            row {
                checkBox(DebuggerBundle.message("debug.settings.includeLocals.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isIncludeLocals()
                        override fun set(value: Boolean) {
                            settings.setIncludeLocals(value)
                        }
                    }
                )
            }
            row {
                checkBox(DebuggerBundle.message("debug.settings.includeStatics.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isIncludeStatics()
                        override fun set(value: Boolean) {
                            settings.setIncludeStatics(value)
                        }
                    }
                )
            }
            row {
                checkBox(DebuggerBundle.message("debug.settings.inScopeOnly.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isInScopeOnly()
                        override fun set(value: Boolean) {
                            settings.setInScopeOnly(value)
                        }
                    }
                )
            }
            row {
                checkBox(DebuggerBundle.message("debug.settings.includeRuntimeSupportValues.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isIncludeRuntimeSupportValues()
                        override fun set(value: Boolean) {
                            settings.setIncludeRuntimeSupportValues(value)
                        }
                    }
                ).comment(DebuggerBundle.message("debug.settings.includeRuntimeSupportValues.checkbox.hint"))
            }
            row {
                checkBox(DebuggerBundle.message("debug.settings.includeRecognizedArguments.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isIncludeRecognizedArguments()
                        override fun set(value: Boolean) {
                            settings.setIncludeRecognizedArguments(value)
                        }
                    }
                ).comment(DebuggerBundle.message("debug.settings.includeRecognizedArguments.checkbox.hint"))
            }
        }

        group(DebuggerBundle.message("debug.settings.behavior.separator")) {
//            row {
//                label(DebuggerBundle.message("debug.settings.launchMode.label"))
//                    .bold()
//            }
//            buttonsGroup {
//                row {
//                    radioButton(DebuggerBundle.message("debug.settings.launchMode.launch.text"), LaunchMode.LAUNCH)
//                        .comment(DebuggerBundle.message("debug.settings.launchMode.launch.description"))
//                }
//                row {
//                    radioButton(DebuggerBundle.message("debug.settings.launchMode.attach.text"), LaunchMode.ATTACH)
//                        .comment(DebuggerBundle.message("debug.settings.launchMode.attach.description"))
//                }
//            }.bind(
//                object : MutableProperty<LaunchMode> {
//                    override fun get(): LaunchMode = settings.getLaunchMode()
//                    override fun set(value: LaunchMode) {
//                        settings.setLaunchMode(value)
//                    }
//                }
//            )

//            separator()

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
//                comment(DebuggerBundle.message("debug.settings.platform.hint"))
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