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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement


/**
 * 根据类名查找指定类型的父元素
 *
 * 从当前元素向上遍历父元素链，查找第一个类名匹配的元素。
 * 支持通过类名字符串匹配，会检查类本身、父类和接口。
 *
 * @param psiClassNames 要匹配的 PSI 类名列表（简单类名，非全限定名）
 * @return 找到的父元素，如果未找到或到达文件根节点则返回 null
 */
private fun PsiElement.parentOfType(vararg psiClassNames: String): PsiElement? {
    fun acceptsClass(javaClass: Class<*>): Boolean {
        if (javaClass.simpleName in psiClassNames) return true
        javaClass.superclass?.let { if (acceptsClass(it)) return true }
        for (superInterface in javaClass.interfaces) {
            if (acceptsClass(superInterface)) return true
        }
        return false
    }
    return generateSequence(this) { it.parent }
        .filter { it !is PsiFile }
        .firstOrNull { acceptsClass(it::class.java) }
}

/**
 * 获取元素的文本内容及其上下文信息
 *
 * 用于调试和日志输出，将 PSI 元素以带标签的形式嵌入其上下文中显示。
 * 上下文优先级：导入项 > 包声明 > 声明体 > 属性 > 文件
 *
 * 输出格式示例：
 * ```
 * <File name: example.cj, Physical: true>
 * func foo() {
 *     <ELEMENT>bar</ELEMENT>
 * }
 * ```
 *
 * @param psiElement 要获取上下文的 PSI 元素
 * @return 包含文件信息和带标记元素的上下文文本，如果元素无效则返回错误提示
 */
fun getElementTextWithContext(psiElement: PsiElement): String {
    if (!psiElement.isValid) return "<invalid element $psiElement>"

    @Suppress("LocalVariableName")
    val ELEMENT_TAG = "ELEMENT"
    val containingFile = psiElement.containingFile

    // 按优先级查找最近的上下文容器
    val context = psiElement.parentOfType("CjImportDirectiveItem")
        ?: psiElement.parentOfType("CjPackageDirective")
        ?: psiElement.parentOfType("CjDeclarationWithBody")
        ?: psiElement.parentOfType("CjProperty")
        ?: containingFile

    // 遍历上下文，用标签包裹目标元素
    val elementTextInContext = buildString {
        context.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element === psiElement) append("<$ELEMENT_TAG>")
                if (element is LeafPsiElement) {
                    append(element.text)
                } else {
                    element.acceptChildren(this)
                }
                if (element === psiElement) append("</$ELEMENT_TAG>")
            }
        })
    }.trimIndent().trim()

    return buildString {
        appendLine("<File name: ${containingFile.name}, Physical: ${containingFile.isPhysical}>")
        append(elementTextInContext)
    }
}


