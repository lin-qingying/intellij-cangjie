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
 * 值参数处理器包装类。
 *
 * 该类包装了基础的值参数处理器，在渲染参数时添加额外的格式化逻辑，
 * 如在每个参数前添加换行和缩进，使参数列表更易读。
 *
 * @property base 基础的值参数处理器
 *
 * @see DescriptorRenderer.ValueParametersHandler
 */
class WrapValueParameterHandler(val base: DescriptorRenderer.ValueParametersHandler) :
    DescriptorRenderer.ValueParametersHandler {


    override fun appendBeforeValueParameters(parameterCount: Int, builder: StringBuilder) {
        base.appendBeforeValueParameters(parameterCount, builder)
    }

    override fun appendBeforeValueParameter(
        parameter: ValueParameterDescriptor,
        parameterIndex: Int,
        parameterCount: Int,
        builder: StringBuilder
    ) {
        builder.append("\n    ")
        base.appendBeforeValueParameter(parameter, parameterIndex, parameterCount, builder)
    }

    override fun appendAfterValueParameter(
        parameter: ValueParameterDescriptor,
        parameterIndex: Int,
        parameterCount: Int,
        builder: StringBuilder
    ) {
        if (parameterIndex != parameterCount - 1) {
            builder.append(",")
        }
    }

    override fun appendAfterValueParameters(parameterCount: Int, builder: StringBuilder) {
        if (parameterCount > 0) {
            builder.appendLine()
        }
        base.appendAfterValueParameters(parameterCount, builder)
    }
}
