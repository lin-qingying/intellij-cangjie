package com.huawei.cangjie.ide.completion.keywords


import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.config.LanguageVersionSettingsImpl
import com.huawei.cangjie.ide.completion.handlers.WithTailInsertHandler
import com.huawei.cangjie.ide.completion.handlers.createKeywordConstructLookupElement
import com.huawei.cangjie.lexer.CjKeywordToken
import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.lexer.CjTokens.*
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.psi.psiUtil.*
import com.huawei.cangjie.renderer.render
import com.huawei.cangjie.resolve.CangJieTarget
import com.huawei.cangjie.resolve.CangJieTarget.*
import com.huawei.cangjie.resolve.deprecatedParentTargetMap
import com.huawei.cangjie.resolve.possibleParentTargetPredicateMap
import com.huawei.cangjie.resolve.possibleTargetMap
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.*
import com.intellij.psi.filters.*
import com.intellij.psi.filters.position.LeftNeighbour
import com.intellij.psi.filters.position.PositionElementFilter
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parentOfTypes
import com.intellij.psi.util.prevLeafs
import com.intellij.psi.xml.XmlTag
import org.jetbrains.annotations.NonNls

/**
 * We want all [KeywordLookupObject]s to be equal to each other.
 *
 * That way, if the same keyword is completed twice, it would not be duplicated in the completion. This is not required in the regular
 * completion, but can be a problem in CodeWithMe plugin (see https://youtrack.jetbrains.com/issue/CWM-438).
 */
open class KeywordLookupObject {
    override fun equals(other: Any?): Boolean = this === other || javaClass == other?.javaClass
    override fun hashCode(): Int = javaClass.hashCode()
}

class KeywordCompletion(private val languageVersionSettingProvider: LanguageVersionSettingProvider) {
    interface LanguageVersionSettingProvider {
        fun getLanguageVersionSetting(element: PsiElement): LanguageVersionSettings
        fun getLanguageVersionSetting(module: Module): LanguageVersionSettings
    }

    companion object {
        private val ALL_KEYWORDS = (KEYWORDS.types + SOFT_KEYWORDS.types)
            .map { it as CjKeywordToken }

        private val INCOMPATIBLE_KEYWORDS_AROUND_SEALED = setOf(
            SEALED_KEYWORD,

            ENUM_KEYWORD,
            OPEN_KEYWORD,

            ABSTRACT_KEYWORD
        ).mapTo(HashSet()) { it.value }

        private val KEYWORDS_ALLOWED_INSIDE_ANNOTATION_ENTRY: Set<CjKeywordToken> = setOf(
            IF_KEYWORD,
            ELSE_KEYWORD,
            TRUE_KEYWORD,
            FALSE_KEYWORD,
            MATCH_KEYWORD,
        )

        /**
         * 关键字组合
         * 例如：
         * abstract class
         * sealed interface
         * const func
         */
        private fun getCompoundKeywords(
            token: CjKeywordToken,
            languageVersionSettings: LanguageVersionSettings
        ): Set<CjKeywordToken>? =
            mapOf<CjKeywordToken, Set<CjKeywordToken>>(

                SEALED_KEYWORD to setOf(CLASS_KEYWORD, INTERFACE_KEYWORD ),
                ABSTRACT_KEYWORD to setOf(CLASS_KEYWORD),

                CONST_KEYWORD to setOf(FUNC_KEYWORD, INIT_KEYWORD),

                MUT_KEYWORD to setOf(FUNC_KEYWORD, PROP_KEYWORD)

            )[token]

        private val COMPOUND_KEYWORDS_NOT_SUGGEST_TOGETHER = mapOf<CjKeywordToken, Set<CjKeywordToken>>(
            // 'fun' can follow 'sealed', e.g. "sealed fun interface". But "sealed fun" looks irrelevant differ to "sealed interface/class".
            SEALED_KEYWORD to setOf(FUNC_KEYWORD),
        )

        private val KEYWORD_CONSTRUCTS = mapOf<CjKeywordToken, String>(
            IF_KEYWORD to "func foo() { if (caret)",
            WHILE_KEYWORD to "func foo() { while(caret)",
            FOR_KEYWORD to "func foo() { for(caret)",
            TRY_KEYWORD to "func foo() { try {\ncaret\n}",
            CATCH_KEYWORD to "func foo() { try {} catch (caret)",
            FINALLY_KEYWORD to "func foo() { try {\n}\nfinally{\ncaret\n}",
            DO_KEYWORD to "func foo() { do {\ncaret\n}",

            INIT_KEYWORD to "class C { init(caret){}",

            )

        private val NO_SPACE_AFTER = listOf(
            THIS_KEYWORD,
            SUPER_KEYWORD,

            TRUE_KEYWORD,
            FALSE_KEYWORD,
            BREAK_KEYWORD,
            CONTINUE_KEYWORD,
            ELSE_KEYWORD,
            MATCH_KEYWORD,
            FILE_KEYWORD,

            GET_KEYWORD,
            SET_KEYWORD
        ).map { it.value }
    }

    fun complete(
        position: PsiElement,
        prefixMatcher: PrefixMatcher,

        consumer: (LookupElement) -> Unit
    ) {
        if (!GENERAL_FILTER.isAcceptable(position, position)) return
        val sealedInterfacesEnabled = languageVersionSettingProvider.getLanguageVersionSetting(position)
            .supportsFeature(LanguageFeature.SealedInterfaces)

        val parserFilter = buildFilter(position)
        for (keywordToken in ALL_KEYWORDS) {
            val nextKeywords = keywordToken.getNextPossibleKeywords(position) ?: setOf(null)
            nextKeywords.forEach {
                if (keywordToken == SEALED_KEYWORD && it == INTERFACE_KEYWORD && !sealedInterfacesEnabled) return@forEach
                handleCompoundKeyword(position, keywordToken, it,  prefixMatcher, parserFilter, consumer)
            }
        }
    }

    private fun CjKeywordToken.getNextPossibleKeywords(position: PsiElement): Set<CjKeywordToken>? {
        return when {
//            this == SUSPEND_KEYWORD && position.isInsideCjTypeReference -> null
            else -> getCompoundKeywords(this, languageVersionSettingProvider.getLanguageVersionSetting(position))
        }
    }

    private fun CjKeywordToken.avoidSuggestingWith(keywordToken: CjKeywordToken): Boolean {
        val nextKeywords = COMPOUND_KEYWORDS_NOT_SUGGEST_TOGETHER[this] ?: return false
        return keywordToken in nextKeywords
    }

    private fun ignorePrefixForKeyword(completionPosition: PsiElement, keywordToken: CjKeywordToken): Boolean =
        when (keywordToken) {
            // it's needed to complete overrides that should work by member name too
            OVERRIDE_KEYWORD -> true

            // keywords that might be used with labels (@label) after them
            THIS_KEYWORD,
            RETURN_KEYWORD,
            BREAK_KEYWORD,
            CONTINUE_KEYWORD -> {
                // If the position is parsed as an expression and has a label, it means that the completion is performed
                // in a place like `return@la<caret>`. The prefix matcher in this case will have its prefix == "la",
                // and it won't match with the keyword text ("return" in this case).
                // That's why we want to ignore the prefix matcher for such positions
                completionPosition is CjExpressionWithLabel && completionPosition.getTargetLabel() != null
            }

            else -> false
        }

    private fun handleCompoundKeyword(
        position: PsiElement,
        keywordToken: CjKeywordToken,
        nextKeyword: CjKeywordToken?,

        prefixMatcher: PrefixMatcher,
        parserFilter: (CjKeywordToken) -> Boolean,
        consumer: (LookupElement) -> Unit
    ) {
        if (position.isInsideAnnotationEntryArgumentList() && keywordToken !in KEYWORDS_ALLOWED_INSIDE_ANNOTATION_ENTRY) return

        var keyword = keywordToken.value

        var applicableAsCompound = false
        if (nextKeyword != null) {
            fun PsiElement.isSpace() = this is PsiWhiteSpace && '\n' !in getText()

            var next = position.nextLeaf { !(it.isSpace() || it.text == "$") }?.text
            next = next?.removePrefix("$")

            if (keywordToken == SEALED_KEYWORD) {
                if (next in INCOMPATIBLE_KEYWORDS_AROUND_SEALED) return
                val prev = position.prevLeaf { !(it.isSpace() || it is PsiErrorElement) }?.text
                if (prev in INCOMPATIBLE_KEYWORDS_AROUND_SEALED) return
            }

            val nextIsNotYetPresent = keywordToken.getNextPossibleKeywords(position)?.none { it.value == next } == true
            if (nextIsNotYetPresent && keywordToken.avoidSuggestingWith(nextKeyword)) return

            if (nextIsNotYetPresent)
                keyword += " " + nextKeyword.value
            else
                applicableAsCompound = true
        }


        if (!ignorePrefixForKeyword(position, keywordToken) && !prefixMatcher.isStartMatch(keyword)) return

        if (!parserFilter(keywordToken)) return

        val constructText = KEYWORD_CONSTRUCTS[keywordToken]
        if (constructText != null && !applicableAsCompound) {
            val element = createKeywordConstructLookupElement(position.project, keyword, constructText)
            consumer(element)
        } else {
            handleTopLevelClassName(position, keyword, consumer)
            consumer(createLookupElementBuilder(keyword, position))
        }
    }

    private fun handleTopLevelClassName(position: PsiElement, keyword: String, consumer: (LookupElement) -> Unit) {
        val topLevelClassName = getTopLevelClassName(position)
        fun consumeClassNameWithoutBraces() {
            consumer(createLookupElementBuilder("$keyword $topLevelClassName", position))
        }
        if (topLevelClassName != null) {
            if (listOf(INTERFACE_KEYWORD).any { keyword.endsWith(it.value) }) {
                consumeClassNameWithoutBraces()
            }
            if (keyword.endsWith(CLASS_KEYWORD.value)) {

                consumeClassNameWithoutBraces()

            }
        }
    }

    private fun createLookupElementBuilder(
        keyword: String,
        position: PsiElement
    ): LookupElementBuilder {
        val isUseSiteAnnotationTarget = position.prevLeaf()?.node?.elementType == AT
        val insertHandler = when {
            isUseSiteAnnotationTarget -> UseSiteAnnotationTargetInsertHandler
            keyword in NO_SPACE_AFTER -> null
            else -> SpaceAfterInsertHandler
        }
        val element =
            LookupElementBuilder.create(KeywordLookupObject(), keyword).bold().withInsertHandler(insertHandler)
        return if (isUseSiteAnnotationTarget) {
            element.withPresentableText("$keyword:")
        } else {
            element
        }
    }

    private fun getTopLevelClassName(position: PsiElement): String? {
        if (position.parents.any { it is CjDeclaration }) return null
        val file = position.containingFile as? CjFile ?: return null
        val name = FileUtil.getNameWithoutExtension(file.name)
        if (!Name.isValidIdentifier(name)
            || Name.identifier(name).render() != name
            || !name[0].isUpperCase()
            || file.declarations.any { it is CjTypeStatement && it.name == name }
        ) return null
        return name
    }

    private object UseSiteAnnotationTargetInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            WithTailInsertHandler(":", spaceBefore = false, spaceAfter = false).postHandleInsert(context, item)
        }
    }

    private object SpaceAfterInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            WithTailInsertHandler.SPACE.postHandleInsert(context, item)
        }
    }

    private val GENERAL_FILTER = NotFilter(
        OrFilter(
            CommentFilter(),
            ParentFilter(ClassFilter(CjLiteralStringTemplateEntry::class.java)),
            ParentFilter(ClassFilter(CjConstantExpression::class.java)),
            FileFilter(ClassFilter(CjTypeCodeFragment::class.java)),
            LeftNeighbour(TextFilter(".")),
            LeftNeighbour(TextFilter("?."))
        )
    )

    private class CommentFilter : ElementFilter {
        override fun isAcceptable(element: Any?, context: PsiElement?) =
            (element is PsiElement) && CjPsiUtil.isInComment(element)

        override fun isClassAcceptable(hintClass: Class<out Any?>) = true
    }

    private class ParentFilter(filter: ElementFilter) : PositionElementFilter() {
        init {
            setFilter(filter)
        }

        override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
            val parent = (element as? PsiElement)?.parent
            return parent != null && (filter?.isAcceptable(parent, context) ?: true)
        }
    }

    private class FileFilter(filter: ElementFilter) : PositionElementFilter() {
        init {
            setFilter(filter)
        }

        override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
            val file = (element as? PsiElement)?.containingFile
            return file != null && (filter?.isAcceptable(file, context) ?: true)
        }
    }

    private fun buildFilter(position: PsiElement): (CjKeywordToken) -> Boolean {
        var parent = position.parent
        var prevParent = position
        while (parent != null) {
            when (parent) {
                is CjBlockExpression -> {


                    var prefixText = "func foo() { "
                    if (prevParent is CjExpression) {
                        // check that we are right after a try-expression without finally-block or after if-expression without else
                        val prevLeaf =
                            prevParent.prevLeaf { it !is PsiWhiteSpace && it !is PsiComment && it !is PsiErrorElement }
                        if (prevLeaf != null) {
                            val isAfterThen =
                                prevLeaf.goUpWhileIsLastChild().any { it.node.elementType == CjNodeTypes.THEN }

                            var isAfterTry = false
                            var isAfterCatch = false
                            if (prevLeaf.node.elementType == RBRACE) {
                                when ((prevLeaf.parent as? CjBlockExpression)?.parent) {
                                    is CjTryExpression -> isAfterTry = true
                                    is CjCatchClause -> {
                                        isAfterTry = true; isAfterCatch = true
                                    }
                                }
                            }

                            if (isAfterThen) {
                                prefixText += if (isAfterTry) {
                                    "if (a)\n"
                                } else {
                                    "if (a) {}\n"
                                }
                            }
                            if (isAfterTry) {
                                prefixText += "try {}\n"
                            }
                            if (isAfterCatch) {
                                prefixText += "catch (e: E) {}\n"
                            }
                        }

                        return buildFilterWithContext(prefixText, prevParent, position)
                    } else {
                        val lastExpression = prevParent
                            .siblings(forward = false, withItself = false)
                            .firstIsInstanceOrNull<CjExpression>()
                        if (lastExpression != null) {
                            val contextAfterExpression = lastExpression
                                .siblings(forward = true, withItself = false)
                                .takeWhile { it != prevParent }
                                .joinToString { it.text }
                            return buildFilterWithContext(
                                prefixText + "x" + contextAfterExpression,
                                prevParent,
                                position
                            )
                        }
                    }
                }

                is CjDeclarationWithInitializer -> {
                    val initializer = parent.initializer
                    if (prevParent == initializer) {
                        return buildFilterWithContext("let v = ", initializer, position)
                    }
                }

                is CjParameter -> {
                    val default = parent.defaultValue
                    if (prevParent == default) {
                        return buildFilterWithContext("let v = ", default, position)
                    }
                }

                // for type references in places like 'listOf<' or 'List<' we want to filter almost all keywords
                // (except maybe for 'suspend' and 'in'/'out', since they can be a part of a type reference)
                is CjTypeReference -> {
                    val shouldIntroduceTypeReferenceContext = when {

                        // it can be a receiver type, or it can be a declaration's name,
                        // so we don't want to change the context
                        parent.isExtensionReceiverInCallableDeclaration -> false

                        // it is probably an annotation entry, or a super class constructor's invocation,
                        // in this case we don't want to change the context
                        parent.parent is CjConstructorCalleeExpression -> false

                        else -> true
                    }

                    if (shouldIntroduceTypeReferenceContext) {

                        // we cannot just search for an outer element of CjTypeReference type, because
                        // we can be inside the lambda type args (e.g. 'val foo: (bar: <caret>) -> Unit');
                        // that's why we have to do a more precise check
                        val prefixText = if (parent.isTypeArgumentOfOuterCjTypeReference) {
                            "fun foo(x: X<"
                        } else {
                            "fun foo(x: "
                        }

                        return buildFilterWithContext(prefixText, contextElement = parent, position)
                    }
                }

                is CjDeclaration -> {
                    when (parent.parent) {
                        is CjTypeStatement -> {
                            return if (parent is CjPrimaryConstructor) {
                                buildFilterWithReducedContext("class X ", parent, position)
                            } else {
                                buildFilterWithReducedContext("class X { ", parent, position)
                            }
                        }

                        is CjFile -> return buildFilterWithReducedContext("", parent, position)
                    }
                }
            }


            prevParent = parent
            parent = parent.parent
        }

        return buildFilterWithReducedContext("", null, position)
    }

    private val CjTypeReference.isExtensionReceiverInCallableDeclaration: Boolean
        get() {
            val parent = parent
            return parent is CjCallableDeclaration && parent.receiverTypeReference == this
        }

    private val CjTypeReference.isTypeArgumentOfOuterCjTypeReference: Boolean
        get() {
            val typeProjection = parent as? CjTypeProjection
            val typeArgumentList = typeProjection?.parent as? CjTypeArgumentList
            val userType = typeArgumentList?.parent as? CjUserType

            return userType?.parent is CjTypeReference
        }

    private fun computeKeywordApplications(prefixText: String, keyword: CjKeywordToken): Sequence<String> =
        when (keyword) {
//        SUSPEND_KEYWORD -> sequenceOf("suspend () -> Unit>", "suspend X")
            else -> {
                if (prefixText.endsWith("@"))
                    sequenceOf(keyword.value + ":X Y.Z")
                else
                    sequenceOf(keyword.value + " X")
            }
        }

    private fun buildFilterWithContext(
        prefixText: String,
        contextElement: PsiElement,
        position: PsiElement
    ): (CjKeywordToken) -> Boolean {
        val offset = position.getStartOffsetInAncestor(contextElement)
        val truncatedContext = contextElement.text!!.substring(0, offset)
        return buildFilterByText(prefixText + truncatedContext, position)
    }

    private fun buildFilterWithReducedContext(
        prefixText: String,
        contextElement: PsiElement?,
        position: PsiElement
    ): (CjKeywordToken) -> Boolean {
        val builder = StringBuilder()
        buildReducedContextBefore(builder, position, contextElement)
        return buildFilterByText(prefixText + builder.toString(), position)
    }


    private fun buildFilesWithKeywordApplication(
        keywordTokenType: CjKeywordToken,
        prefixText: String,
        psiFactory: CjPsiFactory
    ): Sequence<CjFile> {
        return computeKeywordApplications(prefixText, keywordTokenType)
            .map { application -> psiFactory.createFile(prefixText + application) }
    }


    private fun buildFilterByText(prefixText: String, position: PsiElement): (CjKeywordToken) -> Boolean {
        val psiFactory = CjPsiFactory(position.project)

        fun PsiElement.isSecondaryConstructorInObjectDeclaration(): Boolean {
            val secondaryConstructor = parentOfType<CjSecondaryConstructor>() ?: return false
            return false
        }

        fun isKeywordCorrectlyApplied(keywordTokenType: CjKeywordToken, file: CjFile): Boolean {
            val elementAt = file.findElementAt(prefixText.length)!!

            val languageVersionSettings =
                ModuleUtilCore.findModuleForPsiElement(position)
                    ?.let(languageVersionSettingProvider::getLanguageVersionSetting)
                    ?: LanguageVersionSettingsImpl.DEFAULT
            when {
                !elementAt.node!!.elementType.matchesKeyword(keywordTokenType) -> return false

                elementAt.getNonStrictParentOfType<PsiErrorElement>() != null -> return false

                isErrorElementBefore(elementAt) -> return false

                !isModifierSupportedAtLanguageLevel(
                    elementAt,
                    keywordTokenType,
                    languageVersionSettings
                ) -> return false

                (keywordTokenType == LET_KEYWORD || keywordTokenType == VAR_KEYWORD) &&
                        elementAt.parent is CjParameter &&
                        elementAt.parentOfTypes(
                            CjNamedFunction::class,
                            CjSecondaryConstructor::class
                        ) != null -> return false

                keywordTokenType == INIT_KEYWORD && elementAt.isSecondaryConstructorInObjectDeclaration() -> return false

                keywordTokenType !is CjModifierKeywordToken -> return true

                else -> {
                    val container = (elementAt.parent as? CjModifierList)?.parent ?: return true
                    val possibleTargets = when (container) {
                        is CjParameter -> {
                            if (container.ownerFunction is CjPrimaryConstructor)
                                listOf(VALUE_PARAMETER, MEMBER_PROPERTY)
                            else
                                listOf(VALUE_PARAMETER)
                        }

                        is CjTypeParameter -> listOf(TYPE_PARAMETER)

                        is CjEnumEntry -> listOf(ENUM_ENTRY)

                        is CjClassBody -> listOf(
                            CLASS_ONLY,
                            INTERFACE,
                            STRUCT,
                            ENUM,
//                            ANNOTATION_CLASS,
                            MEMBER_FUNCTION,
                            MEMBER_PROPERTY,
                            FUNCTION,
                            PROPERTY
                        )

                        is CjFile -> listOf(
                            CLASS_ONLY,
                            INTERFACE,
                            STRUCT,
                            ENUM,
//                            ANNOTATION_CLASS,
                            TOP_LEVEL_FUNCTION,
                            TOP_LEVEL_PROPERTY,
                            FUNCTION,
                            PROPERTY
                        )

                        else -> listOf()
                    }
                    val modifierTargets = possibleTargetMap[keywordTokenType]?.intersect(possibleTargets.toSet())
                    if (modifierTargets != null && possibleTargets.isNotEmpty() &&
                        modifierTargets.none {
                            isModifierTargetSupportedAtLanguageLevel(keywordTokenType, it, languageVersionSettings)
                        }
                    ) return false

                    val parentTarget =
                        when (val ownerDeclaration = container.getParentOfType<CjDeclaration>(strict = true)) {
                            null -> FILE

                            is CjTypeStatement -> {
                                when {
                                    ownerDeclaration.isInterface() -> INTERFACE
                                    ownerDeclaration.isEnum() -> ENUM
//                                ownerDeclaration.isAnnotation() -> ANNOTATION_CLASS
                                    else -> CLASS_ONLY
                                }
                            }

                            else -> return keywordTokenType != CONST_KEYWORD
                        }

                    if (!isPossibleParentTarget(keywordTokenType, parentTarget, languageVersionSettings)) return false

                    if (keywordTokenType == CONST_KEYWORD) {
                        return when (parentTarget) {

                            FILE -> {
                                val prevSiblings = elementAt.parent.siblings(withItself = false, forward = false)
                                val hasLineBreak = prevSiblings
                                    .takeWhile { it is PsiWhiteSpace || it.isSemicolon() }
                                    .firstOrNull { it.text.contains("\n") || it.isSemicolon() } != null
                                hasLineBreak || prevSiblings.none {
                                    it !is PsiWhiteSpace && !it.isSemicolon() && it !is CjImportList && it !is CjPackageDirective
                                }
                            }

                            else -> false
                        }
                    }

                    return true
                }
            }
        }

        return fun(keywordTokenType): Boolean {
            val files = buildFilesWithKeywordApplication(keywordTokenType, prefixText, psiFactory)
            return files.any { file -> isKeywordCorrectlyApplied(keywordTokenType, file); }
        }
    }

    private fun PsiElement.isSemicolon() = node.elementType == SEMICOLON

    private fun isErrorElementBefore(token: PsiElement): Boolean {
        for (leaf in token.prevLeafs) {
            if (leaf is PsiWhiteSpace || leaf is PsiComment) continue
            if (leaf.parentsWithSelf.any { it is PsiErrorElement }) return true
            if (leaf.textLength != 0) break
        }
        return false
    }

    private fun IElementType.matchesKeyword(keywordType: CjKeywordToken): Boolean {
        return when (this) {
            keywordType -> true

            else -> false
        }
    }

    private fun isModifierSupportedAtLanguageLevel(
        position: PsiElement,
        keyword: CjKeywordToken,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        val feature = when (keyword) {
            TYPE_KEYWORD -> LanguageFeature.TypeAliases


            else -> return true
        }
        return languageVersionSettings.supportsFeature(feature)
    }

    private fun isModifierTargetSupportedAtLanguageLevel(
        keyword: CjKeywordToken,
        target: CangJieTarget,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {

        return true

    }

    // builds text within scope (or from the start of the file) before position element excluding almost all declarations
    private fun buildReducedContextBefore(builder: StringBuilder, position: PsiElement, scope: PsiElement?) {
        if (position == scope) return

        if (position is CjCodeFragment) {
            val cjContext = position.context as? CjElement ?: return
            buildReducedContextBefore(builder, cjContext, scope)
            return
        } else if (position is PsiFile) {
            return
        }

        val parent = position.parent ?: return

        buildReducedContextBefore(builder, parent, scope)

        val prevDeclaration = position.siblings(forward = false, withItself = false).firstOrNull { it is CjDeclaration }

        var child = parent.firstChild
        while (child != position) {
            if (child is CjDeclaration) {
                if (child == prevDeclaration) {
                    builder.appendReducedText(child)
                }
            } else {
                builder.append(child!!.text)
            }

            child = child.nextSibling
        }
    }

    private fun StringBuilder.appendReducedText(element: PsiElement) {
        var child = element.firstChild
        if (child == null) {
            append(element.text!!)
        } else {
            while (child != null) {
                when (child) {
                    is CjBlockExpression, is CjClassBody -> append("{}")
                    else -> appendReducedText(child)
                }

                child = child.nextSibling
            }
        }
    }

    private fun PsiElement.getStartOffsetInAncestor(ancestor: PsiElement): Int {
        if (ancestor == this) return 0
        return parent!!.getStartOffsetInAncestor(ancestor) + startOffsetInParent
    }

    private fun PsiElement.goUpWhileIsLastChild(): Sequence<PsiElement> = generateSequence(this) {
        when {
            it is PsiFile -> null
            it != it.parent.lastChild -> null
            else -> it.parent
        }
    }

    private fun isPossibleParentTarget(
        modifier: CjModifierKeywordToken,
        parentTarget: CangJieTarget,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        deprecatedParentTargetMap[modifier]?.let {
            if (parentTarget in it) return false
        }

        possibleParentTargetPredicateMap[modifier]?.let {
            return it.isAllowed(parentTarget, languageVersionSettings)
        }

        return true
    }


}

class TextFilter : PlainTextFilter {
    constructor(@NonNls value: String?) : super(value)

    constructor(@NonNls vararg values: String?) : super(*values)

    constructor(@NonNls value1: String?, @NonNls value2: String?) : super(value1, value2)

    override fun getTextByElement(element: Any): String {
        return if (element is XmlTag) {
            element.localName
        } else {
            super.getTextByElement(element)
        }
    }
}
