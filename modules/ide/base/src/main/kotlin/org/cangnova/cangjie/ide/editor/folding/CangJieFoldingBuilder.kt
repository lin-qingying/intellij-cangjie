/*
 * Copyright 2026 LinQingYing. and contributors.
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
 */

package org.cangnova.cangjie.ide.editor.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.codeinsight.folding.CangJieFoldingRangeCollector
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes

/**
 * 仓颉 IDE 折叠扩展外壳。
 *
 * 具体折叠规则由 `code-insight:folding` 共享模块维护，IDE 侧只负责转换为 IntelliJ `FoldingDescriptor`。
 */
class CangJieFoldingBuilder : CustomFoldingBuilder(), DumbAware {
    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement,
        document: Document,
        quick: Boolean,
    ) {
        if (root !is CjFile) return

        CangJieFoldingRangeCollector.collect(root, document).forEach { region ->
            descriptors.add(
                FoldingDescriptor(region.element.node, region.range).apply {
                    setCanBeRemovedWhenCollapsed(region.canBeRemovedWhenCollapsed)
                },
            )
        }
    }

    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String {
        return CangJieFoldingRangeCollector.placeholderText(node)
    }

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean = false

    override fun isCustomFoldingRoot(node: ASTNode): Boolean {
        return node.elementType == CjNodeTypes.BLOCK || node.elementType == CjNodeTypes.CLASS_BODY
    }
}
