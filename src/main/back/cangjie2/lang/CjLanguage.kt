package com.huawei.cangjie.lang

import com.intellij.lang.Language

object CjLanguage : Language("CangJie") {




    private fun readResolve(): Any = CjLanguage


    override fun isCaseSensitive() = true

    override fun getDisplayName() = "CangJie"
}



