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

package org.cangnova.cangjie.debugger.dap.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import org.cangnova.cangjie.lang.CangJieFileType

/**
 * 仓颉行断点类型
 *
 * 定义仓颉语言的行断点行为和属性
 */
class CangJieLineBreakpointType : XLineBreakpointType<CangJieLineBreakpointType.Properties>(
    ID,
    "CangJie Line Breakpoint"
) {

    /**
     * 断点属性
     */
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

/**
 * Type alias for backward compatibility
 */
typealias CangJieLineBreakpointProperties = CangJieLineBreakpointType.Properties