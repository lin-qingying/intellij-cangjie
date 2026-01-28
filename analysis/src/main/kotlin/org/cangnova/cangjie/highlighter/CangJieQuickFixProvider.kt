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

import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.psi.CjElement
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.containers.MultiMap
import org.cangnova.cangjie.inspections.suppress.AnnotationHostKind

/**
 * 仓颉快速修复提供者接口
 *
 * 该接口定义了为仓颉语言的诊断信息创建快速修复（Quick Fix）的方法。
 * 快速修复是 IDE 中帮助开发者自动修正代码问题的功能，通常通过 Alt+Enter 触发。
 *
 * 实现此接口的类负责：
 * - 根据不同类型的诊断信息生成相应的修复操作
 * - 处理未解析引用的修复建议
 * - 提供代码问题的抑制（Suppress）选项
 */
interface CangJieQuickFixProvider {
    companion object {
        /**
         * 获取项目级别的快速修复提供者实例
         *
         * 使用 IntelliJ 的服务机制获取当前项目的快速修复提供者实例。
         * 这是一个单例模式的实现，确保每个项目只有一个提供者实例。
         *
         * @param project 当前项目
         * @return 快速修复提供者实例
         */
        fun getInstance(project: Project): CangJieQuickFixProvider = project.service()
    }

    /**
     * 为同类型的诊断信息创建快速修复操作
     *
     * 该方法接收一组相同类型的诊断信息，为每个诊断创建对应的修复操作。
     * 返回的 MultiMap 允许一个诊断对应多个修复方案，用户可以选择最合适的。
     *
     * @param sameTypeDiagnostics 同一类型的诊断信息集合
     * @return 诊断信息到快速修复操作的多重映射
     */
    fun createQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    /**
     * 创建延迟处理的未解析引用快速修复
     *
     * 用于处理需要延迟计算或异步处理的未解析引用问题。
     * 这种延迟机制可以提高 IDE 的响应速度，避免在高亮显示时进行耗时的计算。
     *
     * @param sameTypeDiagnostics 同一类型的诊断信息集合
     * @return 诊断信息到快速修复操作的多重映射
     */
    fun createPostponedUnresolvedReferencesQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    /**
     * 创建未解析引用的快速修复
     *
     * 针对代码中无法解析的引用（如未导入的类、未定义的变量等）创建修复操作。
     * 常见的修复包括：导入缺失的包、创建缺失的声明、修正拼写错误等。
     *
     * @param sameTypeDiagnostics 同一类型的诊断信息集合
     * @return 诊断信息到快速修复操作的多重映射
     */
    fun createUnresolvedReferenceQuickFixes(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction>

    /**
     * 为特定元素创建未解析引用的快速修复（惰性计算）
     *
     * 该方法采用惰性（lazy）计算策略，只在真正需要时才生成修复操作。
     * 这种方式可以避免创建大量冗余的快速修复，特别是在只需要第一个合适的修复方案时。
     *
     * 返回的 Sequence 是惰性序列，修复操作只在被访问时才会创建，
     * 这对于性能优化特别有用，尤其是在有多个可能修复方案的情况下。
     *
     * @param element 需要修复的仓颉元素
     * @return PSI 元素到修复操作序列的映射
     */
    fun createUnresolvedReferenceQuickFixesForElement(element: CjElement): Map<PsiElement, Sequence<IntentionAction>>

    /**
     * 创建抑制警告/错误的修复操作
     *
     * 生成用于抑制特定诊断信息的快速修复。用户可以选择在不同级别
     * （如函数、类、文件）抑制某些警告或检查，通常通过添加注解实现。
     *
     * @param element 需要抑制警告的仓颉元素
     * @param suppressionKey 抑制标识符，用于标识要抑制的警告类型
     * @param hostKind 注解宿主类型，指定在哪个级别添加抑制注解（函数/类/文件等）
     * @return 抑制操作的 IntentionAction
     */
    fun createSuppressFix(element: CjElement, suppressionKey: String, hostKind: AnnotationHostKind): SuppressIntentionAction
}

/**
 * 延迟注册快速修复的意图动作（占位符）
 *
 * 这是一个特殊的单例对象，用作快速修复的占位符标记。
 * 它表示某些快速修复需要延迟注册，而不是立即创建。
 *
 * 这个对象本身不执行任何实际操作，它的存在主要用于：
 * 1. 在快速修复列表中占位，表示有修复操作需要稍后计算
 * 2. 作为标记，让系统知道需要进行额外的处理来获取真正的修复操作
 * 3. 优化性能，避免在不需要时过早创建昂贵的修复对象
 *
 * 实际使用中，这个对象会被识别并在适当的时机替换为真正的修复操作。
 */
object RegisterQuickFixesLaterIntentionAction : IntentionAction {
    /**
     * 获取此操作的显示文本
     *
     * 由于这是一个占位符对象，不会实际显示给用户，因此返回空字符串。
     *
     * @return 空字符串
     */
    override fun getText(): String = ""

    /**
     * 获取此操作所属的家族名称
     *
     * 家族名称用于在 IDE 中对相似的意图操作进行分组。
     * 由于这是一个内部使用的占位符，返回空字符串。
     *
     * @return 空字符串
     */
    override fun getFamilyName(): String = ""

    /**
     * 检查此操作是否可用
     *
     * 由于这是一个占位符对象，不应该被实际执行，因此始终返回 false。
     * 这确保了用户不会意外触发这个占位符操作。
     *
     * @param project 当前项目
     * @param editor 编辑器实例（可能为 null）
     * @param file 当前文件（可能为 null）
     * @return 始终返回 false，表示此操作不可用
     */
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = false

    /**
     * 执行此操作
     *
     * 由于这是一个占位符对象，执行方法为空操作（Unit）。
     * 实际上这个方法不应该被调用，因为 isAvailable 始终返回 false。
     *
     * @param project 当前项目
     * @param editor 编辑器实例（可能为 null）
     * @param file 当前文件（可能为 null）
     */
    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) = Unit

    /**
     * 指示此操作是否需要在写操作中执行
     *
     * 返回 false 表示此操作不需要在写操作中启动。
     * 写操作用于修改文件内容，而这个占位符对象不执行任何修改操作。
     *
     * @return 始终返回 false
     */
    override fun startInWriteAction(): Boolean = false
}