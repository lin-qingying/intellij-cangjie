package com.huawei.cangjie.lang

import com.intellij.lang.Language


object CangJieLanguage : Language("CangJie"){



//    private fun readResolve(): Any = CangJieLanguage

    override fun isCaseSensitive() = true

    override fun getDisplayName() = "CangJie"

}
