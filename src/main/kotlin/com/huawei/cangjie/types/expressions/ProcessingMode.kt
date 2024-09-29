package com.huawei.cangjie.types.expressions

import com.intellij.psi.PsiElement

enum class ProcessingMode {
    CHILD,       // 子类处理

    PARENT,      // 父类处理
    DEFAULT       // 默认值（正常处理）
}

data class ContextConfig(
    val processingMode: ProcessingMode = ProcessingMode.DEFAULT,

    ) {
    var addVariableDescriptor: MutableMap<PsiElement,MutableList< (context: Any) -> Unit>> = mutableMapOf()
    var getEnumEntryType = false

    companion object {
        @JvmField
        val DEFAULT = ContextConfig()
    }
}
