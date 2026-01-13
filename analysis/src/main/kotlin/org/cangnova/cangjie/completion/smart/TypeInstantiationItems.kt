/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.completion.smart

import org.cangnova.cangjie.utils.withRootPrefixIfNeeded
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*

import org.cangnova.cangjie.codeinsight.collectSyntheticStaticMembersAndConstructors
import org.cangnova.cangjie.completion.*
import org.cangnova.cangjie.completion.handlers.CangJieFunctionInsertHandler
import org.cangnova.cangjie.formatter.cangjieCustomSettings
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.references.util.DescriptorToSourceUtilsIde
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.sam.SamConstructorDescriptor
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.types.*

import org.cangnova.cangjie.utils.addIfNotNull
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.indices.CangJieIndicesHelper
import org.cangnova.cangjie.indices.ExpectedInfo
import org.cangnova.cangjie.indices.Tail
import org.cangnova.cangjie.indices.multipleFuzzyTypes
import org.cangnova.cangjie.quickfix.overrideImplement.ImplementMembersHandler
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.calls.util.CallType
import org.cangnova.cangjie.search.ClassInheritorsSearch

class TypeInstantiationItems(
    val resolutionFacade: ResolutionFacade,
    val bindingContext: BindingContext,
    val visibilityFilter: (DeclarationDescriptor) -> Boolean,
    val toFromOriginalFileMapper: ToFromOriginalFileMapper,
    val inheritorSearchScope: GlobalSearchScope,
    val lookupElementFactory: LookupElementFactory,
    private val forOrdinaryCompletion: Boolean,
    val indicesHelper: CangJieIndicesHelper
) {
    companion object {
        private val FUNCTIONS_OR_CLASSIFIERS_MASK =
            DescriptorKindFilter(DescriptorKindFilter.FUNCTIONS_MASK or DescriptorKindFilter.CLASSIFIERS_MASK)
    }

    fun addTo(
        items: MutableCollection<LookupElement>,
        inheritanceSearchers: MutableCollection<InheritanceItemsSearcher>,
        expectedInfos: Collection<ExpectedInfo>
    ) {
        val expectedInfosGrouped = LinkedHashMap<FuzzyType, MutableList<ExpectedInfo>>()
        for (expectedInfo in expectedInfos) {
            for (fuzzyType in expectedInfo.multipleFuzzyTypes) {
                expectedInfosGrouped.getOrPut(fuzzyType.unwrapOption()) { ArrayList() }.add(expectedInfo)
            }
        }

        for ((type, infos) in expectedInfosGrouped) {
            val tail = mergeTails(infos.map { it.tail })
            addTo(items, inheritanceSearchers, type, tail)
        }
    }

    private fun addTo(
        items: MutableCollection<LookupElement>,
        inheritanceSearchers: MutableCollection<InheritanceItemsSearcher>,
        fuzzyType: FuzzyType,
        tail: Tail?
    ) {
        if (fuzzyType.type.isFunctionType) return // do not show "object: ..." for function types

        val classifier =
            fuzzyType.type.constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters ?: return
        val classDescriptor = when (classifier) {
            is ClassDescriptor -> classifier
            is TypeAliasDescriptor -> classifier.classDescriptor
            else -> null
        }

        addSamConstructorItem(items, classifier, classDescriptor, tail)
        items.addIfNotNull(createTypeInstantiationItem(fuzzyType, classDescriptor, tail))

        indicesHelper.resolveTypeAliasesUsingIndex(fuzzyType.type, classifier.name.asString()).forEach {
            addSamConstructorItem(items, it, classDescriptor, tail)
            val typeAliasFuzzyType = it.defaultType.toFuzzyType(fuzzyType.freeParameters)
            items.addIfNotNull(createTypeInstantiationItem(typeAliasFuzzyType, classDescriptor, tail))
        }

        if (classDescriptor != null && !forOrdinaryCompletion && !CangJieBuiltIns.isAny(classDescriptor)) { // do not search inheritors of Any
            val typeArgs = fuzzyType.type.arguments
            inheritanceSearchers.addInheritorSearcher(
                classDescriptor,
                classDescriptor,
                typeArgs,
                fuzzyType.freeParameters,
                tail
            )


        }
    }

    private fun MutableCollection<InheritanceItemsSearcher>.addInheritorSearcher(
        descriptor: ClassDescriptor,
        cangjieClassDescriptor: ClassDescriptor,
        typeArgs: List<TypeArgument>,
        freeParameters: Collection<TypeParameterDescriptor>,
        tail: Tail?
    ) {
        val _declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(resolutionFacade.project, descriptor) ?: return
        val declaration = if (_declaration is CjDeclaration)
            toFromOriginalFileMapper.toOriginalFile(_declaration) ?: return
        else
            _declaration

        val psiClass: CjTypeStatement = when (declaration) {

            is CjTypeStatement -> declaration
            else -> return
        }
        add(InheritanceSearcher(psiClass, cangjieClassDescriptor, typeArgs, freeParameters, tail))
    }

    private fun createTypeInstantiationItem(
        fuzzyType: FuzzyType,
        classDescriptor: ClassDescriptor?,
        tail: Tail?
    ): LookupElement? {
        val classifier =
            fuzzyType.type.constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters ?: return null

        var lookupElement = lookupElementFactory.createLookupElement(classifier, useReceiverTypes = false)


        val isAbstract = classDescriptor?.modality == Modality.ABSTRACT
        if (forOrdinaryCompletion && isAbstract) return null

        val allConstructors = classifier.constructors
        val visibleConstructors = allConstructors.filter {
            if (isAbstract)
                visibilityFilter(it) || it.visibility == DescriptorVisibilities.PROTECTED
            else
                visibilityFilter(it)
        }
        if (allConstructors.isNotEmpty() && visibleConstructors.isEmpty()) return null

        var lookupString = lookupElement.lookupString
        var allLookupStrings = setOf(lookupString)
        var itemText = lookupString
        var signatureText: String? = null
        val typeText = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(classifier)

        val insertHandler: InsertHandler<LookupElement>
        if (isAbstract) {
            val typeArgs = fuzzyType.type.arguments
            // drop "in" and "out" from type arguments - they cannot be used in constructor call
            val typeArgsToUse = typeArgs.map { TypeArgumentImpl(  it.type) }

            val allTypeArgsKnown =
                fuzzyType.freeParameters.isEmpty() || typeArgs.none { it.type.areTypeParametersUsedInside(fuzzyType.freeParameters) }
            itemText += if (allTypeArgsKnown) {
                IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderTypeArguments(typeArgsToUse)
            } else {
                "<...>"
            }

            val constructorParenthesis = if (classifier.kind != ClassKind.INTERFACE) "()" else ""
            itemText += constructorParenthesis
            itemText = "object : $itemText{...}"
            lookupString = "object"
            allLookupStrings = setOf(lookupString, lookupElement.lookupString)
            insertHandler = InsertHandler<LookupElement> { context, _ ->
                val startOffset = context.startOffset

                val settings = context.file.cangjieCustomSettings
                val spaceBefore = if (settings.SPACE_BEFORE_EXTEND_COLON) " " else ""
                val spaceAfter = if (settings.SPACE_AFTER_EXTEND_COLON) " " else ""
                val text1 = "object$spaceBefore:$spaceAfter$typeText"
                val text2 = "$constructorParenthesis {}"
                val text = if (allTypeArgsKnown)
                    text1 + IdeDescriptorRenderers.SOURCE_CODE.renderTypeArguments(typeArgsToUse) + text2
                else
                    "$text1<>$text2"

                context.document.replaceString(startOffset, context.tailOffset, text)

                if (allTypeArgsKnown) {
                    context.editor.caretModel.moveToOffset(startOffset + text.length - 1)

                    shortenReferences(context, startOffset, startOffset + text.length)

                    ImplementMembersHandler().invoke(context.project, context.editor, context.file, true)
                } else {
                    context.editor.caretModel.moveToOffset(startOffset + text1.length + 1) // put caret into "<>"

                    shortenReferences(context, startOffset, startOffset + text.length)
                }
            }
            lookupElement = lookupElement.suppressAutoInsertion()
            lookupElement = lookupElement.assignSmartCompletionPriority(SmartCompletionItemPriority.ANONYMOUS_OBJECT)
        } else {
            //TODO: when constructor has one parameter of lambda type with more than one parameter, generate special additional item
            signatureText = when (visibleConstructors.size) {
                0 -> "()"

                1 -> {
                    val constructor = visibleConstructors.single()
                    val substitutor = ComposableTypeSubstitutor.create(fuzzyType.presentationType())
                    val substitutedConstructor = constructor.substitute(substitutor)
                        ?: constructor // render original signature if failed to substitute
                    BasicLookupElementFactory.SHORT_NAMES_RENDERER.renderFunctionParameters(substitutedConstructor)
                }

                else -> "(...)"
            }

            val baseInsertHandler = when (visibleConstructors.size) {
                0 -> CangJieFunctionInsertHandler.Normal(
                    CallType.DEFAULT,
                    inputTypeArguments = false,
                    inputValueArguments = false,
                    argumentsOnly = true
                )

                1 -> lookupElementFactory.insertHandlerProvider.insertHandler(
                    visibleConstructors.single(),
                    argumentsOnly = true
                )

                else -> CangJieFunctionInsertHandler.Normal(
                    CallType.DEFAULT,
                    inputTypeArguments = false,
                    inputValueArguments = true,
                    argumentsOnly = true
                )
            }

            insertHandler = InsertHandler { context, item ->
                val insertText = FqName(typeText).withRootPrefixIfNeeded(null).asString()
                context.document.replaceString(context.startOffset, context.tailOffset, insertText)
                context.tailOffset = context.startOffset + insertText.length

                baseInsertHandler.handleInsert(context, item)

                shortenReferences(context, context.startOffset, context.tailOffset)
            }

            run {
                val (inputValueArgs, isLambda) = when (baseInsertHandler) {
                    is CangJieFunctionInsertHandler.Normal -> baseInsertHandler.inputValueArguments to (baseInsertHandler.lambdaInfo != null)

                    else -> false to false
                }
                if (inputValueArgs) {
                    lookupElement = lookupElement.keepOldArgumentListOnTab()
                }
                if (isLambda) {
                    lookupElement.acceptOpeningBrace = true
                }
            }
            lookupElement = lookupElement.assignSmartCompletionPriority(SmartCompletionItemPriority.INSTANTIATION)
        }

        class InstantiationLookupElement : LookupElementDecorator<LookupElement>(lookupElement) {
            override fun getLookupString() = lookupString

            override fun getAllLookupStrings() = allLookupStrings

            override fun renderElement(presentation: LookupElementPresentation) {
                delegate.renderElement(presentation)
                presentation.itemText = itemText

                presentation.clearTail()
                signatureText?.let {
                    presentation.appendTailText(it, false)
                }
                presentation.appendTailText(
                    " (" + DescriptorUtils.getFqName(classifier.containingDeclaration) + ")",
                    true
                )
            }

            override fun getDelegateInsertHandler() = insertHandler

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is InstantiationLookupElement) return false
                if (getLookupString() != other.lookupString) return false
                val presentation1 = LookupElementPresentation()
                val presentation2 = LookupElementPresentation()
                renderElement(presentation1)
                other.renderElement(presentation2)
                return presentation1.itemText == presentation2.itemText && presentation1.tailText == presentation2.tailText
            }

            override fun hashCode() = lookupString.hashCode()
        }

        return InstantiationLookupElement().addTail(tail)
    }

    private fun CangJieType.areTypeParametersUsedInside(freeParameters: Collection<TypeParameterDescriptor>): Boolean {
        return FuzzyType(this, freeParameters).freeParameters.isNotEmpty()
    }

    private fun addSamConstructorItem(
        collection: MutableCollection<LookupElement>,
        classifier: ClassifierDescriptorWithTypeParameters,
        classDescriptor: ClassDescriptor?,
        tail: Tail?
    ) {
        if (classDescriptor?.kind == ClassKind.INTERFACE) {
            val samConstructor = run {
                val scope = when (val container = classifier.containingDeclaration) {
                    is PackageFragmentDescriptor -> container.getMemberScope()
                    is ClassDescriptor -> container.unsubstitutedMemberScope
                    else -> return
                }
                scope.collectSyntheticStaticMembersAndConstructors(
                    resolutionFacade,
                    FUNCTIONS_OR_CLASSIFIERS_MASK
                ) { classifier.name == it }
                    .filterIsInstance<SamConstructorDescriptor>()
                    .singleOrNull() ?: return
            }
            lookupElementFactory
                .createStandardLookupElementsForDescriptor(samConstructor, useReceiverTypes = false)
                .mapTo(collection) {
                    it.assignSmartCompletionPriority(SmartCompletionItemPriority.INSTANTIATION).addTail(tail)
                }
        }
    }

    private inner class InheritanceSearcher(
        private val psiClass: CjTypeStatement,
        classDescriptor: ClassDescriptor,
        typeArgs: List<TypeArgument>,
        private val freeParameters: Collection<TypeParameterDescriptor>,
        private val tail: Tail?
    ) : InheritanceItemsSearcher {

        private val baseHasTypeArgs = classDescriptor.declaredTypeParameters.isNotEmpty()
        private val expectedType = CangJieTypeFactory.simpleNonOptionType(TypeAttributes.Empty, classDescriptor, typeArgs)
        private val expectedFuzzyType = expectedType.toFuzzyType(freeParameters)

        override fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit) {
            val parameters =
                ClassInheritorsSearch.SearchParameters(psiClass, inheritorSearchScope, true, true, false, nameFilter)
            for (inheritor in ClassInheritorsSearch.search(parameters)) {
                val descriptor = inheritor.resolveToDescriptor(
                    resolutionFacade
                ) { toFromOriginalFileMapper.toSyntheticFile(it) } ?: continue
                if (!visibilityFilter(descriptor)) continue

                var inheritorFuzzyType = descriptor.defaultType.toFuzzyType(descriptor.typeConstructor.parameters)
                val hasTypeArgs = descriptor.declaredTypeParameters.isNotEmpty()
                if (hasTypeArgs || baseHasTypeArgs) {
                    val substitutor = inheritorFuzzyType.checkIsSubtypeOf(expectedFuzzyType) ?: continue
                    if (!substitutor.isEmpty) {
                        val inheritorTypeSubstituted =
                            substitutor.substitute(inheritorFuzzyType.type )!!
                        inheritorFuzzyType =
                            inheritorTypeSubstituted.toFuzzyType(freeParameters + inheritorFuzzyType.freeParameters)
                    }
                }

                val lookupElement = createTypeInstantiationItem(inheritorFuzzyType, descriptor, tail) ?: continue
                consumer(lookupElement.assignSmartCompletionPriority(SmartCompletionItemPriority.INHERITOR_INSTANTIATION))
            }
        }
    }
}

fun CjTypeStatement.resolveToDescriptor(
    resolutionFacade: ResolutionFacade,
    declarationTranslator: (CjTypeStatement) -> CjTypeStatement? = { it }
): ClassDescriptor? {

    val declaration = declarationTranslator(this) ?: return null
    return resolutionFacade.resolveToDescriptor(declaration)
            as? ClassDescriptor
}
