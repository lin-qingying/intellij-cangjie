/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.ide.newProject.ui

import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.cjpm.project.settings.ui.CangJieProjectSettingsPanel
import com.linqingying.cangjie.cjpm.toolchain.cjpm
import com.linqingying.cangjie.cjpm.toolchain.tools.Cjpm
import com.linqingying.cangjie.ide.project.settings.ui.UiDebouncer
import com.linqingying.cangjie.ide.project.tools.projectWizard.CangJieProjectTypeItem
import com.linqingying.cangjie.ide.project.tools.projectWizard.CangJieUiBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.COLUMNS_MEDIUM
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.columns
import java.nio.file.Path
import java.nio.file.Paths

class CjNewProjectPanel(

    private val showProjectTypeSelection: Boolean,
    cjpmProjectDir: Path = Paths.get("."),
    private val updateListener: (() -> Unit)? = null
) : Disposable {
    //        项目类型
    private val projectTypeComboBox: ComboBox<CangJieProjectTypeItem> = ComboBox<CangJieProjectTypeItem>().apply {

//            添加条目
        addItem(CangJieProjectTypeItem("executable", CangJieBundle.message("cangjie.project.type.executable")))
        addItem(CangJieProjectTypeItem("static", CangJieBundle.message("cangjie.project.type.static")))
        addItem(CangJieProjectTypeItem("dynamic", CangJieBundle.message("cangjie.project.type.dynamic")))


    }
    private val cangjieProjectSettings = CangJieProjectSettingsPanel(

        cjpmProjectDir, updateListener)
    private val cjpm: Cjpm?
        get() = cangjieProjectSettings.data.toolchain?.cjpm()


    private val updateDebouncer = UiDebouncer(this)
    private var needInstallCjpmGenerate = false

    val data: ConfigurationData
        get() = ConfigurationData(
            cangjieProjectSettings.data,
            (projectTypeComboBox.selectedItem as CangJieProjectTypeItem).type
        )

    fun update() {
        updateDebouncer.run(
            onPooledThread = {

            },
            onUiThread = { needInstall ->


                updateListener?.invoke()
            }
        )
    }

    @Throws(ConfigurationException::class)
    fun validateSettings() {
        cangjieProjectSettings.validateSettings()

        if (needInstallCjpmGenerate) {
            @Suppress("DialogTitleCapitalization")
            throw ConfigurationException(CangJieBundle.message("dialog.message.cjpm.generate.needed.to.create.project.from.custom.template"))
        }
    }


    fun attachTo(panel: Panel) = with(panel) {
        cangjieProjectSettings.attachTo(this)

        if (showProjectTypeSelection) {

            row(CangJieUiBundle.message("action.new.project.projecttype.title")) {
                cell(projectTypeComboBox)
                    .columns(COLUMNS_MEDIUM)
                    .component
            }.bottomGap(BottomGap.SMALL)

        }

        update()
    }


    override fun dispose() {
        Disposer.dispose(cangjieProjectSettings)

    }
}

data class ConfigurationData(
    val settings: CangJieProjectSettingsPanel.Data,
    val projectType: String
)
