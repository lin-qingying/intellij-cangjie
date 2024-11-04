
package com.linqingying.cangjie.ide.formatter

import com.linqingying.cangjie.lang.CangJieLanguage
import com.intellij.application.options.CodeStyle
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings


val CodeStyleSettings.cangjieCommonSettings: CangJieCommonCodeStyleSettings
    get() = getCommonSettings(CangJieLanguage) as CangJieCommonCodeStyleSettings

val CodeStyleSettings.cangjieCustomSettings: CangJieCodeStyleSettings
    get() = getCustomSettings(CangJieCodeStyleSettings::class.java)

fun CodeStyleSettings.cangjieCodeStyleDefaults(): String? = cangjieCustomSettings.CODE_STYLE_DEFAULTS?.takeIf { customStyleId ->
    customStyleId == cangjieCommonSettings.CODE_STYLE_DEFAULTS
}

fun CodeStyleSettings.supposedCangJieCodeStyleDefaults(): String? =
    cangjieCustomSettings.CODE_STYLE_DEFAULTS ?: cangjieCommonSettings.CODE_STYLE_DEFAULTS

val PsiFile.cangjieCommonSettings: CangJieCommonCodeStyleSettings get() = CodeStyle.getSettings(this).cangjieCommonSettings
val PsiFile.cangjieCustomSettings: CangJieCodeStyleSettings get() = CodeStyle.getSettings(this).cangjieCustomSettings
val PsiFile.rightMarginOrDefault: Int get() = CodeStyle.getSettings(this).getRightMargin(CangJieLanguage)
