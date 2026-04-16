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

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes.QUOTE_EXPRESSION
import org.cangnova.cangjie.psi.CjNodeTypes.QUOTE_PARAMETERS
import org.cangnova.cangjie.psi.CjNodeTypes.QUOTE_INTERPOLATE
import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType

/**
 * 格式化规则管理器
 */
object FormattingRules {
    /**
     * 检查节点是否应该跳过格式化
     */
    fun shouldSkipFormatting(node: ASTNode): Boolean {
        val parent = node.treeParent
        return when {
            // quote 表达式内的所有内容都不格式化
            parent?.elementType == QUOTE_EXPRESSION && node.elementType != CjTokens.QUOTE_KEYWORD -> true
            // quote 参数不格式化
            node.elementType == QUOTE_PARAMETERS -> true
            // quote 插值表达式不格式化
            node.elementType == QUOTE_INTERPOLATE -> true
            // 其他不需要格式化的内容可以在这里添加
            else -> false
        }
    }

    /**
     * 添加新的不格式化规则
     * @param rule 规则函数，接收 ASTNode 参数，返回是否跳过格式化
     */
    private val skipFormattingRules = mutableListOf<(ASTNode) -> Boolean>()

    fun addSkipFormattingRule(rule: (ASTNode) -> Boolean) {
        skipFormattingRules.add(rule)
    }

    /**
     * 检查节点的父节点类型是否匹配，且节点类型不在排除列表中
     */
    fun checkParentType(node: ASTNode, parentType: IElementType, excludedTypes: Set<IElementType> = emptySet()): Boolean {
        val parent = node.treeParent
        return parent?.elementType == parentType && !excludedTypes.contains(node.elementType)
    }
} 