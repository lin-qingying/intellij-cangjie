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

package com.linqingying.cangjie.ide.stubindex.resolve

import com.linqingying.cangjie.analyzer.CangJieBaseAnalysisBundle
import com.intellij.ide.actions.cache.ProjectRecoveryScope
import com.intellij.ide.actions.cache.Saul
import com.intellij.idea.ActionsBundle
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IntellijInternalApi
import java.util.concurrent.atomic.AtomicBoolean



@OptIn(IntellijInternalApi::class)
internal class CangJieRepairIdeAfterCorruptionNotification(private val project: Project) : CangJieCorruptedIndexListener {
    private val pendingNotificationFlag = AtomicBoolean(false)

    override fun corruptionDetected() {
        if (pendingNotificationFlag.get()) return

        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Recover CangJie Indices")
            .createNotification(CangJieBaseAnalysisBundle.message("cangjie.indices.corrupted"), NotificationType.ERROR)
            .setSuggestionType(true)
            .setImportantSuggestion(true)
            .addAction(
                NotificationAction.createSimpleExpiring(
                    ActionsBundle.message("action.CallSaul.text"),
                    CangJieRepairIdeAction(project)
                )
            )
            .whenExpired {
                pendingNotificationFlag.set(false)
            }

        if (pendingNotificationFlag.compareAndSet(/* expectedValue = */ false, /* newValue = */ true)) {
            notification.notify(project)
        }
    }
}

private class CangJieRepairIdeAction(private val project: Project) : Runnable {
    override fun run() {
        service<Saul>().sortThingsOut(ProjectRecoveryScope(project))
    }
}
