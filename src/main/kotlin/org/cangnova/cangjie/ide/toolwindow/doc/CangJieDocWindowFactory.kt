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

package org.cangnova.cangjie.ide.toolwindow.doc

import org.cangnova.cangjie.messages.CangJieBundle
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import java.net.URL
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

class CangJieDocWindowFactory : ToolWindowFactory, DumbAware {

    @Volatile
    private var urls: JsonObject? = null
    private val versionRef = AtomicReference("11111") // 用 AtomicReference 来保证线程安全
    private lateinit var browser: JBCefBrowser
    private lateinit var panel: DialogPanel

    /**
     * 下载文档索引并初始化 UI
     */
    private fun downloadIndex(onLoaded: () -> Unit) {
        Thread {
            try {
                val indexJson = URL(DOC_INDEX_URL).readText()
                urls = JsonParser.parseString(indexJson).asJsonObject
//                versionRef.set(urls?.keySet()?.firstOrNull() ?: "")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            SwingUtilities.invokeLater {
                onLoaded() // 通知 UI 线程更新
            }
        }.start()
    }

    /**
     * 构建 UI 面板
     */
    private fun createPanel(): DialogPanel {
        browser = JBCefBrowser(getDocUrl(versionRef.get()))

        val versions = urls?.keySet()?.toMutableSet() ?: mutableSetOf()

        return panel {
            row(CangJieBundle.message("cangjie.doc.version")) {
                val comboBox = comboBox(versions)
                    .bindItem(
                        { versionRef.get() },
                        {
                            if (it != null && it != versionRef.get()) {
                                versionRef.set(it)
                                browser.loadURL(getDocUrl(it)) // 更新 JCEF URL
                            }
                        }
                    ).component

                // 让 comboBox 在数据加载后刷新
                if (urls != null) {
                    comboBox.removeAllItems()
                    urls!!.keySet().forEach { comboBox.addItem(it) }
                    comboBox.selectedItem = versionRef.get()
                }
            }
            row {
                cell(browser.component)
            }
        }
    }

    /**
     * 获取文档 URL
     */
    private fun getDocUrl(version: String): String {
        return urls?.get(version)?.asString ?: ""
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        if (!JBCefApp.isSupported()) {
            return
        }

        downloadIndex {
            panel = createPanel() // `urls` 加载完成后，创建 UI
            val content = ContentFactory.getInstance().createContent(panel, "Web View", false)
            toolWindow.contentManager.addContent(content)
        }
    }
}

