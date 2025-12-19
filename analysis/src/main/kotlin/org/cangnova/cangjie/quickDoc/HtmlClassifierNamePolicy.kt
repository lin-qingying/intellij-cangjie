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

package org.cangnova.cangjie.quickDoc
import com.google.common.html.HtmlEscapers
import com.intellij.codeInsight.documentation.DocumentationManagerUtil
import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.CompositeDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.lang.documentation.DocumentationSettings
import com.intellij.lang.documentation.ExternalDocumentationProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.FrontendInternals
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.cdoc.CDocTemplate
import org.cangnova.cangjie.lexer.cdoc.insert
import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.navigation.SourceNavigationHelper
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.*
import org.cangnova.cangjie.quickDoc.cdoc.CDocRenderer.appendCodeSnippetHighlightedByLexer
import org.cangnova.cangjie.quickDoc.cdoc.CDocRenderer.appendHighlighted
import org.cangnova.cangjie.quickDoc.cdoc.CDocRenderer.createHighlightingManager
import org.cangnova.cangjie.quickDoc.cdoc.CDocRenderer.highlight
import org.cangnova.cangjie.quickDoc.cdoc.CDocRenderer.renderCDoc
import org.cangnova.cangjie.quickDoc.cdoc.CangJieIdeDescriptorRenderer
import org.cangnova.cangjie.quickDoc.cdoc.ClassifierNamePolicyEx
import org.cangnova.cangjie.quickDoc.cdoc.findCDoc
import org.cangnova.cangjie.references.mainReference
import org.cangnova.cangjie.references.resolveCDocLink
import org.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import org.cangnova.cangjie.renderer.*
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.caches.getResolutionFacade
import org.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import org.cangnova.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import org.cangnova.cangjie.resolve.deprecation.DeprecationResolver
import org.cangnova.cangjie.resolve.deprecation.deprecatedByAnnotationReplaceWithExpression
import org.cangnova.cangjie.resolve.lazy.BodyResolveMode
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.fqNameUnsafe
import org.cangnova.cangjie.types.isDefinitelyNonOptionType
import org.cangnova.cangjie.utils.safeAs
import org.jetbrains.annotations.Nls
import java.util.function.Consumer
/**
 * HTML 分类器名称策略。
 *
 * 该类为仓颉语言的类型分类器（类、接口、枚举等）提供 HTML 格式的名称渲染。
 * 主要功能是将类型名称转换为可点击的超链接，以支持快速导航。
 *
 * ## 特殊处理
 *
 * ### 内置类型
 * 对于基本的内置类型（如 Int8、Int16、Bool、Float32 等），不会生成超链接，
 * 直接返回纯文本名称，因为这些类型通常不需要导航到定义处。
 *
 * ### 非 Option 类型
 * 对于标记为非 Option 的类型，会在名称后附加 ` & Any` 标记。
 *
 * @property base 基础的分类器名称策略，用于获取原始名称
 *
 * @see ClassifierNamePolicyEx
 * @see isBoringBuiltinClass
 */
class HtmlClassifierNamePolicy(val base: ClassifierNamePolicy) : ClassifierNamePolicyEx {

    override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String =
        render(classifier, renderer, null)

    override fun renderClassifierWithType(
        classifier: ClassifierDescriptor,
        renderer: DescriptorRenderer,
        type: CangJieType
    ): String =
        render(classifier, renderer, type)

    /**
     * 渲染分类器名称为 HTML 格式。
     *
     * @param classifier 要渲染的分类器描述符
     * @param renderer 描述符渲染器
     * @param type 可选的类型信息，用于判断是否为非 Option 类型
     * @return HTML 格式的分类器名称（可能包含超链接）
     */
    private fun render(classifier: ClassifierDescriptor, renderer: DescriptorRenderer, type: CangJieType?): String {

        val name =
            base.renderClassifier(classifier, renderer) + (type?.takeIf { it.isDefinitelyNonOptionType }
                ?.let { " & Any" }
                ?: "")

        if (classifier.isBoringBuiltinClass())
            return name
        return buildString {
            val ref = classifier.fqNameUnsafe.toString()
            DocumentationManagerUtil.createHyperlink(this, ref, name, true)
        }
    }
}
