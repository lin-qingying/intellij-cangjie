package com.linqingying.cangjie.lang

import com.intellij.lang.Language


object CangJieLanguage : Language("CangJie"){


    val NAME:String = "CangJie"

//    private fun readResolve(): Any = CangJieLanguage

    override fun isCaseSensitive() = true

    override fun getDisplayName() =  NAME

}
