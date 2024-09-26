package com.huawei.cangjie.types.expressions

enum class ProcessingMode {
    CHILD,       // 子类处理

    PARENT,      // 父类处理
    DEFAULT       // 默认值（正常处理）
}

data class ContextConfig(
    val processingMode: ProcessingMode = ProcessingMode.DEFAULT,


)
{
    companion object{
        @JvmField
        val DEFAULT = ContextConfig()
    }
}
