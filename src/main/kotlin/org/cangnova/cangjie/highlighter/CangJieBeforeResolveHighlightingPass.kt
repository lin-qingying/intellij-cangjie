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

package org.cangnova.cangjie.highlighter

import org.cangnova.cangjie.highlighter.visitor.AbstractHighlightingVisitor
import org.cangnova.cangjie.psi.CjFile
import com.intellij.codeHighlighting.*
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor

/**
 * 仓颉语言的符号解析前高亮处理器
 * Pre-Resolution Highlighting Pass for CangJie Language
 *
 * 这个类是仓颉语言语法高亮系统中的关键组件，负责在符号解析之前进行高亮处理。主要功能包括：
 * This class is a key component in CangJie's syntax highlighting system, responsible for highlighting before symbol resolution. Main features:
 *
 * 1. 高亮处理流程 (Highlighting Process)
 *    - 接收 CjFile 和 Document 作为输入
 *    - 使用访问器模式遍历代码元素
 *    - 应用基本的语法高亮规则
 *
 * 2. 扩展机制 (Extension Mechanism)
 *    - 通过 BeforeResolveHighlightingExtension 支持插件扩展
 *    - 允许添加自定义的高亮规则
 *    - 使用 ExtensionPointName 管理扩展点
 *
 * 3. 工厂模式集成 (Factory Integration)
 *    - Factory 类用于创建高亮处理实例
 *    - Registrar 类负责注册高亮处理器
 *    - 支持 IDE 的高亮处理框架
 *
 * 核心组件说明：
 * Core Components:
 *
 * 1. CangJieBeforeResolveHighlightingPass
 *    - 继承自 AbstractHighlightingPassBase
 *    - 实现主要的高亮处理逻辑
 *    - 协调各个访问器的工作
 *
 * 2. Factory 内部类
 *    - 实现 TextEditorHighlightingPassFactory
 *    - 负责创建高亮处理器实例
 *    - 确保只处理 CjFile 类型的文件
 *
 * 3. Registrar 内部类
 *    - 实现 TextEditorHighlightingPassFactoryRegistrar
 *    - 注册高亮处理器到 IDE 框架
 *    - 配置高亮处理的时机和顺序
 *
 * 4. BeforeResolveHighlightingExtension 接口
 *    - 定义扩展点接口
 *    - 允许添加自定义高亮访问器
 *    - 支持插件化的语法高亮扩展
 *
 * 工作流程：
 * Workflow:
 * 1. IDE 触发高亮请求
 * 2. Factory 创建高亮处理器实例
 * 3. 处理器使用访问器遍历代码
 * 4. 应用高亮规则和扩展规则
 * 5. 更新编辑器的高亮显示
 */

class CangJieBeforeResolveHighlightingPass(file: CjFile, document: Document) : AbstractHighlightingPassBase(file, document) {
    override fun runAnnotatorWithContext(element: PsiElement, holder: HighlightInfoHolder) {
        val visitor = BeforeResolveHighlightingVisitor(holder)
        val extensions = EP_NAME.extensionList.map { it.createVisitor(holder) }

        element.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                element.accept(visitor)
                extensions.forEach(element::accept)
                super.visitElement(element)
            }
        })
    }

    class Factory : TextEditorHighlightingPassFactory, DumbAware {
        override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
            if (file !is CjFile) return null
            return CangJieBeforeResolveHighlightingPass(file, editor.document)
        }
    }

    class Registrar : TextEditorHighlightingPassFactoryRegistrar {
        override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
            registrar.registerTextEditorHighlightingPass(
                Factory(),
               TextEditorHighlightingPassRegistrar.Anchor.BEFORE,
              Pass.UPDATE_FOLDING,
                false,
                 false
            )
        }
    }

    companion object {
        val EP_NAME = ExtensionPointName.create<BeforeResolveHighlightingExtension>("org.cangnova.cangjie.beforeResolveHighlightingVisitor")
    }
}


interface BeforeResolveHighlightingExtension {
    fun createVisitor(holder: HighlightInfoHolder): AbstractHighlightingVisitor
}
