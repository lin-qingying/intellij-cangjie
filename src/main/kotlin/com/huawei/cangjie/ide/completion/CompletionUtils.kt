package com.huawei.cangjie.ide.completion

import com.huawei.cangjie.builtins.isFunctionType
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ReceiverParameterDescriptor
import com.huawei.cangjie.icon.CangJieIcons
import com.huawei.cangjie.ide.IdeDescriptorRenderers
import com.huawei.cangjie.ide.ShortenReferences
import com.huawei.cangjie.ide.completion.keywords.KeywordLookupObject
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.endOffset
import com.huawei.cangjie.psi.psiUtil.parentsWithSelf
import com.huawei.cangjie.psi.psiUtil.startOffset
import com.huawei.cangjie.renderer.render
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.ResolutionFacade
import com.huawei.cangjie.resolve.scopes.getResolutionScope
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.utils.getImplicitReceiversWithInstanceToExpression
import com.huawei.cangjie.utils.safeAs
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
val KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY = Key<Unit>("KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY")
class ThisItemLookupObject(val receiverParameter: ReceiverParameterDescriptor, val labelName: Name?) : KeywordLookupObject()
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

fun ThisItemLookupObject.createLookupElement() = createKeywordElement("this", labelName.labelNameToTail(), lookupObject = this)
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
    thisClassMember,
    baseClassMember,
    thisTypeExtension,
    baseTypeExtension,
    typeParameterExtension,
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
/**
 * Implementation in K2: [org.jetbrains.kotlin.idea.completion.contributors.keywords.ReturnKeywordHandler]
 */
fun returnExpressionItems(bindingContext: BindingContext, position: CjElement): Collection<LookupElement> {
    val result = mutableListOf<LookupElement>()

//    for (parent in position.parentsWithSelf.filterIsInstance<CjDeclarationWithBody>()) {
//        val returnType = parent.returnType(bindingContext)
//        val isUnit = returnType == null || CangJieBuiltIns.isUnit(returnType)
//        if (parent is CjFunctionLiteral) {
//            val (label, call) = parent.findLabelAndCall()
//            if (label != null) {
//                result.add(createKeywordElementWithSpace("return", tail = label.labelNameToTail(), addSpaceAfter = !isUnit))
//            }
//
//            // check if the current function literal is inlined and stop processing outer declarations if it's not
//            val callee = call?.calleeExpression as? CjReferenceExpression ?: break // not inlined
//            if (!InlineUtil.isInline(bindingContext[BindingContext.REFERENCE_TARGET, callee])) break // not inlined
//        } else {
//            if (parent.hasBlockBody()) {
//                val blockBodyReturns = mutableListOf<LookupElement>()
//                blockBodyReturns.add(createKeywordElementWithSpace("return", addSpaceAfter = !isUnit))
//
//                if (returnType != null) {
//                    if (returnType.nullability() == TypeNullability.NULLABLE) {
//                        blockBodyReturns.add(createKeywordElement("return None"))
//                    }
//
//                    fun emptyListShouldBeSuggested(): Boolean = CangJieBuiltIns.isCollectionOrNullableCollection(returnType)
//                            || CangJieBuiltIns.isListOrNullableList(returnType)
//                            || CangJieBuiltIns.isIterableOrNullableIterable(returnType)
//
//                    if (CangJieBuiltIns.isBooleanOrNullableBoolean(returnType)) {
//                        blockBodyReturns.add(createKeywordElement("return true"))
//                        blockBodyReturns.add(createKeywordElement("return false"))
//                    } else if (emptyListShouldBeSuggested()) {
//                        blockBodyReturns.add(createKeywordElement("return", tail = " emptyList()"))
//                    } else if (CangJieBuiltIns.isSetOrNullableSet(returnType)) {
//                        blockBodyReturns.add(createKeywordElement("return", tail = " emptySet()"))
//                    }
//                }
//
//                if (isLikelyInPositionForReturn(position, parent, isUnit)) {
//                    blockBodyReturns.forEach { it.keywordProbability = KeywordProbability.HIGH }
//                }
//
//                result.addAll(blockBodyReturns)
//            }
//            break
//        }
//    }

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
        val baseLookupElement = createLookupElement(classifier, qualifyNestedClasses = true, includeClassTypeArguments = false)

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
