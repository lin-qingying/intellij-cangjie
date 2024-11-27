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

package com.linqingying.lsp.impl.lsWidget

import com.intellij.lang.LangBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.DumbAware
import com.intellij.platform.lang.lsWidget.impl.fus.LanguageServiceWidgetActionKind
import com.intellij.platform.lang.lsWidget.impl.fus.LanguageServiceWidgetUsagesCollector
//import com.intellij.platform.lang.lsWidget.impl.fus.LanguageServiceWidgetActionKind
//import com.intellij.platform.lang.lsWidget.impl.fus.LanguageServiceWidgetUsagesCollector
import com.intellij.testFramework.LightVirtualFile
import com.linqingying.lsp.api.LspBundle
import com.linqingying.lsp.api.LspServer
import com.linqingying.lsp.api.lsWidget.LspWidgetInternalService
import com.linqingying.lsp.impl.LspServerImpl
import com.linqingying.lsp.impl.LspServerManagerImpl

private class LspWidgetInternalServiceImpl :
    LspWidgetInternalService() {
    override fun createShowErrorOutputAction(lspServer: LspServer): AnAction? {
        return if ((lspServer as LspServerImpl).errorOutput == null) null else (OpenLspErrorOutputAction(
            lspServer
        ))

    }

    override fun restartLspServer(lspServer: LspServer) {
//        LanguageServiceWidgetUsagesCollector.actionInvoked(
//            lspServer.project,
//            LanguageServiceWidgetActionKind.RestartService,
//            lspServer.descriptor::class.java
//        )

        val serverManager = LspServerManagerImpl.getInstanceImpl(lspServer.project)

        serverManager.stopAndRestartIfNeeded(lspServer.providerClass)

        val notificationTitle = LspBundle.message(
            "language.services.0.server.restarted.notification.title",
            lspServer.descriptor.presentableName
        )
        NotificationGroupManager.getInstance()
            .getNotificationGroup("lsp.service.stopped.or.restarted")
            .createNotification(notificationTitle, "", NotificationType.INFORMATION)
            .notify(lspServer.project)
    }

    override fun stopLspServer(lspServer: LspServer) {
//        LanguageServiceWidgetUsagesCollector.actionInvoked(
//            lspServer.project,
//            LanguageServiceWidgetActionKind.StopService,
//            lspServer.descriptor::class.java
//        )

        LspServerManagerImpl.getInstanceImpl(lspServer.project).stopRunningServer(lspServer as LspServerImpl)

        val notificationTitle = LangBundle.message(
            "language.services.0.server.stopped.notification.title",
            lspServer.descriptor.presentableName
        )

        val notificationContent = LspBundle.message("language.services.server.stopped.notification.content")

        NotificationGroupManager.getInstance()
            .getNotificationGroup("lsp.service.stopped.or.restarted")
            .createNotification(notificationTitle, notificationContent, NotificationType.INFORMATION)
            .notify(lspServer.project)
    }
}

private class OpenLspErrorOutputAction(val lspServer: LspServerImpl) :
    AnAction(), DumbAware {


    override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
        val lspServerDescriptor = lspServer.descriptor.presentableName
        val tabName = LspBundle.message("server.error.output.editor.tab.name", lspServerDescriptor)

        val errorOutput = lspServer.errorOutput
        val virtualFile = LightVirtualFile(tabName, PlainTextFileType.INSTANCE, errorOutput as CharSequence)

        FileEditorManager.getInstance(lspServer.project).openTextEditor(
            OpenFileDescriptor(lspServer.project, virtualFile), true
        )
    }
}

