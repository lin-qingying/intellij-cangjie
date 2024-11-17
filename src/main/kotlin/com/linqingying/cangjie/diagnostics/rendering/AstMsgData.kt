/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.diagnostics.rendering

import com.linqingying.utils.AstMsgType

class AstMsgData private constructor(private val messages: MutableMap<AstMsgType, () -> String>) {

    data class AstMsgModule(val type: AstMsgType, val message: String)

    fun addMessage(type: AstMsgType, message: () -> String) {
        messages[type] = message
    }

    fun addMessage(type: AstMsgType, message: String) {
        messages[type] = { message }
    }

    companion object {
        @JvmStatic
        fun create(messages: Map<AstMsgType, () -> String>): AstMsgData {
            return AstMsgData(messages.toMutableMap())
        }
        @JvmStatic

        fun create(type: AstMsgType, message: String): AstMsgData {
            return AstMsgData(mutableMapOf(type to { message }))
        }
        @JvmStatic
        fun create(): AstMsgData {
            return AstMsgData(mutableMapOf())
        }
        @JvmStatic
        fun create(vararg modules: AstMsgModule): AstMsgData {
//            合并类型，并将string转为函数

            val groupedModules = modules.groupBy { it.type }
            val messageMap = groupedModules.mapValues { (_, moduleList) ->
                { -> moduleList.random().message }
            }.toMutableMap()
            return AstMsgData(messageMap)
        }
    }


    fun getMessage(type: AstMsgType): String {
        return messages[type]?.let { it() } ?: ""

    }
}
