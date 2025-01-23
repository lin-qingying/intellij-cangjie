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

package com.linqingying.cangjie.highlighter

import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.ide.inspections.suppress.AnnotationHostKind
import com.linqingying.cangjie.psi.CjElement
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.containers.MultiMap



interface CangJieQuickFixProvider {
    companion object {
        fun getInstance(project: Project): CangJieQuickFixProvider = project.service()
    }

    fun createQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    fun createPostponedUnresolvedReferencesQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    fun createUnresolvedReferenceQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    /**
     * Produces fixes for diagnostics from different factories lazily to avoid creation of redundant quick fixes,
     * e.g. in case only the first suitable fix is required.
     */
    fun createUnresolvedReferenceQuickFixesForElement(element: CjElement): Map<PsiElement, Sequence<IntentionAction>>

    fun createSuppressFix(element: CjElement, suppressionKey: String, hostKind: AnnotationHostKind): SuppressIntentionAction
}
/**
 * 实现IntentionAction接口的单例对象，用于在稍后注册快速修复建议。
 * 此类主要用于IDE中，为开发者提供代码问题的自动修复选项。
 */
object RegisterQuickFixesLaterIntentionAction : IntentionAction {
    /**
     * 获取此操作的文本描述，此处返回空字符串。
     * @return 空字符串
     */
    override fun getText(): String = ""

    /**
     * 获取此操作所属的家族名称，此处返回空字符串。
     * 家族名称用于对相似的操作进行分组。
     * @return 空字符串
     */
    override fun getFamilyName(): String = ""

    /**
     * 检查此操作是否适用于给定的上下文。
     * 此处始终返回false，表示此操作不适用于任何上下文。
     * @param project 当前项目
     * @param editor 编辑器实例，可能为null
     * @param file 当前文件，可能为null
     * @return 始终返回false
     */
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = false

    /**
     * 执行此操作，此处为空操作。
     * @param project 当前项目
     * @param editor 编辑器实例，可能为null
     * @param file 当前文件，可能为null
     */
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) = Unit

    /**
     * 表示此操作是否需要在写操作中启动。
     * 此处返回false，表示不需要在写操作中启动。
     * @return 始终返回false
     */
    override fun startInWriteAction(): Boolean = false
}
