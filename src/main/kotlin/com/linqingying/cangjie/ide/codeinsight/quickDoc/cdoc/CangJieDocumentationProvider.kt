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

package com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc

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
import com.linqingying.cangjie.CangJieBundle
import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.builtins.fqNameUnsafe
import com.linqingying.cangjie.configurable.services.CangJieLanguageServerServices
import com.linqingying.cangjie.configurable.services.Feature
import com.linqingying.cangjie.configurable.services.LanugageServerType
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.doc.CDocTemplate
import com.linqingying.cangjie.doc.insert
import com.linqingying.cangjie.doc.psi.CDoc
import com.linqingying.cangjie.doc.psi.impl.CDocSection
import com.linqingying.cangjie.ide.FrontendInternals
import com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc.CDocRenderer.appendCodeSnippetHighlightedByLexer
import com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc.CDocRenderer.appendHighlighted
import com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc.CDocRenderer.createHighlightingManager
import com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc.CDocRenderer.highlight
import com.linqingying.cangjie.ide.codeinsight.quickDoc.cdoc.CDocRenderer.renderCDoc
import com.linqingying.cangjie.ide.completion.DescriptorBasedDeclarationLookupObject
import com.linqingying.cangjie.ide.navigation.SourceNavigationHelper
import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.*
import com.linqingying.cangjie.references.mainReference
import com.linqingying.cangjie.references.resolveCDocLink
import com.linqingying.cangjie.references.util.DescriptorToSourceUtilsIde
import com.linqingying.cangjie.renderer.*
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.caches.getResolutionFacade
import com.linqingying.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.linqingying.cangjie.resolve.caches.safeAnalyzeNonSourceRootCode
import com.linqingying.cangjie.resolve.deprecation.DeprecationResolver
import com.linqingying.cangjie.resolve.deprecation.deprecatedByAnnotationReplaceWithExpression
import com.linqingying.cangjie.resolve.descriptorUtil.getSuperClassNotAny
import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
import com.linqingying.cangjie.resolve.source.getPsi
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.isDefinitelyNotNullType
import com.linqingying.cangjie.utils.safeAs
import org.jetbrains.annotations.Nls
import java.util.function.Consumer


class HtmlClassifierNamePolicy(val base: ClassifierNamePolicy) : ClassifierNamePolicyEx {

    override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String =
        render(classifier, renderer, null)

    override fun renderClassifierWithType(
        classifier: ClassifierDescriptor,
        renderer: DescriptorRenderer,
        type: CangJieType
    ): String =
        render(classifier, renderer, type)

    private fun render(classifier: ClassifierDescriptor, renderer: DescriptorRenderer, type: CangJieType?): String {

        val name =
            base.renderClassifier(classifier, renderer) + (type?.takeIf { it.isDefinitelyNotNullType }?.let { " & Any" }
                ?: "")

        if (classifier.isBoringBuiltinClass())
            return name
        return buildString {
            val ref = classifier.fqNameUnsafe.toString()
            DocumentationManagerUtil.createHyperlink(this, ref, name, true)
        }
    }
}

private val boringBuiltinClasses = setOf(
    StandardNames.FqNames.unitUFqName,
    StandardNames.FqNames.int8UFqName,
    StandardNames.FqNames.int16UFqName,
    StandardNames.FqNames.int32UFqName,
    StandardNames.FqNames.int64UFqName,
    StandardNames.FqNames.runeUFqName,
    StandardNames.FqNames.boolUFqName,
    StandardNames.FqNames.float16UFqName,

    StandardNames.FqNames.float32UFqName,

    StandardNames.FqNames.float64UFqName,


    )

fun ClassifierDescriptor.isBoringBuiltinClass(): Boolean = DescriptorUtils.getFqName(this) in boringBuiltinClasses

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

    @Nls
    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        if (!CangJieLanguageServerServices.getInstance()
                .isFeatureEnabled(LanugageServerType.AST_ANALYZER, Feature.HOVER_INFO)
        ) {
            return null
        }
        return getText(element, originalElement, false)
    }

    override fun canPromptToConfigureDocumentation(element: PsiElement?): Boolean {
        return false

    }

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

    override fun collectDocComments(file: PsiFile, sink: Consumer<in PsiDocCommentBase>) {
        if (!CangJieLanguageServerServices.getInstance()
                .isFeatureEnabled(LanugageServerType.AST_ANALYZER, Feature.HOVER_INFO)
        ) {
            return
        }
        if (file !is CjFile) return

        PsiTreeUtil.processElements(file) {
            val comment = (it as? CjDeclaration)?.docComment
            if (comment != null) sink.accept(comment)
            true
        }
    }


    override fun getDocumentationElementForLookupItem(
        psiManager: PsiManager,
        `object`: Any?,
        element: PsiElement?
    ): PsiElement? {
        if (`object` is DescriptorBasedDeclarationLookupObject) {
            `object`.psiElement?.let { return it }
            `object`.descriptor?.let { descriptor ->
                return DescriptorToSourceUtilsIde.getAnyDeclaration(psiManager.project, descriptor)
            }
        }
        return null
    }

    @Nls
    override fun generateRenderedDoc(comment: PsiDocCommentBase): String? {
        val docComment = comment as? CDoc ?: return null

        val result = StringBuilder().also {
            it.renderCDoc(docComment.getDefaultSection(), docComment.getAllSections())
        }

        @Suppress("HardCodedStringLiteral")
        return result.toString()
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ): PsiElement? {
        return if (contextElement.isModifier()) contextElement else null
    }

    @Nls
    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        return if (element == null) null else getText(element, originalElement, true)
    }

    override fun promptToConfigureDocumentation(element: PsiElement?) {

    }

    private object Lazy {

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

    companion object {
        private fun findElementWithText(element: PsiElement?, text: String): PsiElement? {
            return when {
                element == null -> null
                element.text == text -> element
                element.prevLeaf()?.text == text -> element.prevLeaf()
                else -> null
            }
        }

        private val LOG = Logger.getInstance(CangJieDocumentationProvider::class.java)

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

        private fun StringBuilder.renderDefinition(descriptor: DeclarationDescriptor, renderer: DescriptorRenderer) {
            append(renderer.render(descriptor))
        }

        private fun PsiElement?.isModifier() =
            this != null && parent is CjModifierList && CjTokens.MODIFIER_KEYWORDS_ARRAY.firstOrNull { it.value == text } != null

        @NlsSafe
        private fun renderEnum(element: CjEnum, originalElement: PsiElement?, quickNavigation: Boolean): String {
            val referenceExpression = originalElement?.getNonStrictParentOfType<CjReferenceExpression>()
            if (referenceExpression != null) {
                // When caret on special enum function (e.g. SomeEnum.values<caret>())
                // element is not an CjReferenceExpression, but CjClass of enum
                // so reference extracted from originalElement
                val context = referenceExpression.safeAnalyzeNonSourceRootCode(BodyResolveMode.PARTIAL)
                (context[BindingContext.REFERENCE_TARGET, referenceExpression]
                    ?: context[BindingContext.REFERENCE_TARGET, referenceExpression.getChildOfType<CjReferenceExpression>()])?.let {
                    if (it is FunctionDescriptor) // To protect from Some<caret>Enum.values()
                        return renderEnumSpecialFunction(element, it, quickNavigation)
                }
            }
            return renderCangJieDeclaration(element, quickNavigation)
        }

        private fun buildCangJieDeclaration(declaration: CjExpression, quickNavigation: Boolean): CDocTemplate {
            val resolutionFacade = declaration.getResolutionFacade()
            val context = declaration.safeAnalyzeNonSourceRootCode(resolutionFacade, BodyResolveMode.PARTIAL)
            val declarationDescriptor = if (declaration is PatternVariableDeclaration) {
                context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration.variable]
            } else {
                context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
            }

            if (declarationDescriptor == null) {
                LOG.info("Failed to find descriptor for declaration " + declaration.getElementTextWithContext())
                return CDocTemplate.NoDocTemplate().apply {
                    error {
                        append(CangJieBundle.message("quick.doc.no.documentation"))
                    }
                }
            }

            return buildCangJie(context, declarationDescriptor, quickNavigation, declaration, resolutionFacade)
        }

        private inline fun StringBuilder.wrapTag(tag: String, crossinline body: () -> Unit) {
            wrap("<$tag>", "</$tag>", body)
        }

        private inline fun StringBuilder.wrap(prefix: String, postfix: String, crossinline body: () -> Unit) {
            this.append(prefix)
            body()
            this.append(postfix)
        }

        private fun String.htmlEscape(): String = HtmlEscapers.htmlEscaper().escape(this)

        private fun CDocTemplate.insertDeprecationInfo(
            declarationDescriptor: DeclarationDescriptor,
            deprecationResolver: DeprecationResolver,
            project: Project
        ) {
            val deprecationInfo = deprecationResolver.getDeprecations(declarationDescriptor).firstOrNull() ?: return

            deprecation {
                deprecationInfo.message?.let { message ->
                    append(SECTION_HEADER_START)
                    append(CangJieBundle.message("quick.doc.section.deprecated"))
                    append(SECTION_SEPARATOR)
                    append(message.htmlEscape())
                    append(SECTION_END)
                }
                deprecationInfo.deprecatedByAnnotationReplaceWithExpression()?.let { replaceWith ->
                    append(SECTION_HEADER_START)
                    append(CangJieBundle.message("quick.doc.section.replace.with"))
                    append(SECTION_SEPARATOR)
                    wrapTag("code") {
                        appendCodeSnippetHighlightedByLexer(project, replaceWith.htmlEscape())
                    }
                    append(SECTION_END)
                }
            }
        }

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

        @NlsSafe
        private fun renderCangJieDeclaration(declaration: CjExpression, quickNavigation: Boolean) = buildString {
            insert(buildCangJieDeclaration(declaration, quickNavigation)) {}
        }

        @Nls
        private fun getTextImpl(element: PsiElement, originalElement: PsiElement?, quickNavigation: Boolean): String? {
            (element as? CjElement)?.navigationElement.takeIf { it != element }?.let {
                return getTextImpl(it, originalElement, quickNavigation)
            }

            if (element is CjVArrayType) {
                return "值数组"
            }
            if (element is CjBasicType) {
                return "基本类型"
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
                                    CangJieBundle.message("quick.doc.text.enum.ordinal", ordinal),
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

        @Nls
        private fun getText(element: PsiElement, originalElement: PsiElement?, quickNavigation: Boolean) =
            getTextImpl(element, originalElement, quickNavigation)

    }
}
