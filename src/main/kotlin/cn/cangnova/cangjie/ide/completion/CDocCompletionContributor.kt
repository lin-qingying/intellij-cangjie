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

package cn.cangnova.cangjie.ide.completion

import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.doc.lexer.CDocTokens
import cn.cangnova.cangjie.doc.parser.CDocKnownTag
import cn.cangnova.cangjie.doc.psi.impl.CDocLink
import cn.cangnova.cangjie.doc.psi.impl.CDocName
import cn.cangnova.cangjie.ide.ExpectedInfo
import cn.cangnova.cangjie.psi.psiUtil.getParentOfType
import cn.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import cn.cangnova.cangjie.references.getCDocLinkMemberScope
import cn.cangnova.cangjie.references.getCDocLinkResolutionScope
import cn.cangnova.cangjie.references.getParamDescriptors
import cn.cangnova.cangjie.references.resolveCDocLink
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import cn.cangnova.cangjie.resolve.scopes.collectDescriptorsFiltered
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext

class CDocCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC, psiElement().inside(CDocName::class.java),
            CDocNameCompletionProvider
        )

        extend(
            CompletionType.BASIC,
            psiElement().afterLeaf(
                StandardPatterns.or(psiElement(CDocTokens.LEADING_ASTERISK), psiElement(CDocTokens.START))
            ),
            CDocTagCompletionProvider
        )

        extend(
            CompletionType.BASIC,
            psiElement(CDocTokens.TAG_NAME), CDocTagCompletionProvider
        )
    }
}

object CDocNameCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        CDocNameCompletionSession(parameters, result).complete()
    }
}

class CDocNameCompletionSession(
    parameters: CompletionParameters,
    resultSet: CompletionResultSet
) :
    CompletionSession(CompletionSessionConfiguration(parameters), parameters, resultSet) {

    override val descriptorKindFilter: DescriptorKindFilter? get() = null
    override val expectedInfos: Collection<ExpectedInfo> get() = emptyList()

    override fun doComplete() {
        val position = parameters.position.getParentOfType<CDocName>(false) ?: return
        val declaration = position.getContainingDoc().getOwner() ?: return
        val cdocLink = position.getStrictParentOfType<CDocLink>()!!
        val declarationDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] ?: return
        withCollectRequiredContextVariableTypes { lookupFactory ->
            if (cdocLink.getTagIfSubject()?.knownTag == CDocKnownTag.PARAM) {
                addParamCompletions(position, declarationDescriptor, lookupFactory)
            } else {
                addLinkCompletions(declarationDescriptor, cdocLink, lookupFactory)
            }
        }
    }


    private fun addParamCompletions(
        position: CDocName,
        declarationDescriptor: DeclarationDescriptor,
        lookupFactory: LookupElementFactory,
    ) {
        if (position.getQualifier() != null) return

        val section = position.getContainingSection()
        val documentedParameters = section.findTagsByName("param").map { it.getSubjectName() }.toSet()
        getParamDescriptors(declarationDescriptor)
            .filter { it.name.asString() !in documentedParameters }
            .forEach {
                collector.addElement(
                    lookupFactory.createLookupElement(
                        it,
                        useReceiverTypes = false,
                        parametersAndTypeGrayed = true
                    )
                )
            }
    }

    private fun collectDescriptorsForLinkCompletion(
        declarationDescriptor: DeclarationDescriptor,
        cDocLink: CDocLink
    ): Collection<DeclarationDescriptor> {
        val contextScope = getCDocLinkResolutionScope(resolutionFacade, declarationDescriptor)

        val qualifier = cDocLink.qualifier
        val nameFilter = descriptorNameFilter.toNameFilter()
        return if (qualifier.isNotEmpty()) {
            val parentDescriptors =
                resolveCDocLink(
                    bindingContext,
                    resolutionFacade,
                    declarationDescriptor,
                    cDocLink,
                    cDocLink.getTagIfSubject(),
                    qualifier
                )
            parentDescriptors.flatMap {
                val scope = getCDocLinkMemberScope(it, contextScope)
                scope.getContributedDescriptors(nameFilter = nameFilter)
            }
        } else {
            contextScope.collectDescriptorsFiltered(DescriptorKindFilter.ALL, nameFilter, changeNamesForAliased = true)
        }
    }

    private fun addLinkCompletions(
        declarationDescriptor: DeclarationDescriptor,
        cDocLink: CDocLink,
        lookupFactory: LookupElementFactory
    ) {
        collectDescriptorsForLinkCompletion(declarationDescriptor, cDocLink).forEach {
            val element = lookupFactory.createLookupElement(it, useReceiverTypes = true, parametersAndTypeGrayed = true)
            collector.addElement(
                // insert only plain name here, no qualifier/parentheses/etc.
                LookupElementDecorator.withDelegateInsertHandler(element, EmptyDeclarativeInsertHandler)
            )
        }
    }
}
