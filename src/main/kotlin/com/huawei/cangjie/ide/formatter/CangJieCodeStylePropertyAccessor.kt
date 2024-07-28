
package com.huawei.cangjie.ide.formatter

import com.intellij.application.options.codeStyle.properties.CodeStyleChoiceList
import com.intellij.application.options.codeStyle.properties.CodeStylePropertyAccessor


class CangJieCodeStylePropertyAccessor(private val cangjieCodeStyle: CangJieCodeStyleSettings) :
    CodeStylePropertyAccessor<String>(),
    CodeStyleChoiceList {
    override fun set(extVal: String): Boolean = applyCangJieCodeStyle(extVal, cangjieCodeStyle.container)
    override fun get(): String? = cangjieCodeStyle.container.cangjieCodeStyleDefaults()
    override fun parseString(string: String): String = string
    override fun valueToString(value: String): String = value
    override fun getChoices(): List<String> = listOf(CangJieStyleGuideCodeStyle.CODE_STYLE_ID, CangJieObsoleteCodeStyle.CODE_STYLE_ID)
    override fun getPropertyName(): String = "code_style_defaults"
}
