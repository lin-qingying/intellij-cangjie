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

package cn.cangnova.cangjie.ide.status

import cn.cangnova.cangjie.cjpm.project.model.CjpmProject
import cn.cangnova.cangjie.cjpm.project.model.CjpmProjectsService
import cn.cangnova.cangjie.cjpm.project.settings.externalLinterSettings
import cn.cangnova.cangjie.cjpm.project.toolwindow.hasCjpmProject
import cn.cangnova.cangjie.icon.CangJieIcons
import cn.cangnova.cangjie.messages.CangJieBundle
import cn.cangnova.cangjie.toolchain.ExternalLinter
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.TextPanel
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent


class CjExternalLinterWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = CjExternalLinterWidget.ID
    override fun getDisplayName(): String = CangJieBundle.message("configurable.name.cangjie.external.linter")
    override fun isAvailable(project: Project): Boolean = project.hasCjpmProject
    override fun createWidget(project: Project): StatusBarWidget = CjExternalLinterWidget(project)
    override fun disposeWidget(widget: StatusBarWidget) = Disposer.dispose(widget)
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}

class CjExternalLinterWidget(private val project: Project) : TextPanel.WithIconAndArrows(), CustomStatusBarWidget {
    override fun getComponent(): JComponent = this
    private var statusBar: StatusBar? = null
    private val linter: ExternalLinter get() = project.externalLinterSettings.tool
    private val turnedOn: Boolean get() = project.externalLinterSettings.runOnTheFly

    var inProgress: Boolean = false
        set(value) {
            field = value
            update()
        }

    init {
        setTextAlignment(CENTER_ALIGNMENT)
        border = JBUI.CurrentTheme.StatusBar.Widget.border()
    }
    override fun ID(): String = ID
    override fun dispose() {
        statusBar = null
        UIUtil.dispose(this)
    }
    companion object {
        const val ID: String = "cangjieExternalLinterWidget"
    }
    private fun update() {
        if (project.isDisposed) return
        UIUtil.invokeLaterIfNeeded {
            if (project.isDisposed) return@invokeLaterIfNeeded
            text = linter.title
            val status = if (turnedOn) CangJieBundle.message("on") else CangJieBundle.message("off")
            toolTipText = CangJieBundle.message("0.2.choice.0.is.in.progress.1.on.the.fly.analysis.is.turned.1", linter.title, status, if (inProgress) 0 else 1)
            icon = when {
                !turnedOn -> CangJieIcons.GEAR_OFF
                inProgress -> CangJieIcons.GEAR_ANIMATED
                else -> CangJieIcons.GEAR
            }
            repaint()
        }
    }

}

class CjExternalLinterWidgetUpdater(private val project: Project) :CjpmProjectsService.CjpmProjectsListener {
    override fun cjpmProjectsUpdated(service: CjpmProjectsService, projects: Collection<CjpmProject>) {
        val manager = project.service<StatusBarWidgetsManager>()
        manager.updateWidget(CjExternalLinterWidgetFactory::class.java)
    }
}
