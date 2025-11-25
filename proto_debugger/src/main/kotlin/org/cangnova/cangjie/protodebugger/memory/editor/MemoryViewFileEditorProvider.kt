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

package org.cangnova.cangjie.protodebugger.memory.editor
//
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.fileEditor.impl.text.TextEditorComponent
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.pom.Navigatable
import kotlinx.coroutines.launch
import org.cangnova.cangjie.protodebugger.memory.Address
import org.cangnova.cangjie.protodebugger.memory.AddressRange
import org.cangnova.cangjie.protodebugger.memory.MemoryCell
import org.cangnova.cangjie.protodebugger.memory.MemoryViewFacade
import org.cangnova.cangjie.protodebugger.memory.vfs.MemoryViewFile
import org.cangnova.cangjie.protodebugger.memory.vfs.MemoryViewVirtualFileSystem
import java.awt.Point
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 准确计算编辑器中可见的文本行数
 *
 * 使用IntelliJ的坐标转换API来获取准确的可见行范围，
 * 避免了直接除法带来的精度损失问题
 */
private val Editor.visibleLineCount: Int
    get() {
        val visibleArea = scrollingModel.visibleArea

        // 获取可见区域的起始和结束点
        val startPoint = Point(visibleArea.x, visibleArea.y)
        val endPoint = Point(visibleArea.x, visibleArea.y + visibleArea.height)

        // 转换为逻辑位置（行列）
        val startLine = xyToLogicalPosition(startPoint).line
        val endLine = xyToLogicalPosition(endPoint).line

        // 返回实际可见的行数，确保至少返回1
        return maxOf(1, endLine - startLine + 1)
    }

/**
 * 内存视图文件编辑器提供者
 *
 * 创建直接绑定到 MemoryViewFacade.baseDocument 的编辑器，
 * 实现内存视图的实时显示和滚动加载。
 */
//class MemoryViewFileEditorProvider : PsiAwareTextEditorProvider(), DumbAware {
//
//    companion object {
//        const val MEMORY_VIEW_EDITOR_ID = "CangJieMemoryViewEditor"
//    }
//
//    override fun accept(project: Project, file: VirtualFile): Boolean {
//        return file.fileSystem is MemoryViewVirtualFileSystem && super.accept(project,file)
//    }
//
//    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
//        val memoryFile = file as? MemoryViewFile<out MemoryCell>
//            ?: return createEmptyEditor(file)
//
//
//        return MemoryViewFileEditor(project, memoryFile, this, facade = file.store.facade)
//    }
//
//    private fun createEmptyEditor(file: VirtualFile): FileEditor {
//        return object : FileEditor {
//            private val propertyChangeSupport = PropertyChangeSupport(this)
//
//            override fun getComponent(): JComponent = JPanel()
//            override fun getPreferredFocusedComponent(): JComponent? = null
//            override fun getName(): String = "Empty Memory View"
//            override fun setState(state: FileEditorState) {}
//            override fun isModified(): Boolean = false
//            override fun isValid(): Boolean = true
//            override fun addPropertyChangeListener(listener: PropertyChangeListener) {
//                propertyChangeSupport.addPropertyChangeListener(listener)
//            }
//
//            override fun removePropertyChangeListener(listener: PropertyChangeListener) {
//                propertyChangeSupport.removePropertyChangeListener(listener)
//            }
//
//            override fun dispose() {}
//            override fun getFile(): VirtualFile = file
//            override fun <T : Any?> getUserData(key: Key<T?>): T? {
//                return null
//            }
//
//            override fun <T : Any?> putUserData(key: Key<T?>, value: T?) {
//
//            }
//        }
//    }
//
//    override fun getEditorTypeId(): String = MEMORY_VIEW_EDITOR_ID
//
//    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
//}
//
///**
// * 内存视图文件编辑器
// *
// * 直接绑定到 MemoryViewFacade 的 baseDocument，
// * 支持滚动时动态加载内存数据。
// */
class MemoryViewFileEditor<T : MemoryCell>(
    project: Project,
    file: MemoryViewFile<T>,
    provider: TextEditorProvider,
    private val facade: MemoryViewFacade
) : PsiAwareTextEditorImpl(project, file, provider) {

    val store = file.store




    init {
        // 应用编辑器配置
        buildEditorPipeline().applyTo(editor)
        if (!file.store.config.vfsDisableLazyLoad) {
            // 监听滚动事件，触发内存加载
            editor.scrollingModel.addVisibleAreaListener(ScrollLoadListener())
        }

    }


    /**
     * 配置编辑器
     */
    private fun configureEditor() {
        val settings = editor.settings

        // 显示设置
        settings.isLineNumbersShown = true
        settings.isWhitespacesShown = false
        settings.isLineMarkerAreaShown = true
        settings.isFoldingOutlineShown = false
        settings.isRightMarginShown = false
        settings.isCaretRowShown = true

        // 编辑设置
        settings.isVirtualSpace = false
        settings.isBlockCursor = true
        settings.isBlinkCaret = true

        // 字体设置（等宽字体）
        editor.colorsScheme.editorFontName = "Monospaced"
        editor.colorsScheme.editorFontSize = 12
    }

    /**
     * 构建编辑器配置管道
     */
    private fun buildEditorPipeline(): EditorPipeline {
        configureEditor()
        return EditorPipeline()
            .with(BasicEditorSettingsConfigurator())
            .with(AddressLineNumberConfigurator(store))
    }

    private inner class ScrollLoadListener : VisibleAreaListener {

        override fun visibleAreaChanged(e: VisibleAreaEvent) {
            val oldRectangle = e.oldRectangle
            val newRectangle = e.newRectangle

            if (oldRectangle != null) {
                // 获取当前可见区域的起始和结束行
                val visibleStartLine = editor.xyToLogicalPosition(
                    Point(newRectangle.x, newRectangle.y)
                ).line

                val visibleEndLine = editor.xyToLogicalPosition(
                    Point(newRectangle.x, newRectangle.y + newRectangle.height)
                ).line


                val startAddress = store.getAddressForLine(visibleStartLine - 1)
                val endAddress = store.getAddressForLine(visibleEndLine - 1)
                if (newRectangle.y > oldRectangle.y) {

                    if (endAddress == null && startAddress != null) {
//    加载高位
//                最后一个地址
                        val lastLine = store.getState().matrix.getLastCell() ?: return
                        facade.debuggerFacade.executeCommand {
                            store.loadRange(

                                AddressRange.of(
                                    lastLine.address + 1,
                                    lastLine.address + 1 + 512
                                )
                            )
                        }

                        return
                    }
                }

                if (newRectangle.y < oldRectangle.y) {
                    if (visibleStartLine < 50 && startAddress != null) {
                        val frist = store.getState().matrix.getFristCell() ?: return
                       facade.debuggerFacade.executeCommand {
                            store.loadRange(
                                AddressRange.of(
                                    frist.address - 512,
                                    frist.address - 1
                                )
                            )
                        }
                        return
                    }
                }
            }
        }

    }


    override fun getName(): String = "Memory View"


    override fun dispose() {
        // Disposer 会处理 editor 的释放
    }


}


/**
 * 内存视图文件编辑器提供者
 *
 * 创建直接绑定到 MemoryViewFacade.baseDocument 的编辑器，
 * 实现内存视图的实时显示和滚动加载。
 *
 * 不知出于何种原因，只能继承FileEditorProvider，继承其他FileEditorProvider子类不起效果
 */
class MemoryViewFileEditorProvider : FileEditorProvider, DumbAware {

    companion object {
        const val MEMORY_VIEW_EDITOR_ID = "MemoryViewEditor"
    }

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileSystem is MemoryViewVirtualFileSystem
    }

    object CangJieMemoryViewFileEditorProvider : PsiAwareTextEditorProvider()

    private val delegate: TextEditorProvider = CangJieMemoryViewFileEditorProvider

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val memoryFile = file as? MemoryViewFile<out MemoryCell>
            ?: return createEmptyEditor(file)


        return MemoryViewFileEditor(project, memoryFile, delegate, facade = file.store.facade)
    }

    private fun createEmptyEditor(file: VirtualFile): FileEditor {
        return object : FileEditor {
            private val propertyChangeSupport = PropertyChangeSupport(this)

            override fun getComponent(): JComponent = JPanel()
            override fun getPreferredFocusedComponent(): JComponent? = null
            override fun getName(): String = "Empty Memory View"
            override fun setState(state: FileEditorState) {}
            override fun isModified(): Boolean = false
            override fun isValid(): Boolean = true
            override fun addPropertyChangeListener(listener: PropertyChangeListener) {
                propertyChangeSupport.addPropertyChangeListener(listener)
            }

            override fun removePropertyChangeListener(listener: PropertyChangeListener) {
                propertyChangeSupport.removePropertyChangeListener(listener)
            }

            override fun dispose() {}
            override fun getFile(): VirtualFile = file
            override fun <T : Any?> getUserData(key: Key<T?>): T? {
                return null
            }

            override fun <T : Any?> putUserData(key: Key<T?>, value: T?) {

            }
        }
    }

    override fun getEditorTypeId(): String = MEMORY_VIEW_EDITOR_ID

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}
