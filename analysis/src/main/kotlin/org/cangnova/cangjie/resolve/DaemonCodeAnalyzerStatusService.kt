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

package org.cangnova.cangjie.resolve

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)

class DaemonCodeAnalyzerStatusService(project: Project) : Disposable {
    companion object {
        fun getInstance(project: Project): DaemonCodeAnalyzerStatusService = project.service()
    }

    @Volatile
    var daemonRunning: Boolean = false
        private set

    init {
        val messageBusConnection = project.messageBus.connect(this)
        messageBusConnection.subscribe(
            DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC,
            object : DaemonCodeAnalyzer.DaemonListener {
                override fun daemonStarting(fileEditors: Collection<FileEditor>) {
                    daemonRunning = true
                }

                override fun daemonFinished(fileEditors: Collection<FileEditor>) {
                    daemonRunning = false
                }

                override fun daemonCancelEventOccurred(reason: String) {
                    daemonRunning = false
                }
            })
    }

    override fun dispose() {}

}