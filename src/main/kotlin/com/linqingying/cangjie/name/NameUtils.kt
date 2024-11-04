package com.linqingying.cangjie.name

object NameUtils{
    @JvmStatic
    val CONTEXT_RECEIVER_PREFIX = "\$context_receiver"
    @JvmStatic
    fun contextReceiverName(index: Int): Name =
        Name.identifier("${CONTEXT_RECEIVER_PREFIX}_$index")



    @JvmStatic
    fun getPackagePartClassNamePrefix(shortFileName: String): String =
        if (shortFileName.isEmpty())
            "_"
        else
            shortFileName

}
