package com.linqingying.lsp.impl.lsWidget

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lang.lsWidget.LanguageServiceWidgetItem
import com.intellij.platform.lang.lsWidget.LanguageServiceWidgetItemsProvider
import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.LspServerManager
import com.linqingying.lsp.api.LspServerManagerListener
import com.linqingying.lsp.api.LspServerSupportProvider
import kotlin.jvm.internal.Intrinsics


internal class LspWidgetItemsProvider :
    LanguageServiceWidgetItemsProvider() {
    override fun createWidgetItems(
        project: Project,
        currentFile: VirtualFile?
    ): List<LanguageServiceWidgetItem> {
        val widgetItems = mutableListOf<LanguageServiceWidgetItem>()
        val extensions = LspServerSupportProvider.EP_NAME.extensionList

        for (provider in extensions) {
            val items = provider.createLspWidgetItems(project, currentFile)
            widgetItems.addAll(items)
        }

        return widgetItems
    }

    override fun registerWidgetUpdaters(
        project: Project,
        widgetDisposable: Disposable,
        updateWidget: () -> Unit
    ) {
        LspServerManager.getInstance(project)
            .addLspServerManagerListener((object : LspServerManagerListener {
                override fun serverStateChanged(lspServer: LspServer) {

                    updateWidget.invoke()
                }
            }) as LspServerManagerListener, widgetDisposable, false)
    }
}

