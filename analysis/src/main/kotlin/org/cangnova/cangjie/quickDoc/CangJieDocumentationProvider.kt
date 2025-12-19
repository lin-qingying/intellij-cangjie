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
import com.intellij.psi.PsiWhiteSpace
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
 * 仓颉语言的文档提供者。
 *
 * 该类是 IntelliJ 平台的文档系统的核心实现，负责为仓颉语言的各种元素生成和显示文档。
 * 它集成了 CDoc 注释系统、语义分析、类型信息和快速导航功能。
 *
 * ## 主要功能
 *
 * 1. **快速文档（Quick Documentation）**: 当用户将鼠标悬停在代码元素上或按下 Ctrl+Q 时显示的文档
 * 2. **快速导航信息**: 在自动补全弹窗中显示的简化文档
 * 3. **CDoc 注释渲染**: 将 CDoc 注释格式化为 HTML 并显示
 * 4. **链接解析**: 处理文档中的交叉引用链接（如 `@see` 标签）
 * 5. **特殊元素支持**: 枚举特殊函数、构造函数、属性等的文档生成
 *
 * ## 文档生成流程
 *
 * ```
 * 用户触发（Ctrl+Q / 悬停）
 *   ↓
 * generateDoc(element, originalElement)
 *   ↓
 * getText(element, originalElement, quickNavigation=false)
 *   ↓
 * 根据元素类型分发：
 *   - CjEnum → renderEnum()
 *   - CjDeclaration → renderCangJieDeclaration()
 *   - CjCallExpression → 解析调用目标
 *   ↓
 * buildCangJieDeclaration() / buildCangJie()
 *   ↓
 * 生成 HTML 文档（定义 + CDoc + 容器信息）
 * ```
 *
 * ## 特殊处理
 *
 * ### 枚举特殊函数
 * 枚举的 `values()`, `valueOf()` 等函数的文档来自枚举的超类。
 *
 * ### 主构造函数
 * 主构造函数的文档在其所属类的 CDoc 的 `@constructor` 标签中查找。
 *
 * ### 枚举条目
 * 显示枚举条目的序号信息。
 *
 * @see AbstractDocumentationProvider
 * @see ExternalDocumentationProvider
 * @see CDoc
 */
class CangJieDocumentationProvider : AbstractDocumentationProvider(), ExternalDocumentationProvider {
    @Deprecated(
        "Deprecated in Java", ReplaceWith(
            "CompositeDocumentationProvider.hasUrlsFor(this, element, originalElement)",
            "com.intellij.lang.documentation.CompositeDocumentationProvider"
        )
    )
    override fun hasDocumentationFor(element: PsiElement?, originalElement: PsiElement?): Boolean {
        return CompositeDocumentationProvider.hasUrlsFor(this, element, originalElement)

    }

    /**
     * 生成元素的完整文档（快速文档模式）。
     *
     * 当用户按下 Ctrl+Q 或通过菜单触发"查看文档"时调用。
     * 生成的文档包含元素的完整定义、CDoc 注释、参数说明、返回值等。
     *
     * @param element 要生成文档的元素
     * @param originalElement 光标所在的原始元素（用于上下文推断）
     * @return HTML 格式的文档字符串；如果无法生成文档则返回 `null`
     *
     * @see getQuickNavigateInfo
     */
    @Nls
    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {

        return getText(element, originalElement, false)
    }

    override fun canPromptToConfigureDocumentation(element: PsiElement?): Boolean {
        return false

    }

    /**
     * 解析文档中的超链接，返回链接指向的 PSI 元素。
     *
     * 当用户点击文档中的链接时（如 `@see` 标签中的引用），该方法负责将链接文本
     * 解析为对应的 PSI 元素，以支持快速导航。
     *
     * ## 链接格式
     *
     * 链接使用点分隔的名称路径，例如：
     * - `SomeClass` - 引用类
     * - `SomeClass.method` - 引用方法
     * - `package.SomeClass` - 引用带包名的类
     *
     * ## 解析过程
     *
     * 1. 获取上下文元素的描述符
     * 2. 使用 [resolveCDocLink] 在当前作用域中解析链接
     * 3. 将描述符转换回 PSI 元素
     *
     * @param psiManager PSI 管理器
     * @param link 链接文本（点分隔的名称）
     * @param context 链接所在的上下文元素
     * @return 链接指向的 PSI 元素；如果无法解析则返回 `null`
     *
     * @see resolveCDocLink
     * @see DescriptorToSourceUtilsIde.getAnyDeclaration
     */
    override fun getDocumentationElementForLink(
        psiManager: PsiManager,
        link: String,
        context: PsiElement?
    ): PsiElement? {
        val navElement = context?.navigationElement as? CjElement ?: return null
        val resolutionFacade = navElement.getResolutionFacade()
        val bindingContext = navElement.safeAnalyzeNonSourceRootCode(resolutionFacade, BodyResolveMode.PARTIAL)
        val contextDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, navElement] ?: return null
        val descriptors = resolveCDocLink(
            bindingContext, resolutionFacade,
            contextDescriptor, navElement, null, link.split('.')
        )
        val target = descriptors.firstOrNull() ?: return null
        return DescriptorToSourceUtilsIde.getAnyDeclaration(psiManager.project, target)
    }

    /**
     * 收集文件中的所有文档注释。
     *
     * 该方法遍历文件中的所有声明，收集它们的 CDoc 注释。
     * 主要用于 IDE 的文档注释相关功能，如"查找所有文档注释"。
     *
     * @param file 要收集注释的文件
     * @param sink 注释接收器，用于接收找到的文档注释
     */
    override fun collectDocComments(file: PsiFile, sink: Consumer<in PsiDocCommentBase>) {

        if (file !is CjFile) return

        PsiTreeUtil.processElements(file) {
            val comment = (it as? CjDeclaration)?.docComment
            if (comment != null) sink.accept(comment)
            true
        }
    }


    /**
     * 为自动补全的查找项获取对应的文档元素。
     *
     * 当用户在代码补全弹窗中选择某个项时，该方法返回对应的 PSI 元素用于显示文档。
     * 当前实现返回 `null`，表示不支持此功能。
     *
     * @param psiManager PSI 管理器
     * @param object 查找项对象
     * @param element 上下文元素
     * @return 文档元素；当前始终返回 `null`
     */
    override fun getDocumentationElementForLookupItem(
        psiManager: PsiManager,
        `object`: Any?,
        element: PsiElement?
    ): PsiElement? {
//        if (`object` is DescriptorBasedDeclarationLookupObject) {
//            `object`.psiElement?.let { return it }
//            `object`.descriptor?.let { descriptor ->
//                return DescriptorToSourceUtilsIde.getAnyDeclaration(psiManager.project, descriptor)
//            }
//        }
        return null
    }

    /**
     * 将文档注释渲染为 HTML。
     *
     * 该方法接收一个 CDoc 注释，将其渲染为格式化的 HTML 文档。
     * 用于在编辑器中显示预览。
     *
     * @param comment 文档注释
     * @return 渲染后的 HTML 字符串；如果不是 CDoc 注释则返回 `null`
     *
     * @see renderCDoc
     */
    @Nls
    override fun generateRenderedDoc(comment: PsiDocCommentBase): String? {
        val docComment = comment as? CDoc ?: return null

        val result = StringBuilder().also {
            it.renderCDoc(docComment.getDefaultSection(), docComment.getAllSections())
        }

        @Suppress("HardCodedStringLiteral")
        return result.toString()
    }

    /**
     * 获取自定义的文档元素。
     *
     * 该方法允许为特殊元素（如修饰符关键字）提供自定义的文档。
     * 当用户将光标放在修饰符上（如 `public`, `private` 等）时，
     * 返回该修饰符元素以显示其说明文档。
     *
     * @param editor 编辑器
     * @param file 文件
     * @param contextElement 上下文元素
     * @param targetOffset 目标偏移量
     * @return 如果是修饰符则返回该元素，否则返回 `null`
     */
    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        return if (contextElement.isModifier()) contextElement else null
    }

    /**
     * 生成快速导航信息（简化版文档）。
     *
     * 当用户在代码补全弹窗中查看元素信息时显示的简化文档。
     * 与 [generateDoc] 不同，快速导航信息只显示元素的定义，不包含完整的 CDoc 内容。
     *
     * @param element 要生成信息的元素
     * @param originalElement 光标所在的原始元素
     * @return HTML 格式的快速导航信息；如果无法生成则返回 `null`
     *
     * @see generateDoc
     */
    @Nls
    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        return if (element == null) null else getText(element, originalElement, true)
    }

    override fun promptToConfigureDocumentation(element: PsiElement?) {

    }

    /**
     * 延迟初始化对象，包含文档生成的共享配置。
     *
     * 该对象使用延迟初始化模式，避免不必要的对象创建开销。
     * 包含的配置会在多个文档生成调用之间复用。
     */
    private object Lazy {

        /**
         * 描述符渲染器，用于将描述符转换为 HTML 格式的定义字符串。
         *
         * ## 渲染器配置
         *
         * - **textFormat**: HTML 格式输出
         * - **classifierNamePolicy**: 使用 [HtmlClassifierNamePolicy] 生成可点击的类型链接
         * - **valueParametersHandler**: 使用 [WrapValueParameterHandler] 格式化参数列表
         * - **annotationArgumentsRenderingPolicy**: 仅在非空时显示注解参数
         * - **renderCompanionObjectName**: 显示伴生对象名称
         * - **renderPrimaryConstructorParametersAsProperties**: 将构造函数参数显示为属性
         * - **eachAnnotationOnNewLine**: 每个注解独占一行
         * - **defaultParameterValueRenderer**: 渲染参数默认值
         *
         * @see CangJieIdeDescriptorRenderer
         * @see HtmlClassifierNamePolicy
         * @see WrapValueParameterHandler
         */
        val DESCRIPTOR_RENDERER = CangJieIdeDescriptorRenderer.withOptions {
            textFormat = RenderingFormat.HTML
            modifiers = DescriptorRendererModifier.ALL
            classifierNamePolicy = HtmlClassifierNamePolicy(ClassifierNamePolicy.SHORT)
            valueParametersHandler = WrapValueParameterHandler(valueParametersHandler)
            annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
            renderCompanionObjectName = true
            renderPrimaryConstructorParametersAsProperties = true
            withDefinedIn = false
            eachAnnotationOnNewLine = true
//            excludedTypeAnnotationClasses = NULLABILITY_ANNOTATIONS
            defaultParameterValueRenderer = { (it.source.getPsi() as? CjParameter)?.defaultValue?.text ?: "..." }
        }
    }

    /**
     * 伴生对象，包含文档生成的私有辅助方法。
     */
    companion object {
        /**
         * 查找包含指定文本的元素。
         *
         * 该方法用于定位光标附近的特定文本元素，通常用于处理上下文相关的文档查询。
         *
         * @param element 起始元素
         * @param text 要查找的文本
         * @return 包含指定文本的元素，如果未找到则返回 `null`
         */
        private fun findElementWithText(element: PsiElement?, text: String): PsiElement? {
            return when {
                element == null -> null
                element.text == text -> element
                element.prevLeaf()?.text == text -> element.prevLeaf()
                else -> null
            }
        }

        private val LOG = Logger.getInstance(CangJieDocumentationProvider::class.java)

        /**
         * 渲染枚举的特殊函数文档。
         *
         * 枚举类型有一些特殊的函数（如 `values()`, `valueOf()` 等），这些函数的文档
         * 通常定义在枚举的超类中。该方法会查找这些文档并进行渲染。
         *
         * ## 查找策略
         *
         * 1. 解析枚举元素的描述符
         * 2. 获取枚举的超类描述符
         * 3. 在超类的源代码中查找对应函数的 CDoc 标签
         * 4. 渲染函数定义和文档内容
         *
         * @param element 枚举类元素
         * @param functionDescriptor 特殊函数的描述符
         * @param quickNavigation 是否为快速导航模式（简化版文档）
         * @return HTML 格式的函数文档
         */
        private fun renderEnumSpecialFunction(
            element: CjEnum,
            functionDescriptor: FunctionDescriptor,
            quickNavigation: Boolean
        ): String {
            val cdoc = run {
                val declarationDescriptor = element.resolveToDescriptorIfAny()
                val enumDescriptor = declarationDescriptor?.getSuperClassNotAny() ?: return@run null

                val enumDeclaration =
                    DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, enumDescriptor) as? CjDeclaration
                        ?: return@run null

                val enumSource = SourceNavigationHelper.getNavigationElement(enumDeclaration)
                val functionName = functionDescriptor.fqNameSafe.shortName().asString()
                return@run enumSource.findDescendantOfType<CDoc> { doc ->
                    doc.getChildrenOfType<CDocSection>().any { it.findTagByName(functionName) != null }
                }
            }

            return buildString {
                insert(CDocTemplate()) {
                    definition {
                        renderDefinition(
                            functionDescriptor, Lazy.DESCRIPTOR_RENDERER
                                .withIdeOptions { highlightingManager = createHighlightingManager(element.project) }
                        )
                    }
                    if (!quickNavigation && cdoc != null) {
                        description {
                            renderCDoc(cdoc.getDefaultSection())
                        }
                    }
                }
            }
        }

        /**
         * 渲染声明描述符的定义。
         *
         * 使用描述符渲染器将声明描述符转换为 HTML 格式的定义字符串，
         * 包括修饰符、名称、类型参数、参数列表、返回类型等。
         *
         * @receiver StringBuilder 用于构建 HTML 字符串的构建器
         * @param descriptor 要渲染的声明描述符
         * @param renderer 描述符渲染器
         */
        private fun StringBuilder.renderDefinition(descriptor: DeclarationDescriptor, renderer: DescriptorRenderer) {
            append(renderer.render(descriptor))
        }

        /**
         * 判断元素是否为修饰符关键字。
         *
         * @receiver PsiElement? 要判断的元素
         * @return 如果是修饰符关键字则返回 `true`
         */
        private fun PsiElement?.isModifier() =
            this != null && parent is CjModifierList && CjTokens.MODIFIER_KEYWORDS_ARRAY.firstOrNull { it.value == text } != null

        /**
         * 渲染枚举类的文档。
         *
         * 该方法处理枚举类的文档生成，包括枚举本身的文档和枚举特殊函数的文档。
         *
         * ## 特殊处理
         *
         * 如果原始元素是对枚举特殊函数的引用（如 `SomeEnum.values()`），
         * 会调用 [renderEnumSpecialFunction] 渲染特殊函数的文档。
         * 否则，渲染枚举类本身的文档。
         *
         * @param element 枚举类元素
         * @param originalElement 光标所在的原始元素
         * @param quickNavigation 是否为快速导航模式
         * @return HTML 格式的枚举文档
         */
        @NlsSafe
        private fun renderEnum(element: CjEnum, originalElement: PsiElement?, quickNavigation: Boolean): String {
            val referenceExpression = originalElement?.getNonStrictParentOfType<CjReferenceExpression>()
            if (referenceExpression != null) {
                // When caret on special enum function (e.g. SomeEnum.values<caret>())
                // element is not an CjReferenceExpression, but CjClass of enum
                // so reference extracted from originalElement
                val context = referenceExpression.safeAnalyzeNonSourceRootCode(BodyResolveMode.PARTIAL)
                (context[BindingContext.REFERENCE_TARGET, referenceExpression]
                    ?: referenceExpression.getChildOfType<CjReferenceExpression>()?.let {
                        context[BindingContext.REFERENCE_TARGET, it]
                    })
                    ?.let {
                        if (it is FunctionDescriptor) // To protect from Some<caret>Enum.values()
                            return renderEnumSpecialFunction(element, it, quickNavigation)
                    }
            }
            return renderCangJieDeclaration(element, quickNavigation)
        }

        /**
         * 构建仓颉声明的文档模板。
         *
         * 该方法是文档生成的核心，负责将 PSI 元素转换为包含完整信息的文档模板。
         *
         * ## 处理流程
         *
         * 1. 解析元素获取描述符
         * 2. 特殊处理模式变量声明
         * 3. 如果解析失败，返回错误模板
         * 4. 调用 [buildCangJie] 构建完整的文档模板
         *
         * @param declaration 要生成文档的声明表达式
         * @param quickNavigation 是否为快速导航模式
         * @return 文档模板对象
         *
         * @see buildCangJie
         * @see CDocTemplate
         */
        private fun buildCangJieDeclaration(declaration: CjExpression, quickNavigation: Boolean): CDocTemplate {
            val resolutionFacade = declaration.getResolutionFacade()
            val context = declaration.safeAnalyzeNonSourceRootCode(resolutionFacade, BodyResolveMode.PARTIAL)
            val declarationDescriptor = if (declaration is PatternVariableDeclaration) {
                declaration.variable?.let {
                    context[BindingContext.DECLARATION_TO_DESCRIPTOR, it]

                }
            } else {
                context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
            }

            if (declarationDescriptor == null) {
                LOG.info("Failed to find descriptor for declaration " + declaration.getElementTextWithContext())
                return CDocTemplate.NoDocTemplate().apply {
                    error {
                        append(CangJieCDocBundle.message("quick.doc.no.documentation"))
                    }
                }
            }

            return buildCangJie(context, declarationDescriptor, quickNavigation, declaration, resolutionFacade)
        }

        /**
         * 使用标签包装内容。
         *
         * @receiver StringBuilder 字符串构建器
         * @param tag HTML 标签名
         * @param body 要包装的内容
         */
        private inline fun StringBuilder.wrapTag(tag: String, crossinline body: () -> Unit) {
            wrap("<$tag>", "</$tag>", body)
        }

        /**
         * 使用前缀和后缀包装内容。
         *
         * @receiver StringBuilder 字符串构建器
         * @param prefix 前缀字符串
         * @param postfix 后缀字符串
         * @param body 要包装的内容
         */
        private inline fun StringBuilder.wrap(prefix: String, postfix: String, crossinline body: () -> Unit) {
            this.append(prefix)
            body()
            this.append(postfix)
        }

        /**
         * 对字符串进行 HTML 转义。
         *
         * @receiver String 要转义的字符串
         * @return 转义后的 HTML 安全字符串
         */
        private fun String.htmlEscape(): String = HtmlEscapers.htmlEscaper().escape(this)

        /**
         * 为文档模板插入弃用信息。
         *
         * 如果声明被标记为弃用，该方法会添加弃用消息和替换建议到文档中。
         *
         * ## 显示内容
         *
         * - **弃用消息**: 来自 `@Deprecated` 注解的 `message` 参数
         * - **替换建议**: 来自 `@Deprecated` 注解的 `replaceWith` 参数，显示为高亮代码
         *
         * @receiver CDocTemplate 文档模板
         * @param declarationDescriptor 声明描述符
         * @param deprecationResolver 弃用信息解析器
         * @param project 项目对象
         */
        private fun CDocTemplate.insertDeprecationInfo(
            declarationDescriptor: DeclarationDescriptor,
            deprecationResolver: DeprecationResolver,
            project: Project
        ) {
            val deprecationInfo = deprecationResolver.getDeprecations(declarationDescriptor).firstOrNull() ?: return

            deprecation {
                deprecationInfo.message?.let { message ->
                    append(SECTION_HEADER_START)
                    append(CangJieCDocBundle.message("quick.doc.section.deprecated"))
                    append(SECTION_SEPARATOR)
                    append(message.htmlEscape())
                    append(SECTION_END)
                }
                deprecationInfo.deprecatedByAnnotationReplaceWithExpression()?.let { replaceWith ->
                    append(SECTION_HEADER_START)
                    append(CangJieCDocBundle.message("quick.doc.section.replace.with"))
                    append(SECTION_SEPARATOR)
                    wrapTag("code") {
                        appendCodeSnippetHighlightedByLexer(project, replaceWith.htmlEscape())
                    }
                    append(SECTION_END)
                }
            }
        }

        /**
         * 构建仓颉声明的完整文档模板。
         *
         * 该方法是文档生成的核心实现，将描述符、CDoc 注释、容器信息等组合成完整的文档。
         *
         * ## 生成的文档结构
         *
         * 1. **定义部分**: 元素的完整声明（使用 [DESCRIPTOR_RENDERER]）
         * 2. **弃用信息**: 如果元素被标记为弃用，显示弃用消息和替换建议
         * 3. **描述部分**: CDoc 注释的主要内容和章节（如 `@param`, `@return` 等）
         * 4. **容器信息**: 元素所属的包、类等信息
         *
         * ## 特殊处理
         *
         * - **值参数**: 如果参数在绑定上下文中被标记为变量，使用变量描述符
         * - **次构造函数**: 如果次构造函数没有自己的文档，尝试使用所属类的文档
         *
         * @param context 绑定上下文，包含语义分析结果
         * @param declarationDescriptor 声明描述符
         * @param quickNavigation 是否为快速导航模式（不显示完整 CDoc 内容）
         * @param cjElement 仓颉 PSI 元素
         * @param resolutionFacade 解析门面，用于访问语义分析服务
         * @return 完整的文档模板对象
         *
         * @see CDocTemplate
         * @see DESCRIPTOR_RENDERER
         */
        @OptIn(FrontendInternals::class)
        private fun buildCangJie(
            context: BindingContext,
            declarationDescriptor: DeclarationDescriptor,
            quickNavigation: Boolean,
            cjElement: CjElement,
            resolutionFacade: ResolutionFacade,
        ): CDocTemplate {
            @Suppress("NAME_SHADOWING")
            var declarationDescriptor = declarationDescriptor
            if (declarationDescriptor is ValueParameterDescriptor) {
                val property = context[BindingContext.VALUE_PARAMETER_AS_VARIABLE, declarationDescriptor]
                if (property != null) {
                    declarationDescriptor = property
                }
            }

            @OptIn(FrontendInternals::class)
            val deprecationProvider = resolutionFacade.frontendService<DeprecationResolver>()

            return CDocTemplate().apply {
                definition {
                    renderDefinition(
                        declarationDescriptor, Lazy.DESCRIPTOR_RENDERER
                            .withIdeOptions { highlightingManager = createHighlightingManager(cjElement.project) }
                    )
                }

                insertDeprecationInfo(declarationDescriptor, deprecationProvider, cjElement.project)

                if (!quickNavigation) {
                    description {
                        declarationDescriptor.findCDoc {
                            DescriptorToSourceUtilsIde.getAnyDeclaration(
                                cjElement.project,
                                it
                            )
                        }?.let {
                            renderCDoc(it.contentTag, it.sections)
                            return@description
                        }
                        if (declarationDescriptor is ClassConstructorDescriptor && !declarationDescriptor.isPrimary) {
                            declarationDescriptor.constructedClass.findCDoc {
                                DescriptorToSourceUtilsIde.getAnyDeclaration(
                                    cjElement.project,
                                    it
                                )
                            }?.let {
                                renderCDoc(it.contentTag, it.sections)
                                return@description
                            }
                        }

                    }
                }

                getContainerInfo(cjElement)?.toString()?.takeIf { it.isNotBlank() }?.let { info ->
                    containerInfo {
                        append(info)
                    }
                }
            }
        }

        /**
         * 获取元素的容器信息。
         *
         * 该方法生成显示元素所属容器的 HTML 片段，包括：
         * - 所属的包或类（带图标和超链接）
         * - 源文件名（如果是包级声明）
         *
         * ## 示例输出
         *
         * ```html
         * <icon src="/icons/classCangJie.svg"/> com.example.MyClass
         * <icon src="icons/cangjie_file.svg"/> MyFile.cj
         * ```
         *
         * @param element PSI 元素
         * @return 容器信息的 HTML 片段；如果无法生成则返回 `null`
         */
        private fun getContainerInfo(element: PsiElement?): HtmlChunk? {
            if (element !is CjExpression) return null

            val resolutionFacade = element.getResolutionFacade()
            val context = element.safeAnalyzeNonSourceRootCode(resolutionFacade, BodyResolveMode.PARTIAL)
            val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, element] ?: return null
            if (DescriptorUtils.isLocal(descriptor)) return null

            val containingDeclaration = descriptor.containingDeclaration ?: return null

            val fqNameSection = containingDeclaration.fqNameSafe
                .takeUnless { it.isRoot }
                ?.let {
                    @Nls val link = StringBuilder().apply {
                        val highlighted =
                            if (DocumentationSettings.isSemanticHighlightingOfLinksEnabled()) highlight(
                                it.asString(),
                                element.project
                            ) { asClassName }
                            else it.asString()
                        DocumentationManagerUtil.createHyperlink(this, it.asString(), highlighted, false, false)
                    }
                    HtmlChunk.fragment(
                        HtmlChunk.tag("icon").attr("src", "/icons/classCangJie.svg"),
                        HtmlChunk.nbsp(),
                        HtmlChunk.raw(link.toString()),
                        HtmlChunk.br()
                    )
                }
                ?: HtmlChunk.empty()

            val fileNameSection = descriptor
                .safeAs<DeclarationDescriptorWithSource>()
                ?.source
                ?.containingFile
                ?.name
                ?.takeIf { containingDeclaration is PackageFragmentDescriptor }
                ?.let { fileName: @NlsSafe String ->
                    HtmlChunk.fragment(
                        HtmlChunk.tag("icon").attr("src", "icons/cangjie_file.svg"),
                        HtmlChunk.nbsp(),
                        HtmlChunk.text(fileName),
                        HtmlChunk.br()
                    )
                }
                ?: HtmlChunk.empty()

            return HtmlChunk.fragment(fqNameSection, fileNameSection)
        }

        /**
         * 渲染仓颉声明的文档。
         *
         * 该方法是文档渲染的便捷包装方法，将 [buildCangJieDeclaration] 生成的
         * 文档模板转换为 HTML 字符串。
         *
         * @param declaration 声明表达式
         * @param quickNavigation 是否为快速导航模式
         * @return HTML 格式的文档字符串
         */
        @NlsSafe
        private fun renderCangJieDeclaration(declaration: CjExpression, quickNavigation: Boolean) = buildString {
            insert(buildCangJieDeclaration(declaration, quickNavigation)) {}
        }

        /**
         * 获取元素的文档文本（内部实现）。
         *
         * 该方法是文档生成的统一入口，根据元素类型分发到不同的渲染方法。
         *
         * ## 支持的元素类型
         *
         * - **CjVArrayType**: 值数组类型
         * - **CjBasicType**: 基本类型
         * - **CjEnum**: 枚举类（包括特殊函数处理）
         * - **CjEnumEntry**: 枚举条目（显示序号信息）
         * - **CjDeclaration**: 一般声明（类、函数、属性等）
         * - **CjValueArgumentList**: 参数列表（解析到被调用的函数/构造器）
         * - **CjCallExpression**: 调用表达式（解析到被调用的元素）
         *
         * ## 特殊处理
         *
         * ### 接收者类型的 `this`
         * 当光标在扩展函数/属性的接收者类型上的 `this` 关键字时，
         * 显示整个扩展函数/属性的文档。
         *
         * ### 枚举特殊函数
         * 对于 `SomeEnum.values()` 这样的调用，显示特殊函数的文档。
         *
         * ### 枚举条目序号
         * 枚举条目的文档会额外显示序号信息。
         *
         * @param element 要生成文档的元素
         * @param originalElement 光标所在的原始元素（用于上下文推断）
         * @param quickNavigation 是否为快速导航模式
         * @return HTML 格式的文档字符串；如果无法生成则返回 `null`
         */
        @Nls
        private fun getTextImpl(element: PsiElement, originalElement: PsiElement?, quickNavigation: Boolean): String? {
            (element as? CjElement)?.navigationElement.takeIf { it != element }?.let {
                return getTextImpl(it, originalElement, quickNavigation)
            }

            if (element is CjVArrayType) {
                return CangJieCDocBundle.message("quick.doc.type.varray")
            }
            if (element is CjBasicType) {
                return CangJieCDocBundle.message("quick.doc.type.basic")
            }
            if (element is CjTypeReference) {
                val declaration = element.parent
                if (declaration is CjCallableDeclaration && declaration.receiverTypeReference == element) {
                    val thisElement = findElementWithText(originalElement, "this")
                    if (thisElement != null) {
                        return getTextImpl(declaration, originalElement, quickNavigation)
                    }
                }
            }

            if (element is CjEnum) {
                // When caret on special enum function (e.g. SomeEnum.values<caret>())
                // element is not an CjReferenceExpression, but CjClass of enum
                return renderEnum(element, originalElement, quickNavigation)
            } else if (element is CjEnumEntry && !quickNavigation) {
                val ordinal =
                    element.containingTypeStatement?.body?.run { getChildrenOfType<CjEnumEntry>().indexOf(element) }

                val project = element.project
                @Suppress("HardCodedStringLiteral")
                return buildString {
                    insert(buildCangJieDeclaration(element, quickNavigation = false)) {
                        definition {
                            it.inherit()
                            ordinal?.let {
                                append("<br>")
                                appendHighlighted("// ", project) { asInfo }
                                appendHighlighted(
                                    CangJieCDocBundle.message("quick.doc.text.enum.ordinal", ordinal),
                                    project
                                ) { asInfo }
                            }
                        }
                    }
                }
            } else if (element is CjDeclaration) {
                return renderCangJieDeclaration(element, quickNavigation)
            } /*else if (element is CjNameReferenceExpression && element.getReferencedNameAsName() == StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME) {
                return renderCangJieImplicitLambdaParameter(element, quickNavigation)
            }*/ else if (element is CjValueArgumentList) {
                val referenceExpression = element.prevSibling as? CjSimpleNameExpression ?: return null
                val calledElement = referenceExpression.mainReference.resolve()
                if (calledElement is CjNamedFunction || calledElement is CjConstructor<*>) { // In case of CangJie function or constructor
                    return renderCangJieDeclaration(calledElement as CjExpression, quickNavigation)
                }
            } else if (element is CjCallExpression) {
                val calledElement = element.referenceExpression()?.mainReference?.resolve()
                return calledElement?.let { getTextImpl(it, originalElement, quickNavigation) }
            }


            return null
        }

        /**
         * 获取元素的文档文本（公共包装方法）。
         *
         * 该方法是 [getTextImpl] 的简单包装，提供统一的访问接口。
         *
         * @param element 要生成文档的元素
         * @param originalElement 光标所在的原始元素
         * @param quickNavigation 是否为快速导航模式
         * @return HTML 格式的文档字符串；如果无法生成则返回 `null`
         *
         * @see getTextImpl
         */
        @Nls
        private fun getText(element: PsiElement, originalElement: PsiElement?, quickNavigation: Boolean) =
            getTextImpl(element, originalElement, quickNavigation)

    }
}
