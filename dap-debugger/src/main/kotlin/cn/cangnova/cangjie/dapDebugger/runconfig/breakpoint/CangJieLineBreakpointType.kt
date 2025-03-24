package cn.cangnova.cangjie.dapDebugger.runconfig.breakpoint

import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import cn.cangnova.cangjie.dapDebugger.runconfig.CangJieDebuggerCoreBundle
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
 
import cn.cangnova.cangjie.lang.CangJieFileType

class CangJieLineBreakpointType : XLineBreakpointType<CangJieLineBreakpointType.Properties>(
    ID,
    CangJieDebuggerCoreBundle.message("line.breakpoint.tab.title")
) {

    class Properties : XBreakpointProperties<Properties.State>() {
        @Tag("breakpoint-state")
        class State {
            @Attribute("enabled")
            var enabled: Boolean = true
            
            @Attribute("valid")
            var valid: Boolean = false
            
            @Attribute("condition")
            var condition: String? = null
            
            @Attribute("suspend-policy")
            var suspendPolicy: String = "ALL"  // ALL, THREAD, NONE
            
            @Attribute("log-enabled")
            var logEnabled: Boolean = false
            
            @Attribute("log-expression")
            var logExpression: String? = null
            
            @Attribute("message")
            var message: String? = null
            
            @Attribute("reason")
            var reason: String? = null
        }

        private var myState = State()

        override fun getState(): State = myState

        override fun loadState(state: State) {
            myState = state
        }

        fun isEnabled(): Boolean = myState.enabled
        fun setEnabled(enabled: Boolean) {
            myState.enabled = enabled
        }

        fun isValid(): Boolean = myState.valid
        fun setValid(valid: Boolean) {
            myState.valid = valid
        }

        fun getCondition(): String? = myState.condition
        fun setCondition(condition: String?) {
            myState.condition = condition
        }

        fun getSuspendPolicy(): String = myState.suspendPolicy
        fun setSuspendPolicy(policy: String) {
            myState.suspendPolicy = policy
        }

        fun isLogEnabled(): Boolean = myState.logEnabled
        fun setLogEnabled(enabled: Boolean) {
            myState.logEnabled = enabled
        }

        fun getLogExpression(): String? = myState.logExpression
        fun setLogExpression(expression: String?) {
            myState.logExpression = expression
        }

        fun setMessage(message: String?) {
            myState.message = message
        }

        fun getMessage(): String? = myState.message

        fun setReason(reason: String?) {
            myState.reason = reason
        }

        fun getReason(): String? = myState.reason
    }

    override fun createBreakpointProperties(file: VirtualFile, line: Int): Properties {
        return Properties()
    }

    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean {
        return file.fileType == CangJieFileType.INSTANCE 
    }

    companion object {
        const val ID = "cangjie-line"
    }
}
