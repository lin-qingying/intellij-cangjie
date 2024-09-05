package com.linqingying.utils


object Config {
    val isLsp: Boolean = false
    val astMsgType: AstMsgType = AstMsgType.FUNNY

}


//ast分析的消息提示类型
enum class AstMsgType {
    NORMAL, FUNNY
}
