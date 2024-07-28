

package com.huawei.cangjie.ide.formatter

import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.lang.Language
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.PredefinedCodeStyle

abstract class CangJiePredefinedCodeStyle(@NlsContexts.ListItem name: String, language: Language) :
    PredefinedCodeStyle(name, language) {
    abstract val codeStyleId: String
}

class CangJieStyleGuideCodeStyle :
    CangJiePredefinedCodeStyle(CangJieStyleBundle.message("list.item.cangjie.style.guide"), CangJieLanguage) {
    override val codeStyleId: String = CODE_STYLE_ID

    override fun apply(settings: CodeStyleSettings) {
        Companion.apply(settings)
    }

    companion object {
        val INSTANCE = CangJieStyleGuideCodeStyle()

        const val CODE_STYLE_ID = "KOTLIN_OFFICIAL"
        const val CODE_STYLE_SETTING = "official"
        const val CODE_STYLE_TITLE = "CangJie Coding Conventions"

        fun apply(settings: CodeStyleSettings) {
            applyToCangJieCustomSettings(settings.cangjieCustomSettings)
            applyToCommonSettings(settings.cangjieCommonSettings)
        }

        fun applyToCangJieCustomSettings(
            cangjieCustomSettings: CangJieCodeStyleSettings,
            modifyCodeStyle: Boolean = true
        ) {
            cangjieCustomSettings.apply {
                if (modifyCodeStyle) {
                    CODE_STYLE_DEFAULTS = CODE_STYLE_ID
                }

                CONTINUATION_INDENT_IN_PARAMETER_LISTS = false
                CONTINUATION_INDENT_IN_ARGUMENT_LISTS = false
                CONTINUATION_INDENT_FOR_EXPRESSION_BODIES = false
                CONTINUATION_INDENT_FOR_CHAINED_CALLS = false
                CONTINUATION_INDENT_IN_SUPERTYPE_LISTS = false
                CONTINUATION_INDENT_IN_IF_CONDITIONS = false
                CONTINUATION_INDENT_IN_ELVIS = false
                WRAP_EXPRESSION_BODY_FUNCTIONS = CodeStyleSettings.WRAP_AS_NEEDED
                IF_RPAREN_ON_NEW_LINE = true
                ALLOW_TRAILING_COMMA = false
            }
        }

        fun applyToCommonSettings(commonSettings: CommonCodeStyleSettings, modifyCodeStyle: Boolean = true) {
            commonSettings.apply {
                WHILE_ON_NEW_LINE = false
                ELSE_ON_NEW_LINE = false
                CATCH_ON_NEW_LINE = false
                FINALLY_ON_NEW_LINE = false

                CALL_PARAMETERS_WRAP = CodeStyleSettings.WRAP_AS_NEEDED + CodeStyleSettings.WRAP_ON_EVERY_ITEM
                CALL_PARAMETERS_LPAREN_ON_NEXT_LINE = true
                CALL_PARAMETERS_RPAREN_ON_NEXT_LINE = true

                METHOD_PARAMETERS_WRAP = CodeStyleSettings.WRAP_AS_NEEDED + CodeStyleSettings.WRAP_ON_EVERY_ITEM
                METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE = true
                METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE = true

                EXTENDS_LIST_WRAP = CodeStyleSettings.WRAP_AS_NEEDED
                METHOD_CALL_CHAIN_WRAP = CodeStyleSettings.WRAP_AS_NEEDED
                ASSIGNMENT_WRAP = CodeStyleSettings.WRAP_AS_NEEDED

                ALIGN_MULTILINE_BINARY_OPERATION = false
            }

            if (modifyCodeStyle && commonSettings is CangJieCommonCodeStyleSettings) {
                commonSettings.CODE_STYLE_DEFAULTS = CODE_STYLE_ID
            }
        }
    }
}
