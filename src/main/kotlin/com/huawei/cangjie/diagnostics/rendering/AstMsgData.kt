package com.huawei.cangjie.diagnostics.rendering

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
