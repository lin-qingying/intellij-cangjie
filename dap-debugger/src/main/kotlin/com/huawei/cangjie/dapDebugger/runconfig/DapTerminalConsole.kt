package com.huawei.cangjie.dapDebugger.runconfig

import com.google.common.base.Ascii
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.terminal.*
import com.intellij.terminal.pty.PtyProcessTtyConnector
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.util.LineSeparator
import com.intellij.util.ui.update.UiNotifyConnector
import com.jediterm.terminal.util.CharUtils
import org.jetbrains.plugins.terminal.*
import java.awt.Color
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.BoundedRangeModel
import javax.swing.JComponent
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import kotlin.concurrent.Volatile

class DapTerminalConsole(val project: Project) : ConsoleView {
    companion object {
        private val LOG = Logger.getInstance(DapTerminalConsole::class.java)

        private const val MAKE_CURSOR_INVISIBLE = "\u001b[?25l"
        private const val MAKE_CURSOR_VISIBLE = "\u001b[?25h"
        private const val CLEAR_SCREEN = "\u001b[2J"
        private fun encodeColor(color: Color): String {
            return Char(Ascii.ESC.toUShort()).toString() + "[" + "38;2;" + color.red + ";" + color.green + ";" +
                    color.blue + "m"
        }

        private fun startsWithClearScreen(text: String): Boolean {
            // ConPTY will randomly send these commands at any time, so we should skip them:
            var offset = 0
            while (text.startsWith(MAKE_CURSOR_INVISIBLE, offset) || text.startsWith(
                    MAKE_CURSOR_VISIBLE, offset
                )
            ) {
                offset += MAKE_CURSOR_INVISIBLE.length
            }

            return text.startsWith(CLEAR_SCREEN, offset)
        }

    }

    private val firstOutput = AtomicBoolean(false)


    val terminalRunner = LocalTerminalDirectRunner(project)
    val option = terminalRunner.configureStartupOptions(ShellStartupOptions.Builder().build())

    private val terminalWidget: TerminalWidget = terminalRunner.startShellTerminalWidget(this, option, false)

    @Volatile
    private var lastCR = false
    private val dataStream: AppendableTerminalDataStream = AppendableTerminalDataStream()

    //    private val contentHelper = TerminalConsoleContentHelper(this)
    val press: Process
        get() {
            return (terminalWidget.ttyConnector as PtyProcessTtyConnector).process
        }

    private val ttyConnectorFuture: CompletableFuture<Boolean> = CompletableFuture<Boolean>()

    private inner class TerminalListener : JBTerminalWidgetListener {
        override fun onNewSession() {

        }

        override fun onTerminalStarted() {


            ttyConnectorFuture.complete(true)
        }

        override fun onPreviousTabSelected() {

        }

        override fun onNextTabSelected() {

        }

        override fun onSessionClosed() {

        }

        override fun showTabs() {

        }

    }

    init {
        component.listener = TerminalListener()

    }

    val shellId: Long
        get() {

            printText("测试", ConsoleViewContentType.NORMAL_OUTPUT)

            ttyConnectorFuture.join()
//            terminalWidget.ttyConnector as PtyProcessTtyConnector
            return press.pid()
        }


    override fun dispose() {

        press.onExit()
    }

    override fun getComponent(): JBTerminalWidget {
        return terminalWidget.component as JBTerminalWidget
    }

    override fun getPreferredFocusableComponent(): JComponent {
        return terminalWidget.preferredFocusableComponent
    }

    private fun convertTextToCRLF(text: String): String {
        if (text.isEmpty()) return text
        // Handle the case when \r and \n are in different chunks: "text1 \r" and "\n text2"
        val preserveFirstLF = text.startsWith(LineSeparator.LF.separatorString) && lastCR
        val preserveLastCR = text.endsWith(LineSeparator.CR.separatorString)
        lastCR = preserveLastCR
        val textToConvert =
            text.substring(if (preserveFirstLF) 1 else 0, if (preserveLastCR) text.length - 1 else text.length)
        var textCRLF = StringUtil.convertLineSeparators(textToConvert, LineSeparator.CRLF.separatorString)
        if (preserveFirstLF) {
            textCRLF = LineSeparator.LF.separatorString + textCRLF
        }
        if (preserveLastCR) {
            textCRLF += LineSeparator.CR.separatorString
        }
        return textCRLF
    }

    override fun print(text: String, contentType: ConsoleViewContentType) {


        // Convert line separators to CRLF to behave like ConsoleViewImpl.
        // For example, stacktraces passed to com.intellij.execution.testframework.sm.runner.SMTestProxy.setTestFailed have
        // only LF line separators on Unix.
        val textCRLF: String = convertTextToCRLF(text)
        try {
            printText(textCRLF, contentType)
        } catch (e: IOException) {
            LOG.info(e)
        }
    }


    val terminalPanel: JBTerminalPanel
        get() {
            return component.terminalPanel
        }

    @Throws(IOException::class)
    private fun printText(text: String, contentType: ConsoleViewContentType?) {
        if (LOG.isTraceEnabled) {
            LOG.trace(
                "[" + Thread.currentThread().name + "] Print request received: " + CharUtils.toHumanReadableText(
                    text
                )
            )
        }
        val foregroundColor = contentType?.attributes?.foregroundColor
        if (foregroundColor != null) {
            dataStream.append(encodeColor(foregroundColor))
        }

        if (contentType !== ConsoleViewContentType.SYSTEM_OUTPUT && firstOutput.compareAndSet(
                false,
                true
            ) && startsWithClearScreen(text)
        ) {
            LOG.trace("Clear Screen request detected at the beginning of the output, scheduling a scroll command.")
            // Windows ConPTY generates the 'clear screen' escape sequence (ESC[2J) optionally preceded by a "make cursor invisible" (ESC?25l) before the process output.
            // It pushes the already printed command line into the scrollback buffer which is not displayed by default.
            // In such cases, let's scroll up to display the printed command line.
            val verticalScrollModel: BoundedRangeModel = terminalPanel.verticalScrollModel
            verticalScrollModel.addChangeListener(object : ChangeListener {
                override fun stateChanged(e: ChangeEvent) {
                    verticalScrollModel.removeChangeListener(this)
                    UiNotifyConnector.doWhenFirstShown(
                        terminalPanel
                    ) {
                        terminalPanel.scrollToShowAllOutput()
                    }
                }
            })
        }
        dataStream.append(text)

        if (foregroundColor != null) {
            dataStream.append(Char(Ascii.ESC.toUShort()).toString() + "[39m") //restore default foreground color
        }
//        myContentHelper.onContentTypePrinted(
//            text,
//            ObjectUtils.notNull<ConsoleViewContentType>(contentType, ConsoleViewContentType.NORMAL_OUTPUT)
//        )
    }

    override fun clear() {
        lastCR = false
        terminalPanel.clearBuffer()

    }

    override fun scrollTo(offset: Int) {

    }

    override fun attachToProcess(processHandler: ProcessHandler) {

    }

    override fun setOutputPaused(value: Boolean) {

    }

    override fun isOutputPaused(): Boolean = false

    override fun hasDeferredOutput(): Boolean = false
    override fun performWhenNoDeferredOutput(runnable: Runnable) {

    }

    override fun setHelpId(helpId: String) {

    }

    override fun addMessageFilter(filter: Filter) {
        component.addMessageFilter(filter)

    }

    override fun printHyperlink(hyperlinkText: String, info: HyperlinkInfo?) {

    }

    override fun getContentSize(): Int = 0

    override fun canPause(): Boolean = false

    override fun createConsoleActions(): Array<AnAction> {
        return arrayOf(ScrollToTheEndAction(), ClearAction())

    }

    override fun allowHeavyFilters() {

    }

    private inner class ScrollToTheEndAction : DumbAwareAction(
        ActionsBundle.messagePointer("action.EditorConsoleScrollToTheEnd.text"),
        ActionsBundle.messagePointer("action.EditorConsoleScrollToTheEnd.text")
    ) {
        override fun update(e: AnActionEvent) {
            val verticalScrollModel: BoundedRangeModel = terminalPanel.verticalScrollModel
            e.presentation.isEnabled = verticalScrollModel.value != 0

        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT

        }

        override fun actionPerformed(e: AnActionEvent) {
            terminalPanel.verticalScrollModel.value = 0

        }
    }

    private inner class ClearAction : DumbAwareAction(
        ExecutionBundle.messagePointer("clear.all.from.console.action.name"),
        ExecutionBundle.messagePointer("clear.all.from.console.action.text"),
        AllIcons.Actions.GC
    ) {
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = true

        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT

        }

        override fun actionPerformed(e: AnActionEvent) {
            clear()

        }
    }

}
