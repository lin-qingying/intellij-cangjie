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

package org.cangnova.cangjie.debugger.protobuf.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.util.application
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.OptionTag
import lldbprotobuf.Model.DynamicValueType
import org.cangnova.cangjie.configurable.CjConfigurableBase
import org.cangnova.cangjie.debugger.protobuf.messages.ProtoDebuggerBundle

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
 * 调试器设置服务
 *
 * 项目级别的服务，用于管理仓颉语言调试器的各种配置选项。
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
@State(name = "CangJieProtoDebuggerServices", storages = [Storage("cangjie.debugger.protobuf.xml")])
@Service(Service.Level.APP)
class ProtoDebuggerService : PersistentStateComponent<ProtoDebuggerService> {

    // ==================== 格式化设置 ====================

    @OptionTag("HEX_FORMATTING_ENABLED")
    var hexFormattingEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
            }
        }

    @OptionTag("HEX_AS_SECONDARY_FORMATTING_ENABLED")
    var hexAsSecondaryFormattingEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
            }
        }


    // ==================== 栈帧装饰设置 ====================

    @OptionTag("SHOW_FRAME_MODULE_NAME")
    var showFrameModuleName: Boolean = false
        set(value) {
            if (field != value) {
                field = value
            }
        }


    // ==================== 调试器行为配置 ====================

    @OptionTag("LAUNCH_MODE")
    var launchMode: LaunchMode = LaunchMode.LAUNCH
        set(value) {
            if (field != value) {
                field = value
            }
        }


    @OptionTag("EMULATE_TERMINAL")
    var emulateTerminal: Boolean = false
        set(value) {
            if (field != value) {
                field = value
            }
        }


    @OptionTag("DISABLE_ASLR")
    var disableASLR: Boolean = false
        set(value) {
            if (field != value) {
                field = value
            }
        }


    // ==================== 变量过滤设置 ====================

    @OptionTag("INCLUDE_ARGUMENTS")
    var includeArguments: Boolean = true
        set(value) {
            if (field != value) {
                field = value
            }
        }

    @OptionTag("INCLUDE_STATICS")
    var includeStatics: Boolean = true
        set(value) {
            if (field != value) {
                field = value
            }
        }

    @OptionTag("INCLUDE_LOCALS")
    var includeLocals: Boolean = true
        set(value) {
            if (field != value) {
                field = value
            }
        }

    @OptionTag("IN_SCOPE_ONLY")
    var inScopeOnly: Boolean = true
        set(value) {
            if (field != value) {
                field = value
            }
        }

    @OptionTag("INCLUDE_RUNTIME_SUPPORT_VALUES")
    var includeRuntimeSupportValues: Boolean = true
        set(value) {
            if (field != value) {
                field = value
            }
        }

    @OptionTag("USE_DYNAMIC")
    var useDynamic: DynamicValueType = DynamicValueType.DYNAMIC_VALUE_NONE
        set(value) {
            if (field != value) {
                field = value
            }
        }

    @OptionTag("INCLUDE_RECOGNIZED_ARGUMENTS")
    var includeRecognizedArguments: Boolean = true
        set(value) {
            if (field != value) {
                field = value
            }
        }


    // ==================== PersistentStateComponent 实现 ====================

    override fun getState(): ProtoDebuggerService {
        return this
    }

    override fun loadState(state: ProtoDebuggerService) {
        XmlSerializerUtil.copyBean(state, this)
    }


    companion object {
        fun getInstance(): ProtoDebuggerService {
            return application.getService(ProtoDebuggerService::class.java)
        }
    }
}

/**
 * 调试器设置配置面板
 */
class ProtoDebuggerConfigurable(project: Project) :
    CjConfigurableBase(project, ProtoDebuggerBundle.message("proto.debug.title")), SearchableConfigurable {

    private val settings: ProtoDebuggerService = ProtoDebuggerService.getInstance()

    override fun createPanel() = panel {
        group(ProtoDebuggerBundle.message("proto.debug.settings.formatting.separator")) {
            lateinit var hex: Cell<JBCheckBox>
            row {
                hex = checkBox(ProtoDebuggerBundle.message("proto.debug.settings.enableHexNumberFormatting.checkbox"))
                    .bindSelected(settings::hexFormattingEnabled)
            }

            indent {
                row {
                    checkBox(ProtoDebuggerBundle.message("proto.debug.settings.enableHexNumberFormatting.asSecondary.checkbox"))
                        .bindSelected(settings::hexAsSecondaryFormattingEnabled)
                        .comment(ProtoDebuggerBundle.message("proto.debug.settings.enableHexNumberFormatting.asSecondary.checkbox.hint"))
                        .enabledIf(hex.selected)
                }
            }
        }

        group(ProtoDebuggerBundle.message("proto.debug.settings.frames.separator")) {
            row {
                checkBox(ProtoDebuggerBundle.message("proto.debug.settings.showFrameModule.checkbox"))
                    .bindSelected(settings::showFrameModuleName)
            }
        }

        group(ProtoDebuggerBundle.message("proto.debug.settings.variables.separator")) {
            row {
                checkBox(ProtoDebuggerBundle.message("proto.debug.settings.includeArguments.checkbox"))
                    .bindSelected(settings::includeArguments)
            }
            row {
                checkBox(ProtoDebuggerBundle.message("proto.debug.settings.includeLocals.checkbox"))
                    .bindSelected(settings::includeLocals)
            }
            row {
                checkBox(ProtoDebuggerBundle.message("proto.debug.settings.includeStatics.checkbox"))
                    .bindSelected(settings::includeStatics)
            }
            row {
                checkBox(ProtoDebuggerBundle.message("proto.debug.settings.inScopeOnly.checkbox"))
                    .bindSelected(settings::inScopeOnly)
            }
            row {
                checkBox(ProtoDebuggerBundle.message("proto.debug.settings.includeRuntimeSupportValues.checkbox"))
                    .bindSelected(settings::includeRuntimeSupportValues)
                    .comment(ProtoDebuggerBundle.message("proto.debug.settings.includeRuntimeSupportValues.checkbox.hint"))
            }
            row {
                checkBox(ProtoDebuggerBundle.message("proto.debug.settings.includeRecognizedArguments.checkbox"))
                    .bindSelected(settings::includeRecognizedArguments)
                    .comment(ProtoDebuggerBundle.message("proto.debug.settings.includeRecognizedArguments.checkbox.hint"))
            }
        }

        group(ProtoDebuggerBundle.message("proto.debug.settings.behavior.separator")) {
//            row {
//                label(ProtoDebuggerBundle.message("proto.debug.settings.launchMode.label"))
//                    .bold()
//            }
//            buttonsGroup {
//                row {
//                    radioButton(ProtoDebuggerBundle.message("proto.debug.settings.launchMode.launch.text"), LaunchMode.LAUNCH)
//                        .comment(ProtoDebuggerBundle.message("proto.debug.settings.launchMode.launch.description"))
//                }
//                row {
//                    radioButton(ProtoDebuggerBundle.message("proto.debug.settings.launchMode.attach.text"), LaunchMode.ATTACH)
//                        .comment(ProtoDebuggerBundle.message("proto.debug.settings.launchMode.attach.description"))
//                }
//            }.bind(settings::launchMode)

//            separator()

            row {
                checkBox(ProtoDebuggerBundle.message("proto.debug.settings.emulateTerminal.checkbox"))
                    .bindSelected(settings::emulateTerminal)
                    .comment(ProtoDebuggerBundle.message("proto.debug.settings.emulateTerminal.checkbox.description"))
            }

//            row {
//                comment(ProtoDebuggerBundle.message("proto.debug.settings.platform.hint"))
//            }
        }
    }

    override fun getDisplayName(): String {
        return ProtoDebuggerBundle.message("proto.debug.settings.name.c.cpp")
    }

    override fun getHelpTopic(): String {
        return "reference.idesettings.debugger.cpp"
    }

    override fun getId(): String = "Debugger.ObjectiveC"
}