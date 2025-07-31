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

package cn.cangnova.cangjie.notifications

import com.intellij.ide.plugins.PluginManagerCore.isUnitTestMode
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService
import com.intellij.openapi.util.NlsContexts
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference


@Service(Service.Level.PROJECT)
class CompileDriverNotifications(
    private val project: Project
) : Disposable {
    private val currentNotification = AtomicReference<Notification>(null)

    companion object {
        @JvmStatic
        fun getInstance(project: Project) = project.service<CompileDriverNotifications>()
    }

    override fun dispose() {

        currentNotification.set(null)
    }

    fun createCannotStartNotification() : LightNotification {
        return LightNotification()
    }

    inner class LightNotification {
        private val isShown = AtomicBoolean()
        private val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("CangJie Build configuration error")

        private val baseNotification = notificationGroup
            .createNotification(CangJieCompilerBundle.message("notification.title.jps.cannot.start.compiler"), NotificationType.ERROR)
            .setImportant(true)

        fun withExpiringAction(@NlsContexts.NotificationContent title : String,
                               handler: () -> Unit) = apply {
            baseNotification.addAction(NotificationAction.createSimpleExpiring(title, handler))
        }

        @JvmOverloads
        fun withOpenSettingsAction(moduleNameToSelect: String? = null, tabNameToSelect: String? = null) =
            withExpiringAction(CangJieCompilerBundle.message("notification.action.jps.open.configuration.dialog")) {
                val service = ProjectSettingsService.getInstance(project)
                if (moduleNameToSelect != null) {
                    service.showModuleConfigurationDialog(moduleNameToSelect, tabNameToSelect)
                }
                else {
                    service.openProjectSettings()
                }
            }

        fun withContent(@NlsContexts.NotificationContent content: String): LightNotification = apply {
            baseNotification.setContent(content)
        }

        /**
         * This wrapper helps to make sure we have only one active unresolved notification per project
         */
        fun showNotification() {
            if (isUnitTestMode) {
                thisLogger().error("" + baseNotification.content)
                return
            }

            if (!isShown.compareAndSet(false, true)) return

            val showNotification = Runnable {
                baseNotification.whenExpired {
                    currentNotification.compareAndExchange(baseNotification, null)
                }

                currentNotification.getAndSet(baseNotification)?.expire()
                baseNotification.notify(project)
            }

            showNotification.run()
        }
    }
}
