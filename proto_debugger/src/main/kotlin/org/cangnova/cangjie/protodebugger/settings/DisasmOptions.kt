package org.cangnova.cangjie.protodebugger.settings

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.xdebugger.XDebugSession
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import org.cangnova.cangjie.protodebugger.core.CangJieDebugProcess
import org.cangnova.cangjie.protodebugger.memory.MemoryDoc
import org.cangnova.cangjie.protodebugger.services.DisasmService


interface MemoryDocOptions {
    companion object {
        val DEFAULT: MemoryDocOptions = object : MemoryDocOptions {
        }
    }

    fun configure(memoryDoc: MemoryDoc<*>) {
    }

    fun getOptionActions(memoryDoc: MemoryDoc<*>): List<AnAction> = emptyList()
}


class DisasmOptions(val session: XDebugSession) :
    MemoryDocOptions {
    companion object {
        private const val USE_INTEL_SYNTAX: String = "cangjie.debugger.disasm.use.intel.syntax"

        @JvmStatic
        fun getUseIntelSyntax(): Boolean? {

            return PropertiesComponent.getInstance().getValue("cangjie.debugger.disasm.use.intel.syntax")?.toBoolean()
        }

        @JvmStatic
        fun saveUseIntelSyntax(useIntelSyntax: Boolean?): Unit {
            PropertiesComponent.getInstance().setValue(
                "cangjie.debugger.disasm.use.intel.syntax",
                useIntelSyntax?.toString(), null as String?
            )

        }
    }


    override fun getOptionActions(memoryDoc: MemoryDoc<*>): List<AnAction> {
        return listOf(IntelSyntaxAction(session, memoryDoc))
    }
}

/**
 * XDebugSession的扩展属性，用于获取反汇编服务
 *
 * 该扩展属性提供了从调试会话中获取反汇编服务的便捷方法。
 * 只有在调试会话未停止且调试进程是CangJieDebugProcess类型时才返回服务。
 */
val XDebugSession.disasmService: DisasmService?
    get() {
        val process = if (!this.isStopped) {
            val debugProcess = this.debugProcess
            debugProcess as? CangJieDebugProcess
        } else {
            null
        }
        return process?.disasmService
    }

/**
 * Intel语法切换动作
 *
 * 该动作类用于在调试器中切换反汇编语法的显示风格，
 * 允许用户在Intel语法和AT&T语法之间进行切换。
 *
 * @param session 调试会话对象
 * @param memoryDoc 内存文档对象，用于显示反汇编内容
 */
class IntelSyntaxAction(
    private val session: XDebugSession,
    private val memoryDoc: MemoryDoc<*>
) : DumbAwareToggleAction("Use Intel Syntax") {

    /**
     * 检查动作是否被选中
     *
     * 根据当前保存的Intel语法设置状态来确定复选框状态。
     *
     * @param e 动作事件对象
     * @return 如果启用了Intel语法返回true，否则返回false
     */
    override fun isSelected(e: AnActionEvent): Boolean {
        return DisasmOptions.getUseIntelSyntax() ?: false
    }

    /**
     * 设置动作选中状态
     *
     * 当用户点击该动作时，根据新的选中状态更新Intel语法设置，
     * 并通知调试器切换反汇编语法风格。
     *
     * @param e 动作事件对象
     * @param state 新的选中状态，true表示使用Intel语法，false表示使用AT&T语法
     */
    @OptIn(DelicateCoroutinesApi::class)
    override fun setSelected(e: AnActionEvent, state: Boolean) {
        // 保存新的Intel语法设置
        DisasmOptions.saveUseIntelSyntax(state)

        // 获取反汇编服务并设置语法风格
        val disasmService = session.disasmService
        if (disasmService != null) {
            val flavor = if (state) {
                DisasmFlavor.INTEL
            } else {
                DisasmFlavor.ATT
            }

            // 异步设置反汇编语法风格
            // 注意：这里需要在协程中执行，因为setDisasmFlavor是suspend函数
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    disasmService.setDisasmFlavor(flavor)
                    // 设置成功后，可以刷新内存文档显示
                    memoryDoc.refresh()
                } catch (ex: Exception) {
                    // 处理设置失败的情况
                    println("Failed to set disasm flavor: ${ex.message}")
                }
            }
        }
    }

    /**
     * 更新动作的显示状态
     *
     * 根据当前调试会话的状态来决定是否启用该动作。
     * 只有在调试会话未停止且存在反汇编服务时才启用。
     *
     * @param e 动作事件对象
     */
    override fun update(e: AnActionEvent) {
        val disasmService = session.disasmService
        e.presentation.isEnabled = disasmService != null && !session.isStopped
        super.update(e)
    }
}



