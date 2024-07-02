package com.huawei.cangjie.name

object NameUtils{
    @JvmStatic
    val CONTEXT_RECEIVER_PREFIX = "\$context_receiver"
    @JvmStatic
    fun contextReceiverName(index: Int): Name =
        Name.identifier("${CONTEXT_RECEIVER_PREFIX}_$index")
}