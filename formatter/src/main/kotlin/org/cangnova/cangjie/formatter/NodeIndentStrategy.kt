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




package org.cangnova.cangjie.formatter

import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.NonNls

/**
 * 用于确定 AST 节点的缩进策略的抽象类。
 * 该类提供了不同类型的缩进策略，包括固定缩进和基于位置的动态缩进。
 *
 * 这些策略可以根据节点的类型、父节点类型以及其他上下文信息来动态调整缩进。
 *
 * 主要包括两种缩进策略：
 * 1. `ConstIndentStrategy`：为所有符合条件的节点使用固定缩进。
 * 2. `PositionStrategy`：根据节点的父节点类型、节点类型以及额外的回调逻辑来决定缩进。
 *
 * 适用于复杂的代码格式化场景，特别是需要根据节点的上下文来动态调整缩进。
 *
 * ### 使用示例：
 * - `ConstIndentStrategy`：适用于需要固定缩进的场景，例如语法结构的统一缩进。
 * - `PositionStrategy`：适用于根据节点的父类型、节点类型以及自定义回调逻辑来决定缩进的场景。
 *
 * 示例：
 * ```
 * val strategy = NodeIndentStrategy.strategy("调试信息")
 *    .within(FUNC)
 *    .forType(PROPERTY)
 *    .set(Indent.getNormalIndent())
 * ```
 * 这个示例会将正常的缩进应用于 `FUNC` 节点内的 `PROPERTY` 节点。
 */
abstract class NodeIndentStrategy {


    /**
     * 获取给定 AST 节点的缩进。
     *
     * @param node 当前 AST 节点
     * @param settings 当前的代码风格设置
     * @return 适用于该节点的缩进
     */
    abstract fun getIndent(node: ASTNode, settings: CodeStyleSettings): Indent?

    /**
     * 固定缩进策略。
     * 所有符合条件的节点都会使用同一固定缩进。
     */

    class ConstIndentStrategy(private val indent: Indent) : NodeIndentStrategy() {
        override fun getIndent(node: ASTNode, settings: CodeStyleSettings): Indent {
            return indent
        }
    }

    /**
     * 位置策略：根据节点的父节点和其他条件来确定缩进。
     */
    class PositionStrategy(private val debugInfo: String?) : NodeIndentStrategy() {
        private var indentCallback: (CodeStyleSettings) -> Indent = { Indent.getNoneIndent() }

        private val within = ArrayList<IElementType>()
        private var withinCallback: ((ASTNode) -> Boolean)? = null

        private val notIn = ArrayList<IElementType>()

        private val forElement = ArrayList<IElementType>()
        private val notForElement = ArrayList<IElementType>()
        private var forElementCallback: ((ASTNode) -> Boolean)? = null

        override fun toString(): String {
            return "PositionStrategy " + (debugInfo ?: "No debug info")
        }

        /**
         * 设置固定的缩进类型。
         */
        fun set(indent: Indent): PositionStrategy {
            indentCallback = { indent }
            return this
        }

        /**
         * 设置根据代码风格设置来动态确定缩进类型。
         */
        fun set(indentCallback: (CodeStyleSettings) -> Indent): PositionStrategy {
            this.indentCallback = indentCallback
            return this
        }

        /**
         * 限制在某些父节点类型中应用该策略。
         */
        fun within(parents: TokenSet): PositionStrategy {
            val types = parents.types
            if (types.isEmpty()) {
                throw IllegalArgumentException("Empty token set is unexpected")
            }

            fillTypes(within, types[0], types.copyOfRange(1, types.size))
            return this
        }

        /**
         * 限制在某些父节点类型中应用该策略。
         */
        fun within(parentType: IElementType, vararg orParentTypes: IElementType): PositionStrategy {
            fillTypes(within, parentType, orParentTypes)
            return this
        }

        /**
         * 根据自定义条件来限制应用该策略。
         */
        fun within(callback: (ASTNode) -> Boolean): PositionStrategy {
            withinCallback = callback
            return this
        }

        /**
         * 限制不在某些父节点类型中应用该策略。
         */
        fun notWithin(parentType: IElementType, vararg orParentTypes: IElementType): PositionStrategy {
            fillTypes(notIn, parentType, orParentTypes)
            return this
        }

        /**
         * 放开父节点类型的限制，允许应用策略到任意父节点。
         */
        fun withinAny(): PositionStrategy {
            within.clear()
            notIn.clear()
            return this
        }

        /**
         * 对某些特定类型的节点应用该策略。
         */
        fun forType(elementType: IElementType, vararg otherTypes: IElementType): PositionStrategy {
            fillTypes(forElement, elementType, otherTypes)
            return this
        }

        /**
         * 对某些特定类型的节点不应用该策略。
         */
        fun notForType(elementType: IElementType, vararg otherTypes: IElementType): PositionStrategy {
            fillTypes(notForElement, elementType, otherTypes)
            return this
        }

        /**
         * 放开对特定节点类型的限制，允许应用该策略到任意节点。
         */
        fun forAny(): PositionStrategy {
            forElement.clear()
            notForElement.clear()
            return this
        }

        /**
         * 根据自定义条件对某些节点应用该策略。
         */
        fun forElement(callback: (ASTNode) -> Boolean): PositionStrategy {
            forElementCallback = callback
            return this
        }

        override fun getIndent(node: ASTNode, settings: CodeStyleSettings): Indent? {
            if (!isValidIndent(forElement, notForElement, node, forElementCallback)) return null

            val parent = node.treeParent
            if (parent != null) {
                if (!isValidIndent(within, notIn, parent, withinCallback)) return null
            } else if (within.isNotEmpty()) return null

            return indentCallback(settings)
        }

        /**
         * 填充一个类型集合。
         */
        private fun fillTypes(resultCollection: MutableList<IElementType>, singleType: IElementType, otherTypes: Array<out IElementType>) {
            resultCollection.clear()
            resultCollection.add(singleType)
            resultCollection.addAll(otherTypes)
        }
    }

    companion object {
        /**
         * 创建一个固定缩进策略。
         */
        fun constIndent(indent: Indent): NodeIndentStrategy {
            return ConstIndentStrategy(indent)
        }

        /**
         * 创建一个位置缩进策略。
         */
        fun strategy(@NonNls debugInfo: String?): PositionStrategy {
            return PositionStrategy(debugInfo)
        }
    }
}

/**
 * 检查节点是否符合缩进条件。
 */
private fun isValidIndent(
    elements: ArrayList<IElementType>,
    excludeElements: ArrayList<IElementType>,
    node: ASTNode,
    callback: ((ASTNode) -> Boolean)?
): Boolean {
    if (elements.isNotEmpty() && !elements.contains(node.elementType)) return false
    if (excludeElements.contains(node.elementType)) return false
    if (callback?.invoke(node) == false) return false
    return true
}
