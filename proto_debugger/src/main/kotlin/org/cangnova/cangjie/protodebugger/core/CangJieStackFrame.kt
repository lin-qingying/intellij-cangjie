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

import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueModifier
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XValuePlace
import kotlinx.coroutines.launch
import org.cangnova.cangjie.protodebugger.data.LLThread

/**
 * 仓颉调试器栈帧
 *
 * 表示调用栈中的一个帧，包含函数名、源码位置、局部变量等信息。
 *
 * @param frameIndex 栈帧在调用栈中的索引
 * @param functionName 函数名
 * @param thread 关联的执行线程
 * @param debuggerDriver 调试器驱动门面
 */
class CangJieStackFrame(
    private val frameIndex: Int,
    private val functionName: String,
    private val thread: LLThread,
    private val debuggerDriver: DebuggerDriverFacade
) : XStackFrame() {

    /**
     * 获取源码位置
     *
     * @return 源码位置对象，如果无法确定源码位置则返回null
     */
    override fun getSourcePosition(): XSourcePosition? {
        // 尝试获取当前栈帧的源码位置
        // 这里需要调用实际的调试器API获取源码位置信息
        return null
    }

    /**
     * 获取变量列表
     *
     * @param node 变量节点的容器，用于添加局部变量和参数
     */
    override fun computeChildren(node: XCompositeNode) {
        val children = XValueChildrenList()

        try {

        } catch (e: Exception) {
            node.setErrorMessage("Failed to get variables: ${e.message}")
        }
    }
}

/**
 * 仓颉调试器值
 *
 * 表示调试器中的一个值，可以是变量、表达式结果等。
 *
 * @param name 值的名称
 * @param type 值的类型
 * @param value 值的字符串表示
 */
class CangJieValue(
    private val name: String,
    private val type: String,
    private val value: String
) : XNamedValue(name) {

    /**
     * 获取值的字符串表示
     *
     * @param node 值节点
     * @param place 值的位置
     */
    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        // 设置值的显示信息
        // 这里需要根据XValueNode的实际API来设置值和类型
    }

    /**
     * 计算子节点
     *
     * @param node 子节点容器
     */
    override fun computeChildren(node: XCompositeNode) {
        // 检查是否有子节点
        val hasChildren = type in setOf("Array", "Struct", "Class", "Map", "List")

        if (!hasChildren) {
            super.computeChildren(node)
            return
        }

        val children = XValueChildrenList()

        // 根据类型添加子节点
        when (type) {
            "Array" -> {
                children.add(CangJieValue("[0]", "Int", "1"))
                children.add(CangJieValue("[1]", "Int", "2"))
            }
            "Struct" -> {
                children.add(CangJieValue("field1", "Int", "10"))
                children.add(CangJieValue("field2", "String", "\"hello\""))
            }
        }

        node.addChildren(children, true)
    }
}