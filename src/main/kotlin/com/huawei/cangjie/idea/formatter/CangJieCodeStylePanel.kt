
package com.huawei.cangjie.idea.formatter

import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider

class CangJieCodeStylePanel(currentSettings: CodeStyleSettings, settings: CodeStyleSettings) :
    TabbedLanguageCodeStylePanel(CangJieLanguage, currentSettings, settings) {
    override fun initTabs(settings: CodeStyleSettings) {
        super.initTabs(settings)

        addTab(ImportSettingsPanelWrapper(settings))
        addTab(CangJieOtherSettingsPanel(settings))
        for (provider in CodeStyleSettingsProvider.EXTENSION_POINT_NAME.extensions) {
            if (provider.language == CangJieLanguage && !provider.hasSettingsPage()) {
                createTab(provider)
            }
        }

        addTab(CangJieSaveStylePanel(settings))
    }
}
