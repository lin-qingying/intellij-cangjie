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

import com.intellij.icons.AllIcons
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import org.cangnova.cangjie.protodebugger.data.LLDBRegister
import org.cangnova.cangjie.protodebugger.data.LLDBVariable
import org.cangnova.cangjie.protodebugger.data.ValueFormatter

/**
 * 仓颉调试器值抽象基类
 *
 * 表示调试器中的一个值的抽象概念，可以是变量、寄存器等。
 * 提供了值的通用接口和功能。
 */
abstract class CangJieValue(
    name: String
) : XNamedValue(name) {

    /**
     * 线程 ID（用于获取子元素）
     */
    protected abstract val threadId: Long

    /**
     * 栈帧索引（用于获取子元素）
     */
    protected abstract val frameIndex: Int

    /**
     * 调试器驱动门面
     */
    protected abstract val facade: DebuggerDriverFacade




    /**
     * 获取值的字符串表示
     *
     * @param node 值节点
     * @param place 值的位置
     */
    abstract override fun computePresentation(node: XValueNode, place: XValuePlace)

    /**
     * 计算子节点
     *
     * @param node 子节点容器
     */
    abstract override fun computeChildren(node: XCompositeNode)

    /**
     * 是否支持修改变量值
     *
     * @return 支持修改返回对应的修改器，不支持返回 null
     */
    abstract override fun getModifier(): XValueModifier?


    /**
     * 计算变量声明的源代码位置
     *
     * 当用户在调试器变量视图中对变量使用"跳转到声明"功能时调用。
     *
     * 注意：LLDB 通常不提供局部变量的声明位置信息，
     * 这个功能需要通过静态分析当前文件的 PSI 来实现。
     *
     * 当前实现：使用默认行为（不跳转），子类可以根据需要重写此方法。
     *
     * @param navigatable 导航回调接口，用于设置源代码位置
     */
    override fun computeSourcePosition(navigatable: XNavigatable) {
        // 默认不提供跳转功能
        // TODO: 可以通过 PSI 分析在当前栈帧的源文件中查找变量声明
        navigatable.setSourcePosition(null)
    }

    /**
     * 计算类型定义的源代码位置
     *
     * 当用户在调试器变量视图中对变量使用"跳转到类型定义"功能时调用。
     *
     * 注意：需要根据类型名在项目中查找类型定义，这需要：
     * 1. 解析类型名（可能包含泛型参数、命名空间等）
     * 2. 在项目索引中查找对应的类型定义
     * 3. 返回类型定义的源代码位置
     *
     * 当前实现：使用默认行为（不跳转），子类可以根据需要重写此方法。
     *
     * @param navigatable 导航回调接口，用于设置类型定义位置
     */
    override fun computeTypeSourcePosition(navigatable: XNavigatable) {
        // 默认不提供跳转功能
        // TODO: 可以通过类型名在项目中查找类型定义
        // 1. 获取类型名（去除泛型参数等）
        // 2. 使用项目索引查找类型定义
        // 3. 创建 XSourcePosition 并调用 navigatable.setSourcePosition()
        navigatable.setSourcePosition(null)
    }
}
fun LLDBVariable.toCangJieValue(
    threadId: Long,
    frameIndex: Int,
    facade: DebuggerDriverFacade
): CangJieVariableValue {
    return CangJieVariableValue(
        lldbVariable = this,
        threadId = threadId,
        frameIndex = frameIndex,
        facade = facade
    )
}

fun LLDBRegister.toCangJieRegisterValue(
    threadId: Long,
    frameIndex: Int,
    facade: DebuggerDriverFacade
): CangJieRegisterValue {
    return CangJieRegisterValue(
        lldbRegister = this,
        threadId = threadId,
        frameIndex = frameIndex,
        facade = facade
    )
}
/**
 * 普通变量值
 *
 * 表示调试器中的一个普通变量，基于 LLDBVariable。
 *
 * @param lldbVariable LLDB 变量数据
 * @param threadId 线程 ID
 * @param frameIndex 栈帧索引
 * @param facade 调试器驱动门面
 */
class CangJieVariableValue(
    private val lldbVariable: LLDBVariable,
    override val threadId: Long,
    override val frameIndex: Int,
    override val facade: DebuggerDriverFacade
) : CangJieValue(lldbVariable.name) {

    /**
     * 获取值的字符串表示
     *
     * @param node 值节点
     * @param place 值的位置
     */
    override fun computePresentation(node: XValueNode, place: XValuePlace) {

        facade.executeCommandAsync {
            val value = facade.evalService.getValue(lldbVariable)


            node.setPresentation(
                AllIcons.Debugger.Value,
                lldbVariable.type.typeName,
                value.getPresentableValue(),
                lldbVariable.hasChildren

            )
            node.setPresentation(
                AllIcons.Debugger.Value,
                object : XValuePresentation() {
                    override fun renderValue(renderer: XValuePresentation.XValueTextRenderer) {
                        renderer.renderValue(value.getPresentableValue())
                    }

                    override fun getType(): String = lldbVariable.type.typeName

                    override fun getSeparator(): String = " = "
                },
                lldbVariable.hasChildren
            )

        }


    }

    /**
     * 计算子节点
     *
     * @param node 子节点容器
     */
    override fun computeChildren(node: XCompositeNode) {
        facade.executeCommandAsync {
            val children = XValueChildrenList()

            val resule = facade.evalService.getVariableChildren(lldbVariable)
            resule.items.forEach {

                children.add(
                    it.toCangJieValue(threadId, frameIndex, facade)
                )
            }
            node.addChildren(children, true)
        }
    }


    /**
     * 支持修改变量值
     *
     * @return 返回变量修改器，让后端处理是否可以修改
     */
    override fun getModifier(): XValueModifier {
        return CangJieValueModifier(
            variable = lldbVariable,
            facade = facade
        )
    }


}

/**
 * 寄存器变量值
 *
 * 表示调试器中的一个寄存器值，基于 LLDBRegister。
 * 寄存器值通常不能修改，但可能有子元素（如向量寄存器）。
 *
 * @param lldbRegister LLDB 寄存器数据
 * @param threadId 线程 ID
 * @param frameIndex 栈帧索引
 * @param facade 调试器驱动门面
 */
class CangJieRegisterValue(
    private val lldbRegister: LLDBRegister,
    override val threadId: Long,
    override val frameIndex: Int,
    override val facade: DebuggerDriverFacade
) : CangJieValue(lldbRegister.name) {

    /**
     * 获取值的字符串表示
     *
     * @param node 值节点
     * @param place 值的位置
     */
    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        // 寄存器值通常不需要异步获取，直接使用 register 中的信息
        // 应用十六进制格式化设置
        val rawDisplayValue = if (lldbRegister.summary.isNotEmpty()) {
            lldbRegister.summary
        } else {
            lldbRegister.value
        }

        // 寄存器值使用专门的格式化器
        val displayValue = ValueFormatter.formatRegisterValue(rawDisplayValue)

        val typeDisplay = if (lldbRegister.typeName.isNotEmpty()) {
            lldbRegister.typeName
        } else {
            "register" // 默认类型显示
        }

        node.setPresentation(
            AllIcons.Debugger.Value, // 可以考虑使用寄存器专用图标
            typeDisplay,
            displayValue,
            lldbRegister.hasChildren()
        )

        node.setPresentation(
            AllIcons.Debugger.Value,
            object : XValuePresentation() {
                override fun renderValue(renderer: XValuePresentation.XValueTextRenderer) {
                    renderer.renderValue(displayValue)
                }

                override fun getType(): String = typeDisplay

                override fun getSeparator(): String = " = "
            },
            lldbRegister.hasChildren()
        )
    }

    /**
     * 计算子节点
     *
     * @param node 子节点容器
     */
    override fun computeChildren(node: XCompositeNode) {
        if (lldbRegister.hasChildren()) {
            val children = XValueChildrenList()

            // 将寄存器的子元素转换为 CangJieRegisterValue
            lldbRegister.children.forEach { childRegister ->
                children.add(
                    childRegister.toCangJieRegisterValue(
                        threadId = threadId,
                        frameIndex = frameIndex,
                        facade = facade
                    )
                )
            }

            node.addChildren(children, true)
        }
    }

    /**
     * 寄存器值通常不支持修改
     *
     * @return null 表示不支持修改
     */
    override fun getModifier(): XValueModifier? = null
}

