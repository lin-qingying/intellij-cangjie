package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.doc.lexer.CDocTokens
import com.huawei.cangjie.doc.parser.CDocKnownTag
import com.huawei.cangjie.doc.psi.impl.CDocLink
import com.huawei.cangjie.doc.psi.impl.CDocName
import com.huawei.cangjie.ide.ExpectedInfo
import com.huawei.cangjie.psi.psiUtil.getParentOfType
import com.huawei.cangjie.psi.psiUtil.getStrictParentOfType
import com.huawei.cangjie.references.getCDocLinkMemberScope
import com.huawei.cangjie.references.getCDocLinkResolutionScope
import com.huawei.cangjie.references.getParamDescriptors
import com.huawei.cangjie.references.resolveCDocLink
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.scopes.DescriptorKindFilter
import com.huawei.cangjie.resolve.scopes.collectDescriptorsFiltered
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
