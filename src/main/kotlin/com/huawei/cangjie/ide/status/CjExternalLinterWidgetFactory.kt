package com.huawei.cangjie.ide.status

import com.huawei.cangjie.CangJieBundle
import com.huawei.cangjie.cjpm.project.model.CjpmProject
import com.huawei.cangjie.cjpm.project.model.CjpmProjectsService
import com.huawei.cangjie.cjpm.project.settings.externalLinterSettings
import com.huawei.cangjie.cjpm.project.toolwindow.hasCjpmProject
import com.huawei.cangjie.cjpm.toolchain.ExternalLinter
import com.huawei.cangjie.icon.CangJieIcons
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
