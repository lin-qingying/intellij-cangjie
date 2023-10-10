
package com.huawei.cj.lang

import com.intellij.lang.Language

object RsLanguage : Language("CangJie", "text/CangJie", "text/x-rust", "application/x-rust") {
    override fun isCaseSensitive() = true

    override fun getDisplayName() = "CangJie"
}



