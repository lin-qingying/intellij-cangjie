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

package org.cangnova.cangjie.highlighter.visitor

import org.cangnova.cangjie.highlighter.HighlightingFactory
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjVisitorVoid
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * 高亮访问器抽象基类 - 提供语法高亮的通用访问器功能
 * Abstract Highlighting Visitor - Provides common visitor functionality for syntax highlighting
 *
 * 这个抽象类为仓颉语言的语法高亮访问器提供了基础实现。主要功能包括：
 * This abstract class provides the base implementation for syntax highlighting visitors in CangJie language. Main features include:
 *
 * 1. 元素高亮 (Element Highlighting)
 *    - 提供 highlightName 方法用于高亮各类语法元素
 *    - Provides highlightName methods for highlighting various syntax elements
 *
 * 2. 声明高亮 (Declaration Highlighting)
 *    - 通过 highlightNamedDeclaration 方法处理命名声明的高亮
 *    - Handles highlighting of named declarations through highlightNamedDeclaration method
 *
 * 3. 高亮信息管理 (Highlight Info Management)
 *    - 维护 HighlightInfoHolder 用于存储高亮信息
 *    - Maintains HighlightInfoHolder for storing highlighting information
 *
 * 该类继承自 CjVisitorVoid，是仓颉语言语法高亮系统的核心组件之一。
 * This class extends CjVisitorVoid and is one of the core components of the CangJie language highlighting system.
 */

abstract class AbstractHighlightingVisitor(protected val holder: HighlightInfoHolder) : CjVisitorVoid() {
    protected fun highlightName(element: PsiElement, highlightInfoType: HighlightInfoType, message: String? = null) {
        holder.add(HighlightingFactory.highlightName(element, highlightInfoType, message)?.create())
    }

    protected fun highlightName(
        project: Project,
        textRange: TextRange,
        highlightInfoType: HighlightInfoType,
        message: String? = null
    ) {
        holder.add(HighlightingFactory.highlightName(project, textRange, highlightInfoType, message).create())
    }

    protected fun highlightNamedDeclaration(declaration: CjNamedDeclaration, attributesKey: HighlightInfoType) {
        declaration.nameIdentifier?.let { highlightName(it, attributesKey) }
    }


}
