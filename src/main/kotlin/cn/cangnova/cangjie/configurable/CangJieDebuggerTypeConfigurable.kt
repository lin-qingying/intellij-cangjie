package cn.cangnova.cangjie.configurable//package cn.cangnova.cangjie.configurable
//
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.components.PersistentStateComponent
//import com.intellij.openapi.components.Service
//import com.intellij.openapi.components.State
//import com.intellij.openapi.components.Storage
//import com.intellij.openapi.options.Configurable
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.DialogPanel
//import com.intellij.ui.dsl.builder.bindItem
//import com.intellij.ui.dsl.builder.panel
//import com.intellij.util.xmlb.XmlSerializerUtil
//import cn.cangnova.cangjie.messages.CangJieBundle
//import cn.cangnova.cangjie.configurable.CjConfigurableBase
//import cn.cangnova.cangjie.configurable.services.CangJieLanguageServerServices
//import cn.cangnova.cangjie.configurable.state.PluginLanguageState
//import java.util.Locale
//
//class CangJieDebuggerTypeConfigurable(override val project: Project) :
//    CjConfigurableBase(project, CangJieBundle.message("CangJie.debugger")), Configurable.NoScroll {
//
//
//    override fun createPanel(): DialogPanel = panel {
//        group(CangJieBundle.message("CangJie.debugger.implementation.type")) {
//            indent {
//                row {
//                    comboBox(DebuggerType.entries)
//                        .label(CangJieBundle.message("CangJie.debugger.implementation.type.select"))
//                        .bindItem(
//                            { CangJieDebuggerServices.instance.type },
//                            { selectedType ->
//
//
//                                CangJieDebuggerServices.instance.type = selectedType!!
//
//                            }
//                        )
//                }
//            }
//        }
//    }
//
//
//
//}
//
//@State(name = "CangJieDebuggerServices", storages = [Storage("cangjie.debugger.xml")])
//@Service(Service.Level.APP)
//class CangJieDebuggerServices : PersistentStateComponent<CangJieDebuggerServices> {
//    var type: DebuggerType = DebuggerType.LLDB
//
//    override fun getState(): CangJieDebuggerServices {
//        return this
//    }
//
//    override fun loadState(state: CangJieDebuggerServices) {
//        XmlSerializerUtil.copyBean(state, this)
//
//    }
//
//
//    companion object {
//        val instance: CangJieDebuggerServices
//            get() {
//                return ApplicationManager.getApplication().getService(CangJieDebuggerServices::class.java)
//            }
//    }
//}
//enum class DebuggerType(val displayName: String) {
//    LLDB(CangJieBundle.message("CangJie.debugger.implementation.type.lldb")),
//    DAP(CangJieBundle.message("CangJie.debugger.implementation.type.dap")),
//    DAP_LSP4J(CangJieBundle.message("CangJie.debugger.implementation.type.dap_lsp4j"));
//
//    override fun toString(): String {
//        return displayName
//    }
//
//
//}
