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

package org.cangnova.cangjie.completion


import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.completion.handlers.CastReceiverInsertHandler
import org.cangnova.cangjie.completion.keywords.KeywordLookupObject
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.endOffset
import org.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import org.cangnova.cangjie.psi.psiUtil.startOffset
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.scopes.getResolutionScope
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.utils.getImplicitReceiversWithInstanceToExpression
import org.cangnova.cangjie.utils.safeAs
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.OffsetKey
import com.intellij.codeInsight.completion.OffsetMap
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import org.cangnova.cangjie.codeinsight.ShortenReferences
import org.cangnova.cangjie.diagnostics.rendering.IdeDescriptorRenderers
import org.cangnova.cangjie.icon.CangJieIcons
import org.cangnova.cangjie.imports.ImportInsertHelper
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.types.TypeOptionality
import org.cangnova.cangjie.types.isFunctionType
import org.cangnova.cangjie.types.optionality
import org.cangnova.cangjie.utils.ImportableFqNameClassifier
import org.cangnova.cangjie.utils.importableFqName

val KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY = Key<Unit>("KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY")

class ThisItemLookupObject(val receiverParameter: ReceiverParameterDescriptor, val labelName: Name?) :
    KeywordLookupObject()

fun thisExpressionItems(
    bindingContext: BindingContext,
    position: CjExpression,
    prefix: String,
    resolutionFacade: ResolutionFacade
): Collection<ThisItemLookupObject> {
    val scope = position.getResolutionScope(bindingContext, resolutionFacade)

    val psiFactory = CjPsiFactory(resolutionFacade.project)

    val result = ArrayList<ThisItemLookupObject>()
    for ((receiver, expressionFactory) in scope.getImplicitReceiversWithInstanceToExpression()) {
        if (expressionFactory == null) continue
        // if prefix does not start with "this@" do not include immediate this in the form with label
        val expression =
            expressionFactory.createExpression(psiFactory, shortThis = !prefix.startsWith("this@")) as? CjThisExpression
                ?: continue
        result.add(ThisItemLookupObject(receiver, expression.getLabelNameAsName()))
    }
    return result
}

fun ThisItemLookupObject.createLookupElement() =
    createKeywordElement("this", labelName.labelNameToTail(), lookupObject = this)
        .withTypeText(BasicLookupElementFactory.SHORT_NAMES_RENDERER.renderType(receiverParameter.type))

fun Name?.labelNameToTail(): String = if (this != null) "@" + render() else ""

fun OffsetMap.tryGetOffset(key: OffsetKey): Int? {
    try {
        if (!containsOffset(key)) return null
        return getOffset(key).takeIf { it != -1 } // prior to IDEA 2016.3 getOffset() returned -1 if not found, now it throws exception
    } catch (e: Exception) {
        return null
    }
}

val STATISTICS_INFO_CONTEXT_KEY = Key<String>("STATISTICS_INFO_CONTEXT_KEY")

val DeclarationDescriptor.isArtificialImportAliasedDescriptor: Boolean
    get() = original.name != name
val NOT_IMPORTED_KEY = Key<Unit>("NOT_IMPORTED_KEY")
val CALLABLE_WEIGHT_KEY = Key<CallableWeight>("CALLABLE_WEIGHT_KEY")
fun ((String) -> Boolean).toNameFilter(): NameFilter {
    return { name -> !name.isSpecial && this(name.identifier) }
}
typealias NameFilter = (Name) -> Boolean

enum class CallableWeightEnum {
    local, // local non-extension
    thisClassMember,  // 当前类的成员
    baseClassMember,  // 基类的成员
    thisTypeExtension,  // 当前类型的 extend 成员（精确类型匹配）
    baseTypeExtension,  // 基类型的 extend 成员

    globalOrStatic, // global non-extension
    receiverCastRequired
}

class CallableWeight(val enum: CallableWeightEnum, val receiverIndex: Int?) {
    companion object {
        val local = CallableWeight(CallableWeightEnum.local, null)
        val globalOrStatic = CallableWeight(CallableWeightEnum.globalOrStatic, null)
        val receiverCastRequired = CallableWeight(CallableWeightEnum.receiverCastRequired, null)
    }
}

fun InsertionContext.isAfterDot(): Boolean {
    var offset = startOffset
    val chars = document.charsSequence
    while (offset > 0) {
        offset--
        val c = chars[offset]
        if (!Character.isWhitespace(c)) {
            return c == '.'
        }
    }
    return false
}

fun shortenReferences(
    context: InsertionContext,
    startOffset: Int,
    endOffset: Int,
    shortenReferences: ShortenReferences = ShortenReferences.DEFAULT
) {
    PsiDocumentManager.getInstance(context.project).commitDocument(context.document)
    val file = context.file as CjFile
    val element = file.findElementAt(startOffset)?.parentsWithSelf?.find {
        it.startOffset == startOffset && it.endOffset == endOffset
    }?.safeAs<CjElement>()

    if (element != null)
        shortenReferences.process(element)
    else
        shortenReferences.process(file, startOffset, endOffset)
}

private fun CjDeclarationWithBody.returnType(bindingContext: BindingContext): CangJieType? {
    val callable = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, this] as? CallableDescriptor ?: return null
    return callable.returnType
}


fun returnExpressionItems(bindingContext: BindingContext, position: CjElement): Collection<LookupElement> {
    val result = mutableListOf<LookupElement>()

    for (parent in position.parentsWithSelf.filterIsInstance<CjDeclarationWithBody>()) {
        val returnType = parent.returnType(bindingContext)
        val isUnit = returnType == null || CangJieBuiltIns.isUnit(returnType)

        if (parent.hasBlockBody()) {
            val blockBodyReturns = mutableListOf<LookupElement>()
            blockBodyReturns.add(createKeywordElementWithSpace("return", addSpaceAfter = !isUnit))

            if (returnType != null) {
                if (returnType.optionality() == TypeOptionality.OPTIONAL) {
                    blockBodyReturns.add(createKeywordElement("return None"))
                }


                if (CangJieBuiltIns.isBoolean(returnType)) {
                    blockBodyReturns.add(createKeywordElement("return true"))
                    blockBodyReturns.add(createKeywordElement("return false"))
                }
                //                    else if (emptyListShouldBeSuggested()) {
//                        blockBodyReturns.add(createKeywordElement("return", tail = " emptyList()"))
//                    } else if (CangJieBuiltIns.isSetOrNullableSet(returnType)) {
//                        blockBodyReturns.add(createKeywordElement("return", tail = " emptySet()"))
//                    }
//                fun emptyListShouldBeSuggested(): Boolean = CangJieBuiltIns.isCollectionOrNullableCollection(returnType)
//                        || CangJieBuiltIns.isListOrNullableList(returnType)
//                        || CangJieBuiltIns.isIterableOrNullableIterable(returnType)

            }

//            if (isLikelyInPositionForReturn(position, parent, isUnit)) {
//                blockBodyReturns.forEach { it.keywordProbability = KeywordProbability.HIGH }
//            }

            result.addAll(blockBodyReturns)
        }
        break

    }

    return result
}

fun LookupElement.assignPriority(priority: ItemPriority): LookupElement {
    this.priority = priority
    return this
}

private val CangJieType.fqType: String get() = IdeDescriptorRenderers.FQ_NAMES_IN_TYPES_WITH_NORMALIZER.renderType(this)

private open class BaseTypeLookupElement(type: CangJieType, baseLookupElement: LookupElement) :
    LookupElementDecorator<LookupElement>(baseLookupElement) {
    private val fullText = type.fqType

    override fun equals(other: Any?) = other is BaseTypeLookupElement && fullText == other.fullText
    override fun hashCode() = fullText.hashCode()

    override fun getDelegateInsertHandler(): InsertHandler<LookupElement> = InsertHandler { context, _ ->
        context.document.replaceString(context.startOffset, context.tailOffset, fullText)
        context.tailOffset = context.startOffset + fullText.length
        shortenReferences(context, context.startOffset, context.tailOffset)
    }
}

fun BasicLookupElementFactory.createLookupElementForType(type: CangJieType): LookupElement? {
    if (type.isError) return null

    return if (type.isFunctionType) {
        val text = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)
        val baseLookupElement = LookupElementBuilder.create(text).withIcon(CangJieIcons.LAMBDA)
        BaseTypeLookupElement(type, baseLookupElement)
    } else {
        val classifier = type.constructor.declarationDescriptor ?: return null
        val baseLookupElement =
            createLookupElement(classifier, qualifyNestedClasses = true, includeClassTypeArguments = false)

        // if type is simply classifier without anything else, use classifier's lookup element to avoid duplicates (works after "as" in basic completion)
        if (type.fqType == IdeDescriptorRenderers.FQ_NAMES_IN_TYPES_WITH_NORMALIZER.renderClassifierName(classifier))
            baseLookupElement
        else {
            val itemText = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)
            object : BaseTypeLookupElement(type, baseLookupElement) {
                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    presentation.itemText = itemText
                }
            }
        }
    }
}

fun LookupElement.withReceiverCast(): LookupElement =
    LookupElementDecorator.withDelegateInsertHandler(this) { context, element ->
        element.handleInsert(context)
        CastReceiverInsertHandler.postHandleInsert(context, element)
    }

infix fun <T> ((T) -> Boolean).or(otherFilter: (T) -> Boolean): (T) -> Boolean = { this(it) || otherFilter(it) }

fun ImportableFqNameClassifier.isImportableDescriptorImported(descriptor: DeclarationDescriptor): Boolean {
    val classification = classify(descriptor.importableFqName!!, false)
    return classification != ImportableFqNameClassifier.Classification.notImported
            && classification != ImportableFqNameClassifier.Classification.siblingImported
}

fun LookupElement.decorateAsStaticMember(
    memberDescriptor: DeclarationDescriptor,
    classNameAsLookupString: Boolean
): LookupElement? {
    val container = memberDescriptor.containingDeclaration as? ClassDescriptor ?: return null

    val containerFqName = container.importableFqName ?: return null
    val qualifierPresentation = container.name.asString()

    return object : LookupElementDecorator<LookupElement>(this) {

        override fun getAllLookupStrings(): Set<String> {
            return if (classNameAsLookupString) setOf(
                delegate.lookupString,
                qualifierPresentation
            ) else super.getAllLookupStrings()
        }

        override fun renderElement(presentation: LookupElementPresentation) {
            delegate.renderElement(presentation)



            val tailText = " (" + DescriptorUtils.getFqName(container.containingDeclaration) + ")"
            if (memberDescriptor is FunctionDescriptor) {
                presentation.appendTailText(tailText, true)
            } else {
                presentation.setTailText(tailText, true)
            }

            if (presentation.typeText.isNullOrEmpty()) {
                presentation.typeText = BasicLookupElementFactory.SHORT_NAMES_RENDERER.renderType(container.defaultType)
            }
        }

        override fun handleInsert(context: InsertionContext) {
            val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
            val file = context.file as CjFile

            fun importFromSameParentIsPresent() = file.importDirectives.flatMap { it.importItems }.any {
                !it.isAllUnder && it.importPath?.fqName?.parent() == containerFqName
            }

            val addMemberImport =  importFromSameParentIsPresent()

            if (addMemberImport) {
                psiDocumentManager.commitDocument(context.document)
                ImportInsertHelper.getInstance(context.project).importDescriptor(file, memberDescriptor)
                psiDocumentManager.doPostponedOperationsAndUnblockDocument(context.document)
            }

            super.handleInsert(context)
        }
    }
}

fun LookupElement.keepOldArgumentListOnTab(): LookupElement {
    putUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY, Unit)
    return this
}

var LookupElement.acceptOpeningBrace: Boolean by NotNullableUserDataProperty(
    Key("CANGJIE_ACCEPT_OPENING_BRACE"),
    defaultValue = false,
)
