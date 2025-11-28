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

package org.cangnova.cangjie.protodebugger.core

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.ColoredText
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.TextTransferable
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueModifier
import org.cangnova.cangjie.messages.DebuggerBundle
import org.cangnova.cangjie.protodebugger.data.LLDBFrame
import org.cangnova.cangjie.protodebugger.data.LLDBThread
import org.cangnova.cangjie.protodebugger.data.LLDBVariable
import org.cangnova.cangjie.protodebugger.settings.DebuggerSettings
import org.cangnova.cangjie.psi.*

fun LLDBFrame.toCangJieStackFrame(
    thread: LLDBThread,
    facade: DebuggerDriverFacade
): CangJieStackFrame {
    return CangJieStackFrame(
        frame = this,
        thread = thread,
        facade = facade
    )
}

/**
 * 仓颉调试器栈帧
 *
 * 表示调用栈中的一个帧，包含函数名、源码位置、局部变量等信息。
 *
 * 维护两个位置：
 * - sourcePosition: 源代码位置（如果源文件可用）
 * - disassemblyPosition: 反汇编位置（基于程序计数器，总是可用）
 *
 * @param frame LLDB 栈帧数据
 * @param thread 关联的执行线程
 * @param facade 调试器驱动门面
 */
class CangJieStackFrame(
    val frame: LLDBFrame,
    private val thread: LLDBThread,
    private val facade: DebuggerDriverFacade
) : XStackFrame() {
    companion object {
        val LOG = Logger.getInstance(CangJieStackFrame::class.java)
    }

    /**
     * 反汇编位置（延迟初始化）
     *
     * 基于程序计数器（PC）创建的反汇编视图位置，总是可用的。
     */
    val disassemblyPosition: XSourcePosition? by lazy {
        createDisassemblyPosition()
    }

    /**
     * 获取源码位置
     *
     * 优先返回源代码位置，如果源文件不可用则返回 null。
     * 反汇编位置通过 getDisassemblyPosition() 单独获取。
     *
     * @return 源码位置对象，如果无法确定源码位置则返回null
     */
    override fun getSourcePosition(): XSourcePosition? {
        return try {
            val filePath = frame.file ?: return null
            if (filePath.isEmpty()) return null
            val line = frame.line

            // 尝试创建源代码位置
            facade.createSourcePosition(filePath, frame.hash, line)
        } catch (e: Exception) {
            LOG.error("Failed to get source position for frame: ${frame.function}", e)
            null
        }
    }


    /**
     * 创建反汇编位置
     *
     * 使用 MemoryViewFacade.createAddressPosition() 创建基于地址的源位置。
     */
    private fun createDisassemblyPosition(): XSourcePosition? {
        return try {


            facade.memoryViewFacade.getDisasmStore().createAddressPosition(frame.programCounter)
        } catch (e: Exception) {
            LOG.error("Failed to create disassembly position for frame: ${frame.function}", e)
            null
        }
    }

    /**
     * 获取变量列表
     *
     * @param node 变量节点的容器，用于添加局部变量和参数
     */
    override fun computeChildren(node: XCompositeNode) {
        facade.executeCommand {
            try {
                val children = XValueChildrenList()

                // 从 DebuggerSettings 获取变量过滤配置
                val settings = org.cangnova.cangjie.protodebugger.settings.DebuggerSettings.getInstance()

                // 获取栈帧的变量，使用用户配置的过滤选项
                val variables = facade.evalService.getVariables(
                    thread,
                    frame,
                    includeArgument = settings.isIncludeArguments(),
                    includeStatics = settings.isIncludeStatics(),
                    includeLocals = settings.isIncludeLocals(),
                    inScopeOnly = settings.isInScopeOnly(),
                    includeRuntimeSupportValues = settings.isIncludeRuntimeSupportValues(),
                    useDynamic = settings.getUseDynamic(),
                    includeRecognizedArguments = settings.isIncludeRecognizedArguments()
                )

                // 将变量转换为 XValue 并添加到列表
                variables.forEach { variable ->
                    children.add(
                        variable.toCangJieValue(
                            threadId = thread.id,
                            frameIndex = frame.index,
                            facade = facade
                        )
                    )
                }

                // 添加所有子节点
                node.addChildren(children, true)
            } catch (e: Exception) {
                node.setErrorMessage("Failed to get variables: ${e.message}")
            }
        }
    }

    fun getFrameFunctionColoredText(

        attributes: SimpleTextAttributes,
        renderForUiLabel: Boolean = false
    ): ColoredText {
        return ColoredText.singleFragment(frame.function, attributes)

    }

    /**
     * 自定义栈帧显示
     *
     * @param container 用于自定义显示的容器
     */
    override fun customizePresentation(component: ColoredTextContainer) {
        val position = sourcePosition
        val functionNameAttributes =
            if (position != null) SimpleTextAttributes.REGULAR_ATTRIBUTES else SimpleTextAttributes.GRAYED_ATTRIBUTES
        val unknownSymbolAttributes =
            if (position != null) SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES else SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES

        val showModule = DebuggerSettings.getInstance().isShowFrameModuleName()
        val module = if (showModule) frame.module else null
        if (module != null) {
            component.append("[$module] ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }

        if (frame.hasSymbolInfo()) {


            val isPlainTextRendering = component is TextTransferable.ColoredStringBuilder
            val coloredText =
                getFrameFunctionColoredText(functionNameAttributes, !isPlainTextRendering)
            component.append(coloredText)

        } else {
            component.append(DebuggerBundle.message("debug.frames.unknownFunction"), unknownSymbolAttributes)
        }

        if (position != null) {
            component.append(" ${position.file.name}:${position.line + 1}", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES)
        } else {
            component.append(" ${frame.programCounter}", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES)
        }
    }

    override fun getEvaluator(): XDebuggerEvaluator {
        return CangJieDebuggerEvaluator(thread, frame, facade)
    }

}


/**
 * 仓颉调试器变量值修改器
 *
 * 用于在调试时修改变量的值
 *
 * @param variable 变量对象
 * @param facade 调试器驱动门面
 */
class CangJieValueModifier(
    private val variable: LLDBVariable,
    private val facade: DebuggerDriverFacade
) : XValueModifier() {

    override fun setValue(expression: XExpression, callback: XModificationCallback) {
        facade.executeCommandAsync {
            try {
                // 发送请求并处理响应
                val success = facade.evalService.setValue(variable, expression.expression)

                if (success) {
                    callback.valueModified()
                } else {
                    callback.errorOccurred("Failed to set variable value")
                }

            } catch (e: Exception) {
                // 处理异常
                callback.errorOccurred("Error setting variable value: ${e.message}")
            }
        }
    }

}

/**
 * 假设的 SessionService 接口扩展
 * 这些方法需要在实际的 SessionService 中实现
 */
/**
 * 仓颉调试器表达式求值器
 *
 * 负责在调试会话中求值表达式，支持变量查看、计算表达式等功能。
 */
class CangJieDebuggerEvaluator(
    private val thread: LLDBThread,
    private val frame: LLDBFrame,
    private val facade: DebuggerDriverFacade
) : XDebuggerEvaluator() {

    companion object {
        private val LOG = Logger.getInstance(CangJieDebuggerEvaluator::class.java)
    }

    override fun evaluate(
        expression: String,
        callback: XEvaluationCallback,
        expressionPosition: XSourcePosition?
    ) {


        LOG.debug("Evaluating expression: '$expression' in frame ${frame.index} of thread ${thread.id}")

        facade.executeCommandAsync {
            try {
                // 调用调试器服务求值表达式
                val result = facade.evalService.evaluate(
                    thread = thread,
                    frame = frame,
                    expression = expression
                )


                // 创建成功的结果
                val xValue = result.toCangJieValue(
                    threadId = thread.id,
                    frameIndex = frame.index,
                    facade = facade
                )
                callback.evaluated(xValue)
                LOG.debug("Expression evaluated successfully: '$expression'")

            } catch (e: Exception) {
                val errorMessage = "Failed to evaluate expression: ${e.message}"
                callback.errorOccurred(errorMessage)
                LOG.error("Error evaluating expression: '$expression'", e)
            }
        }
    }

    /**
     * 获取在指定位置可以被求值的表达式范围
     *
     * 这个方法对于实现鼠标悬浮显示变量值的功能至关重要。
     * 当用户将鼠标悬浮在代码上时，IDE会调用此方法来确定哪个表达式应该被求值。
     *
     * @param project 当前项目
     * @param document 文档对象
     * @param offset 光标在文档中的偏移量
     * @param sideEffectsAllowed 是否允许有副作用的表达式（如函数调用）
     * @return 可以被求值的表达式的文本范围，如果没有有效表达式则返回null
     */
    override fun getExpressionRangeAtOffset(
        project: Project,
        document: Document,
        offset: Int,
        sideEffectsAllowed: Boolean
    ): TextRange? {
        // 获取PSI文件
        val psiFile =  PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return null

        // 查找偏移量处的元素
        val element = psiFile.findElementAt(offset) ?: return null

        // 查找包含该元素的表达式
        val expression = findEvaluatableExpression(element, sideEffectsAllowed)
        if (expression != null) {
            return expression.textRange
        }

        return null
    }

    /**
     * 查找可以被求值的表达式
     *
     * 从给定的PSI元素开始，向上遍历PSI树，查找第一个可以被求值的表达式。
     * 支持：
     * - 简单变量引用（如 `x`）
     * - 属性访问（如 `obj.field`）
     * - 数组访问（如 `arr[0]`）
     * - 函数调用（如 `func()`）- 仅在 sideEffectsAllowed 为 true 时
     *
     * @param element PSI元素
     * @param sideEffectsAllowed 是否允许有副作用的表达式（如函数调用）
     * @return 可求值的表达式，如果没有则返回null
     */
    private fun findEvaluatableExpression(
        element: PsiElement,
        sideEffectsAllowed: Boolean
    ):  PsiElement? {
        var current:  PsiElement? = element

        while (current != null) {
            // 检查是否是可以求值的表达式类型
            when (current) {
                // 简单变量名 - 总是允许
                is CjSimpleNameExpression -> return current

                // 引用表达式（属性访问）- 总是允许
                is CjReferenceExpression -> {
                    // 排除函数调用（函数调用也是引用表达式）
                    if (current !is CjCallExpression) {
                        return current
                    }
                }

                // 函数调用 - 仅在允许副作用时
                is  CjCallExpression -> {
                    if (sideEffectsAllowed) {
                        return current
                    }
                }

                // 数组访问 - 总是允许
                is CjArrayAccessExpression -> return current

                // 二元表达式（加减乘除等）- 总是允许
                is CjBinaryExpression -> return current

                // 常量表达式（字面量）- 总是允许
                is CjConstantExpression -> return current

                // 如果遇到语句级别的元素，停止向上查找
                is CjBlockExpression,
                is CjDeclaration -> return null
            }

            current = current.parent
        }

        return null
    }

    /**
     * 返回求值模式
     *
     * CODE_FRAGMENT 模式允许更灵活的表达式求值，支持更复杂的表达式
     */
    override fun getEvaluationMode(text: String, startOffset: Int, endOffset: Int, psiFile: PsiFile?): EvaluationMode {
        return EvaluationMode.CODE_FRAGMENT
    }


}
