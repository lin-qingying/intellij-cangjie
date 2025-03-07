package cn.cangnova.cangjie.toolwindow.doc

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
import cn.cangnova.cangjie.CangJieBundle
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

