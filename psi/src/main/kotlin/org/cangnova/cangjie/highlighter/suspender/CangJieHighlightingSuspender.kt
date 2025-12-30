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

package org.cangnova.cangjie.highlighter.suspender


import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import com.intellij.util.Alarm
import org.cangnova.cangjie.CangJiePluginDisposable
import java.util.concurrent.TimeUnit

/**
 * 控制编辑器的语法高亮暂停
 */
@Service(Service.Level.PROJECT)

class CangJieHighlightingSuspender(private val project: Project) {

    private val timeoutSeconds = Registry.intValue("cangjie.highlighting.suspended.timeout", 10)

    private val lastThrownExceptionTimestampPerFile = mutableMapOf<VirtualFile, Long>()
    private val suspendTimeoutMs = TimeUnit.SECONDS.toMillis(timeoutSeconds.toLong())
    private val updateQueue = Alarm(CangJiePluginDisposable.getInstance(project))

    private fun cleanup() {
        val timestamp = System.currentTimeMillis()

        val filesToUpdate = mutableListOf<VirtualFile>()
        val filesToUpdateLater = mutableListOf<VirtualFile>()
        synchronized(lastThrownExceptionTimestampPerFile) {
            if (lastThrownExceptionTimestampPerFile.isEmpty()) return

            val it = lastThrownExceptionTimestampPerFile.entries.iterator()
            while (it.hasNext()) {
                val next = it.next()
                if (timestamp - next.value > suspendTimeoutMs) {
                    filesToUpdate += next.key
                    it.remove()
                }
            }
            filesToUpdateLater.addAll(lastThrownExceptionTimestampPerFile.keys)
        }

        updateQueue.cancelAllRequests()
        filesToUpdate.forEach(::updateNotifications)
        filesToUpdateLater.forEach(::scheduleUpdate)
    }

    private fun scheduleUpdate(file: VirtualFile) {
        updateQueue.apply { addRequest(Runnable { updateNotifications(file) }, suspendTimeoutMs + 1) }
    }

    private fun updateNotifications(file: VirtualFile) {
        EditorNotifications.getInstance(project).updateNotifications(file)
    }

    /**
     * @return true, when file is suspended for the 1st time (within a timeout window)
     */
    fun suspend(file: VirtualFile): Boolean {
        cleanup()

        if (suspendTimeoutMs <= 0) return false

        val timestamp = System.currentTimeMillis()
        // daemon is restarted when exception is thrown
        // if there is a recurred error (e.g. within a resolve) it could lead to infinite highlighting loop
        // so, do not rethrow exception too often to disable HL for a while
        val lastThrownExceptionTimestamp = synchronized(lastThrownExceptionTimestampPerFile) {
            val lastThrownExceptionTimestamp = lastThrownExceptionTimestampPerFile[file] ?: run {
                lastThrownExceptionTimestampPerFile[file] = timestamp
                0L
            }
            lastThrownExceptionTimestamp
        }

        scheduleUpdate(file)
        updateNotifications(file)

        return timestamp - lastThrownExceptionTimestamp > suspendTimeoutMs
    }


    companion object {
        fun getInstance(project: Project): CangJieHighlightingSuspender = project.service()
    }
}
