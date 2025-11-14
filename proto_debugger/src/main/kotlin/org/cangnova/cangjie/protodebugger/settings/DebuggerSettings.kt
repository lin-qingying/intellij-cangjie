package org.cangnova.cangjie.protodebugger.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.DslConfigurableBase
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.layout.and
import com.intellij.util.EventDispatcher
import com.intellij.util.PlatformUtils
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.annotations.XMap
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreeState
import com.intellij.xdebugger.settings.DebuggerSettingsCategory
import com.intellij.xdebugger.settings.XDebuggerSettings
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.symbol.NtSymbolSettings
import org.jetbrains.annotations.Nls
import java.util.*

/**
 * 调试器设置管理类
 *
 * 该类继承自XDebuggerSettings，用于管理仓颉语言调试器的各种配置选项。
 * 包括数据视图设置、渲染器配置、显示选项、格式化设置等多个方面的配置。
 * 所有设置都会持久化保存，并在IDE重启后恢复。
 *
 * 主要功能：
 * - 数据渲染器：控制各种类型数据的显示方式
 * - 显示选项：配置调试器界面中显示的元素
 * - 格式化设置：控制数值和类型的显示格式
 * - 寄存器配置：管理不同架构的寄存器显示
 * - 符号设置：配置调试符号的加载和解析
 *
 * 使用场景：
 * - 调试器个性化：根据用户偏好调整调试器外观和行为
 * - 性能优化：通过开关某些功能来提升调试性能
 * - 兼容性配置：针对不同平台和调试场景进行配置
 * - 调试辅助：启用或禁用特定的调试辅助功能
 */
class DebuggerSettings : XDebuggerSettings<DebuggerSettings>("CangJie") {
    private var renderersEnabled = true
    private val renderersEnabledDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var valuesFilterEnabled = true
    private val valuesFilterEnabledDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var showFunctionReturnValue = true
    private val showFunctionReturnValueDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var cocoaRenderersEnabled = true
    private var coreDataRenderersEnabled = true
    private val cocoaRenderersEnabledDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var stlRenderersEnabled = true
    private val stlRenderersEnabledDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var hexFormattingEnabled = false
    private var hexAsSecondaryFormattingEnabled = true
    private val hexFormattingSettingsDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var muteVariables = false
    private val muteVariablesDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var lldbNatvisRenderersEnabled = true
    private var lldbNatvisDiagnosticsLevel: LLDBNatvisDiagnosticsLevel? = LLDBNatvisDiagnosticsLevel.DISABLED
    private val natvisSettingsDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var showFrameModuleName = false
    private var showFrameFunctionParameters = true
    private var showFrameFunctionTemplateArguments = false
    private val frameDecorationSettingsDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var showTypes = true
    private var showTypeTemplateArguments = true
    private val valuePresentationSettingsDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    private var stripCxxAuxiliaryNamespaces = true
    private var sugarizeCxxStlTypes = true
      var ntSymbolSettings: NtSymbolSettings = NtSymbolSettings()
    private var registerSettings: List<DebuggerRegisterSettings>? = null
    private var showRegisters = false
    private val registersSettingsDispatcher: EventDispatcher<SettingListener> =
        EventDispatcher.create(SettingListener::class.java)

    @XCollection(propertyElementName = "register-settings", style = XCollection.Style.v2)
    fun getRegisterSettings(): List<DebuggerRegisterSettings>? {
        return registerSettings
    }

    @OptionTag("MUTE_VARIABLES")
            /**
             * 获取变量静音设置状态
             *
             * 变量静音功能用于在调试过程中抑制变量的自动显示。
             * 当启用时，某些变量可能不会在调试器中自动显示或更新，
             * 这有助于减少调试界面的混乱，提高调试效率。
             *
             * 使用场景：
             * - 在大型项目中减少调试器的信息显示
             * - 避免不重要的变量干扰调试过程
             * - 提高调试器在复杂项目中的性能
             *
             * @return true如果启用了变量静音功能，false表示所有变量都会正常显示
             */
    fun isMuteVariables(): Boolean {
        return muteVariables
    }

    @OptionTag("SHOW_FUNCTION_RETURN_VALUE")
            /**
             * 获取函数返回值显示设置状态
             *
             * 控制在调试器中是否显示函数的返回值。
             * 当启用时，函数执行完成后会在调试器中显示其返回值。
             *
             * 使用场景：
             * - 查看函数调用后的返回结果
             * - 验证函数逻辑的正确性
             * - 调试函数的返回值处理
             *
             * @return true如果启用函数返回值显示，false则不显示函数返回值
             */
    fun isShowFunctionReturnValue(): Boolean {
        return showFunctionReturnValue
    }

    fun fireShowFunctionReturnValueChanged() {
        showFunctionReturnValueDispatcher.getMulticaster().settingChanged()
    }

    @OptionTag("SHOW_REGISTERS")
            /**
             * 获取寄存器显示设置状态
             *
             * 控制在调试器中是否显示CPU寄存器的值。
             * 当启用时，寄存器窗口会显示当前线程的寄存器状态。
             *
             * 使用场景：
             * - 低级调试：查看和修改CPU寄存器
             * - 性能分析：分析寄存器使用情况
             * - 汇编调试：跟踪寄存器变化
             * - 逆向工程：分析程序执行流程
             *
             * @return true如果启用寄存器显示，false则不显示寄存器信息
             */
    fun isShowRegisters(): Boolean {
        return showRegisters
    }

    fun setShowRegisters(value: Boolean) {
        if (showRegisters != value) {
            showRegisters = value
            fireRegistersSettingsChanged()
        }
    }


    fun setShowFunctionReturnValue(showFunctionReturnValueEnabled: Boolean) {
        if (showFunctionReturnValue != showFunctionReturnValueEnabled) {
            showFunctionReturnValue = showFunctionReturnValueEnabled
            this.fireShowFunctionReturnValueChanged()
        }
    }

    fun setMuteVariables(muteVariables: Boolean) {
        if (muteVariables != muteVariables) {
            this.muteVariables = muteVariables
            this.fireMuteVariablesChanged()
        }
    }

    fun fireMuteVariablesChanged() {
        muteVariablesDispatcher.getMulticaster().settingChanged()
    }

    fun setRegisterSettings(registerSetSettings: List<DebuggerRegisterSettings>?) {
        registerSettings = registerSetSettings
    }

    @OptionTag("SHOW_TYPES")
            /**
             * 获取类型显示设置状态
             *
             * 控制在调试器中是否显示变量的类型信息。
             * 当启用时，每个变量旁边都会显示其类型。
             *
             * 使用场景：
             * - 类型检查：确认变量的实际类型
             * - 类型调试：分析类型转换和类型推断
             * - 代码理解：快速了解变量类型
             * - 泛型调试：查看模板参数的具体类型
             *
             * @return true如果启用类型显示，false则不显示类型信息
             */
    fun isShowTypes(): Boolean {
        return showTypes
    }

    fun setShowTypes(showTypes: Boolean) {
        if (showTypes != showTypes) {
            this.showTypes = showTypes
            fireValuePresentationSettingsChanged()
        }
    }

    fun getRegisterSetSettings(archName: String, driverName: String): Map<String, Boolean>? {

        return if (registerSettings == null) {
            null
        } else {
            val arch = registerSettings!!.find { it.architecture == archName && it.driver == driverName }

            arch?.registerSets?.toMap()
        }
    }

    fun setRegisterSetSettings(archName: String, driverName: String, registerSetSelection: Map<String, Boolean>) {

        val current = registerSettings.orEmpty()
        val newSettings = current.filterNot { it.architecture == archName && it.driver == driverName }
            .toMutableList()

        val changedEntry = DebuggerRegisterSettings().apply {

            architecture = archName
            driver = driverName
            registerSets = registerSetSelection.toMutableMap()

        }

        newSettings.add(changedEntry)
        registerSettings = newSettings
        fireRegistersSettingsChanged()
    }

    private fun fireRegistersSettingsChanged() {
        registersSettingsDispatcher.getMulticaster().settingChanged()
    }


    override fun getState(): DebuggerSettings {
        return this

    }

    @OptionTag("LLDB_NATVIS_DIAGNOSTICS_LEVEL")
    fun getLLDBNatvisDiagnosticsLevel(): LLDBNatvisDiagnosticsLevel? {
        return lldbNatvisDiagnosticsLevel
    }

    fun setLLDBNatvisDiagnosticsLevel(value: LLDBNatvisDiagnosticsLevel?) {
        if (lldbNatvisDiagnosticsLevel !== value) {
            lldbNatvisDiagnosticsLevel = value
            fireNatvisSettingsChanged()
        }
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

    @OptionTag("STRIP_CXX_AUXILIARY_NAMESPACES")
    fun isStripCxxAuxiliaryNamespaces(): Boolean {
        return stripCxxAuxiliaryNamespaces
    }

    @OptionTag("SUGARIZE_CXX_STL_TYPES")
    fun isSugarizeCxxStlTypes(): Boolean {
        return sugarizeCxxStlTypes
    }

    fun setSugarizeCxxStlTypes(sugarizeCxxStlTypes: Boolean) {
        if (sugarizeCxxStlTypes != sugarizeCxxStlTypes) {
            this.sugarizeCxxStlTypes = sugarizeCxxStlTypes
            fireFrameDecorationSettingsChanged()
            fireValuePresentationSettingsChanged()
        }
    }

    fun setStripCxxAuxiliaryNamespaces(stripCxxAuxiliaryNamespaces: Boolean) {
        if (stripCxxAuxiliaryNamespaces != stripCxxAuxiliaryNamespaces) {
            this.stripCxxAuxiliaryNamespaces = stripCxxAuxiliaryNamespaces
            fireFrameDecorationSettingsChanged()
            fireValuePresentationSettingsChanged()
        }
    }

    @OptionTag("SHOW_FRAME_FUNCTION_TEMPLATE_ARGUMENTS")
    fun isShowFrameFunctionTemplateArguments(): Boolean {
        return showFrameFunctionTemplateArguments
    }

    fun setShowFrameFunctionTemplateArguments(showFrameFunctionTemplateArguments: Boolean) {
        if (showFrameFunctionTemplateArguments != showFrameFunctionTemplateArguments) {
            this.showFrameFunctionTemplateArguments = showFrameFunctionTemplateArguments
            fireFrameDecorationSettingsChanged()
        }
    }

    @OptionTag("SHOW_FRAME_MODULE_NAME")
    fun isShowFrameModuleName(): Boolean {
        return showFrameModuleName
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

    fun setShowFrameModuleName(showFrameModuleName: Boolean) {
        if (showFrameModuleName != showFrameModuleName) {
            this.showFrameModuleName = showFrameModuleName
            this.fireFrameDecorationSettingsChanged()
        }
    }

    @OptionTag("SHOW_FRAME_FUNCTION_PARAMETERS")
    fun isShowFrameFunctionParameters(): Boolean {
        return showFrameFunctionParameters
    }

    fun setShowFrameFunctionParameters(showFrameFunctionParameters: Boolean) {
        if (showFrameFunctionParameters != showFrameFunctionParameters) {
            this.showFrameFunctionParameters = showFrameFunctionParameters
            fireFrameDecorationSettingsChanged()
        }
    }

    fun fireFrameDecorationSettingsChanged() {
        frameDecorationSettingsDispatcher.getMulticaster().settingChanged()
    }

    @OptionTag("HEX_FORMATTING_ENABLED")
    fun isHexFormattingEnabled(): Boolean {
        return hexFormattingEnabled
    }

    fun setHexFormattingEnabled(value: Boolean) {
        if (hexFormattingEnabled != value) {
            hexFormattingEnabled = value
            this.fireHexFormattingSettingsChanged()
        }
    }

    @OptionTag("SHOW_TYPE_TEMPLATE_ARGUMENTS")
    fun isShowTypeTemplateArguments(): Boolean {
        return showTypeTemplateArguments
    }

    fun fireValuePresentationSettingsChanged() {
        valuePresentationSettingsDispatcher.getMulticaster().settingChanged()
    }

    fun setShowTypeTemplateArguments(showTypeTemplateArguments: Boolean) {
        if (showTypeTemplateArguments != showTypeTemplateArguments) {
            this.showTypeTemplateArguments = showTypeTemplateArguments
            this.fireValuePresentationSettingsChanged()
        }
    }

    private fun fireHexFormattingSettingsChanged() {
        hexFormattingSettingsDispatcher.getMulticaster().settingChanged()
    }

    @OptionTag("STL_RENDERERS_ENABLED")
    fun isStlRenderersEnabled(): Boolean {
        return stlRenderersEnabled
    }

    fun fireStlRenderersEnabledChanged() {
        stlRenderersEnabledDispatcher.getMulticaster().settingChanged()
    }

    @OptionTag("LLDB_NATVIS_RENDERERS_ENABLED")
    fun isLLDBNatvisRenderersEnabled(): Boolean {
        return lldbNatvisRenderersEnabled
    }

    fun fireNatvisSettingsChanged() {
        natvisSettingsDispatcher.getMulticaster().settingChanged()
    }

    @OptionTag("VALUES_FILTER_ENABLED")
    fun isValuesFilterEnabled(): Boolean {
        return valuesFilterEnabled
    }

    fun fireValuesFilterEnabledChanged() {
        valuesFilterEnabledDispatcher.getMulticaster().settingChanged()
    }

    fun setValuesFilterEnabled(valuesFilterEnabled: Boolean) {
        if (valuesFilterEnabled != valuesFilterEnabled) {
            this.valuesFilterEnabled = valuesFilterEnabled
            this.fireValuesFilterEnabledChanged()
        }
    }

    fun setLLDBNatvisRenderersEnabled(LLDBNatvisRenderersEnabled: Boolean) {
        if (lldbNatvisRenderersEnabled != LLDBNatvisRenderersEnabled) {
            lldbNatvisRenderersEnabled = LLDBNatvisRenderersEnabled
            this.fireNatvisSettingsChanged()
        }
    }

    fun setStlRenderersEnabled(stlRenderersEnabled: Boolean) {
        if (stlRenderersEnabled != stlRenderersEnabled) {
           this. stlRenderersEnabled = stlRenderersEnabled
            this.fireStlRenderersEnabledChanged()
        }
    }

    @OptionTag("CORE_DATA_RENDERERS_ENABLED")
    fun isCoreDataRenderersEnabled(): Boolean {
        return coreDataRenderersEnabled
    }

    fun setCoreDataRenderersEnabled(coreDataRenderersEnabled: Boolean) {
        if (coreDataRenderersEnabled != coreDataRenderersEnabled) {
            this.coreDataRenderersEnabled = coreDataRenderersEnabled
            fireCocoaRenderersEnabledChanged()
        }
    }

    @OptionTag("COCOA_RENDERERS_ENABLED")
    fun isCocoaRenderersEnabled(): Boolean {
        return cocoaRenderersEnabled
    }

    fun setCocoaRenderersEnabled(cocoaRenderersEnabled: Boolean) {
        if (cocoaRenderersEnabled != cocoaRenderersEnabled) {
           this. cocoaRenderersEnabled = cocoaRenderersEnabled
            this.fireCocoaRenderersEnabledChanged()
        }
    }

    fun fireCocoaRenderersEnabledChanged() {
        cocoaRenderersEnabledDispatcher.getMulticaster().settingChanged()
    }

    fun addHexFormattingSettingsListener(listener: SettingListener, disposable: Disposable) {
        hexFormattingSettingsDispatcher.addListener(listener, disposable)
    }

    fun addMuteVariablesListener(listener: SettingListener, disposable: Disposable) {
        muteVariablesDispatcher.addListener(listener, disposable)
    }

    fun addValuesFilterEnabledListener(listener: SettingListener, disposable: Disposable) {
        valuesFilterEnabledDispatcher.addListener(listener, disposable)
    }

    fun addShowFunctionReturnValueListener(listener: SettingListener, disposable: Disposable) {
        showFunctionReturnValueDispatcher.addListener(listener, disposable)
    }

    fun addFrameDecorationSettingsListener(listener: SettingListener, disposable: Disposable) {
        frameDecorationSettingsDispatcher.addListener(listener, disposable)
    }

    fun addValuePresentationSettingsListener(listener: SettingListener, disposable: Disposable) {
        valuePresentationSettingsDispatcher.addListener(listener, disposable)
    }

    fun addRegistersSettingsListener(listener: SettingListener, disposable: Disposable) {
        registersSettingsDispatcher.addListener(listener, disposable)
    }

    fun addCocoaRenderersEnabledListener(listener: SettingListener, disposable: Disposable) {
        cocoaRenderersEnabledDispatcher.addListener(listener, disposable)
    }

    @OptionTag("RENDERERS_ENABLED")
    fun isRenderersEnabled(): Boolean {
        return renderersEnabled
    }

    fun setRenderersEnabled(renderersEnabled: Boolean) {
        if (renderersEnabled != renderersEnabled) {
            this.renderersEnabled = renderersEnabled
            this.fireRenderersEnabledChanged()
        }
    }

    private fun fireRenderersEnabledChanged() {
        renderersEnabledDispatcher.getMulticaster().settingChanged()
    }

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


enum class LLDBNatvisDiagnosticsLevel(
    @field:Nls val description: String,
    val level: Int
) {
    DISABLED("DISABLED", 0),

    ERRORS_ONLY("ERRORS_ONLY", 1),

    VERBOSE("VERBOSE", 2);


    override fun toString(): String = description
}

@Tag
class DebuggerRegisterSettings {
    var architecture: String = ""
        @Attribute get

    var driver: String = ""
        @Attribute get

    @field:XMap
    var registerSets: MutableMap<String, Boolean> = mutableMapOf()
}

class DebuggerSettingsConfigurable(val settings: DebuggerSettings) :
    DslConfigurableBase(), SearchableConfigurable {

    override fun createPanel(): DialogPanel = panel {
        group(DebuggerBundle.message("debug.settings.variables.separator")) {
            var renderers: Cell<JBCheckBox>? = null

            rowsRange {
                row {
                    renderers =
                        checkBox(DebuggerBundle.message("debug.settings.enableValueRenderers.checkbox")).bindSelected(
                            object :
                                MutableProperty<Boolean> {
                                override fun get(): Boolean =
                                    settings.isRenderersEnabled()


                                override fun set(value: Boolean) {

                                    settings.setRenderersEnabled(value)
                                }

                            })
                }
                var cocoaRenderers: Cell<JBCheckBox>? = null

                indent {
                    row {

                        cocoaRenderers =
                            checkBox(DebuggerBundle.message("debug.settings.enableCocoaRenderers.checkbox"))
                                .bindSelected(object :
                                    MutableProperty<Boolean> {
                                    override fun get(): Boolean = settings.isCocoaRenderersEnabled()

                                    override fun set(value: Boolean) {
                                        settings.setCocoaRenderersEnabled(value)
                                    }

                                }).enabledIf(renderers?.selected!!)
                    }

                    indent {
                        row {
                            checkBox(DebuggerBundle.message("debug.settings.enableCoreDataRenderers.checkbox")).bindSelected(
                                object :
                                    MutableProperty<Boolean> {
                                    override fun get(): Boolean = settings.isCoreDataRenderersEnabled()

                                    override fun set(value: Boolean) {
                                        settings.setCoreDataRenderersEnabled(value)
                                    }
                                }
                            ).enabledIf(renderers?.selected!!.and(cocoaRenderers?.selected!!))
                        }

                    }
                }
            }

            row {
                checkBox(DebuggerBundle.message("debug.settings.enableGNUSTLRenderers.checkbox")).bindSelected(
                    object :
                        MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isStlRenderersEnabled()

                        override fun set(value: Boolean) {
                            settings.setStlRenderersEnabled(value)
                        }
                    }).comment(DebuggerBundle.message("debug.settings.enableGNUSTLRenderers.checkbox.description"))
            }
            var natvisRenderers: Cell<JBCheckBox>?
            row {
                natvisRenderers =
                    checkBox(DebuggerBundle.message("debug.settings.enableLLDBNatvisRenderers.checkbox")).bindSelected(
                        object : MutableProperty<Boolean> {
                            override fun get(): Boolean = settings.isLLDBNatvisRenderersEnabled()

                            override fun set(value: Boolean) {
                                settings.setLLDBNatvisRenderersEnabled(value)
                            }

                        })
                        .comment(DebuggerBundle.message("debug.settings.enableLLDBNatvisRenderers.checkbox.description"))
                label(DebuggerBundle.message("debug.settings.enableLLDBNatvisRenderers.diagnostics"))
                comboBox(EnumComboBoxModel(LLDBNatvisDiagnosticsLevel::class.java)).bindItem(
                    object : MutableProperty<LLDBNatvisDiagnosticsLevel?> {
                        override fun get(): LLDBNatvisDiagnosticsLevel? {
                            return settings.getLLDBNatvisDiagnosticsLevel()
                        }

                        override fun set(value: LLDBNatvisDiagnosticsLevel?) {

                            settings.setLLDBNatvisDiagnosticsLevel(value)
                        }

                    }
                ).enabledIf(natvisRenderers.selected)

            }
            row {
                checkBox(DebuggerBundle.message("debug.settings.enableValuesFilter.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isValuesFilterEnabled()
                        override fun set(value: Boolean) {
                            settings.setValuesFilterEnabled(value)
                        }
                    }
                ).comment(DebuggerBundle.message("debug.settings.enableValuesFilter.checkbox.hint"))
            }


            var hex: Cell<JBCheckBox>? = null
            row {


                hex =
                    checkBox(DebuggerBundle.message("debug.settings.enableHexNumberFormatting.checkbox")).bindSelected(
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

                    checkBox((DebuggerBundle.message("debug.settings.enableHexNumberFormatting.asSecondary.checkbox"))).bindSelected(
                        object : MutableProperty<Boolean> {
                            override fun get(): Boolean = settings.isHexAsSecondaryFormattingEnabled()
                            override fun set(value: Boolean) {
                                settings.setHexAsSecondaryFormattingEnabled(value)
                            }
                        })
                        .comment(DebuggerBundle.message("debug.settings.enableHexNumberFormatting.asSecondary.checkbox.hint"))
                        .enabledIf(hex?.selected!!)

                }
            }
            row {
                checkBox(
                    DebuggerBundle.message("debug.settings.showTypeTemplateArguments.checkbox")
                ).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isShowTypeTemplateArguments()
                        override fun set(value: Boolean) {
                            settings.setShowTypeTemplateArguments(value)
                        }
                    })


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
            row {
                checkBox(DebuggerBundle.message("debug.settings.showFrameFunctionParameterTypes.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isShowFrameFunctionParameters()
                        override fun set(value: Boolean) {
                            settings.setShowFrameFunctionParameters(value)
                        }
                    }
                )
            }

            row {

                checkBox(DebuggerBundle.message("debug.settings.showFrameFunctionTemplateArguments.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isShowFrameFunctionTemplateArguments()
                        override fun set(value: Boolean) {
                            settings.setShowFrameFunctionTemplateArguments(value)
                        }
                    })

            }
        }
        group(DebuggerBundle.message("debug.settings.cxxTypes.separator")) {

            row {
                checkBox(DebuggerBundle.message("debug.settings.stripCxxStdAbiVersionNamespace.checkbox")).bindSelected(

                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isStripCxxAuxiliaryNamespaces()
                        override fun set(value: Boolean) {
                            settings.setStripCxxAuxiliaryNamespaces(value)
                        }
                    })
                    .comment(DebuggerBundle.message("debug.settings.stripCxxStdAbiVersionNamespace.checkbox.description"))
            }
            row {
                checkBox(DebuggerBundle.message("debug.settings.sugarizeCxxStlTypes.checkbox")).bindSelected(
                    object : MutableProperty<Boolean> {
                        override fun get(): Boolean = settings.isSugarizeCxxStlTypes()
                        override fun set(value: Boolean) {
                            settings.setSugarizeCxxStlTypes(value)
                        }
                    }
                ).comment(DebuggerBundle.message("debug.settings.sugarizeCxxStlTypes.checkbox.description"))
            }
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

