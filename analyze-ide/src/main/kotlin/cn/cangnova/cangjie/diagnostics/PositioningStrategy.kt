/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.diagnostics

import cn.cangnova.cangjie.psi.psiUtil.endOffset
import cn.cangnova.cangjie.psi.psiUtil.startOffset
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace

/**
 * 定位策略类
 *
 * 用于确定与诊断相关的PSI元素的文本范围，以便在编辑器中正确高亮显示错误或警告
 *
 * @param E PSI元素类型
 */
open class PositioningStrategy<in E : PsiElement>{
    /**
     * 标记诊断
     *
     * 获取与诊断相关联的文本范围列表
     *
     * @param diagnostic 要标记的诊断
     * @return 文本范围列表
     */
    open fun markDiagnostic(diagnostic: DiagnosticMarker): List<TextRange> {
        @Suppress("UNCHECKED_CAST")
        return mark(diagnostic.psiElement as E)
    }
    
    /**
     * 标记元素
     *
     * 获取与指定元素相关联的文本范围列表
     *
     * @param element 要标记的PSI元素
     * @return 文本范围列表
     */
    open fun mark(element: E): List<TextRange> {
        return markElement(element)
    }

    /**
     * 检查元素是否有效
     *
     * 确定元素是否适合用于诊断标记
     *
     * @param element 要检查的PSI元素
     * @return 如果元素有效则为true，否则为false
     */
    open fun isValid(element: E): Boolean {
        return !hasSyntaxErrors(element)
    }
}

/**
 * 检查PSI元素是否包含语法错误
 *
 * @param psiElement 要检查的PSI元素
 * @return 如果元素包含语法错误则为true，否则为false
 */
fun hasSyntaxErrors(psiElement: PsiElement): Boolean {
    if (psiElement is PsiErrorElement) return true

    val children = psiElement.children
    return children.isNotEmpty() && hasSyntaxErrors(children.last())
}

/**
 * 标记文本范围
 *
 * @param range 要标记的文本范围
 * @return 包含该范围的列表
 */
fun markRange(range: TextRange): List<TextRange> {
    return listOf(range)
}

/**
 * 标记从一个元素到另一个元素的范围
 *
 * @param from 起始PSI元素
 * @param to 结束PSI元素
 * @return 包含该范围的列表
 */
fun markRange(from: PsiElement, to: PsiElement): List<TextRange> {
    return markRange(TextRange(getStartOffset(from), getEndOffset(to)))
}

/**
 * 标记单个PSI元素
 *
 * @param element 要标记的PSI元素
 * @return 包含该元素范围的列表
 */
fun markElement(element: PsiElement): List<TextRange> {
    return listOf(TextRange(getStartOffset(element), getEndOffset(element)))
}

/**
 * 获取元素的结束偏移量
 *
 * 跳过尾部的注释和空白
 *
 * @param element 要获取偏移量的PSI元素
 * @return 结束偏移量
 */
private fun getEndOffset(element: PsiElement): Int {
    var child = element.lastChild
    if (child != null) {
        while (child is PsiComment || child is PsiWhiteSpace) {
            child = child.prevSibling
        }
        if (child != null) {
            return getEndOffset(child)
        }
    }

    return element.endOffset
}

/**
 * 获取元素的起始偏移量
 *
 * 跳过开头的注释和空白
 *
 * @param element 要获取偏移量的PSI元素
 * @return 起始偏移量
 */
private fun getStartOffset(element: PsiElement): Int {
    var child = element.firstChild
    if (child != null) {
        while (child is PsiComment || child is PsiWhiteSpace) {
            child = child.nextSibling
        }
        if (child != null) {
            return getStartOffset(child)
        }
    }
    return element.startOffset
}

