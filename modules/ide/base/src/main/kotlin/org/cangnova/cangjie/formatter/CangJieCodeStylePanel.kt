/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */


package org.cangnova.cangjie.formatter

import org.cangnova.cangjie.lang.CangJieLanguage
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
