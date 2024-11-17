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

package com.linqingying.cangjie.parsing


import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.lang.*
import com.intellij.lang.impl.PsiBuilderAdapter
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.lexer.Lexer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringHash
import com.intellij.openapi.util.text.StringUtil.*
import com.intellij.psi.PsiReference
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.resolve.FileContextUtil
import com.intellij.psi.impl.source.tree.CompositePsiElement
import com.intellij.psi.tree.ICompositeElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.PairProcessor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.LimitedPool
import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.lexer.CjTokens
import org.intellij.grammar.config.Options
import org.intellij.grammar.parser.GeneratedParserUtilBase
import org.jetbrains.annotations.Contract

abstract class BnfParsing(
    builder: SemanticWhitespaceAwarePsiBuilder,
    isLazy: Boolean
) : AbstractCangJieParsing(
    builder, isLazy
) {
    private class DummyBlockElementType : IElementType("DUMMY_BLOCK", Language.ANY), ICompositeElementType {
        override fun createCompositeNode(): ASTNode {
            return DummyBlock()
        }
    }

    interface Parser {
        fun parse(builder: PsiBuilder, level: Int): Boolean
    }

    class DummyBlock : CompositePsiElement(DUMMY_BLOCK) {
        override fun getReferences(): Array<PsiReference> {
            return PsiReference.EMPTY_ARRAY
        }

        override fun getLanguage(): Language {
            return parent.language
        }
    }

    interface Hook<T> {
        @Contract("_,null,_->null")
        fun run(builder: PsiBuilder?, marker: PsiBuilder.Marker?, param: T): PsiBuilder.Marker?
    }

    class MyList<E>(initialCapacity: Int) : ArrayList<E>(initialCapacity) {
        fun setSize(fromIndex: Int) {
            removeRange(fromIndex, size)
        }

        override fun add(element: E): Boolean {
            val size = size
            if (size >= MAX_VARIANTS_SIZE) {
                removeRange(MAX_VARIANTS_SIZE / 4, size - MAX_VARIANTS_SIZE / 4)
            }
            return super.add(element)
        }
    }

    fun isWhitespaceOrComment(type: IElementType?): Boolean {
        return (myBuilder as PsiBuilderImpl).whitespaceOrComment(type)
    }

    inner class CompletionState(val offset: Int) : (Any?) -> String? {
        val items: MutableSet<String> = HashSet()

        fun convertItem(o: Any?): String {
            return if (o is Array<*>) {
                join(o, this, " ")
            } else {
                o.toString()
            }
        }

        override fun invoke(o: Any?): String {
            return convertItem(o)
        }

        fun addItem(text: String) {
            items.add(text)
        }

        fun prefixMatches(text: String): Boolean {
            val builderOffset = myBuilder.currentOffset
            var diff = offset - builderOffset
            val length = text.length
            return when {
                diff == 0 -> true
                diff > 0 && diff <= length -> {
                    val fragment = myBuilder.originalText.subSequence(builderOffset, offset)
                    prefixMatches(fragment.toString(), text)
                }

                diff < 0 -> {
                    for (i in -1 downTo Int.MIN_VALUE) {
                        val type = myBuilder.rawLookup(i)
                        val tokenStart = myBuilder.rawTokenTypeStart(i)
                        when {
                            isWhitespaceOrComment(type) -> diff = offset - tokenStart
                            type != null && tokenStart < offset -> {
                                val fragment = myBuilder.originalText.subSequence(tokenStart, offset)
                                if (prefixMatches(fragment.toString(), text)) {
                                    diff = offset - tokenStart
                                }
                                break
                            }

                            else -> break
                        }
                    }
                    diff in 0 until length
                }

                else -> false
            }
        }

        fun prefixMatches(prefix: String, variant: String): Boolean {
            val matches = CamelHumpMatcher(prefix, false).prefixMatches(variant.replace(' ', '_'))
            return if (matches && prefix.last().isWhitespace()) {
                startsWithIgnoreCase(variant, prefix)
            } else {
                matches
            }
        }

        private fun startsWithIgnoreCase(str: String, prefix: String): Boolean {
            return str.regionMatches(0, prefix, 0, prefix.length, ignoreCase = true)
        }
    }

    inner class Builder(
        builder: PsiBuilder,
        val state: ErrorState,
        val parser: PsiParser
    ) : PsiBuilderAdapter(builder) {


        fun getLexer(): Lexer {
            return (myDelegate as PsiBuilderImpl).lexer
        }


        fun getProductions(): List<PsiBuilderImpl.ProductionMarker> {
            return (myDelegate as PsiBuilderImpl).productions
        }
    }

    class ErrorState {

        var currentFrame: Frame? = null
        var completionState: CompletionState? = null
        var variants = MyList<Variant>(INITIAL_VARIANTS_SIZE)
        var unexpected = MyList<Variant>(INITIAL_VARIANTS_SIZE / 10)

        var predicateCount: Int = 0
        var level: Int = 0
        var predicateSign: Boolean = true
        var suppressErrors: Boolean = false
        var hooks: Hooks<*>? = null

        var extendsSets: Array<TokenSet>? = null
        var altExtendsChecker: PairProcessor<IElementType, IElementType>? = null
        var caseSensitive: Boolean = false
        var braces: Array<BracePair>? = null
        var tokenAdvancer: Parser = TOKEN_ADVANCER
        var altMode: Boolean = false

        val VARIANTS = LimitedPool(VARIANTS_POOL_SIZE) { Variant() }
        val FRAMES = LimitedPool(FRAMES_POOL_SIZE) { Frame() }

        companion object {
            fun get(builder: PsiBuilder): ErrorState {
                return (builder as Builder).state
            }

            fun initState(state: ErrorState, builder: PsiBuilder, root: IElementType, extendsSets: Array<TokenSet>) {
                state.extendsSets = extendsSets
                val file = builder.getUserData(FileContextUtil.CONTAINING_FILE_KEY)
                state.completionState = file?.getUserData(COMPLETION_STATE_KEY)
                val language = file?.language ?: root.language
                state.caseSensitive = language.isCaseSensitive
                val matcher = LanguageBraceMatching.INSTANCE.forLanguage(language)
                state.braces = matcher?.pairs
                if (state.braces?.isEmpty() == true) state.braces = null
            }

        }

        fun getExpected(position: Int, expected: Boolean): String {
            val sb = StringBuilder()
            val list = if (expected) variants else unexpected
            val strings = Array(list.size) { "" }
            val hashes = LongArray(strings.size)
            var count = 0
            for (variant in list) {
                if (position == variant.position) {
                    val text = variant.`object`.toString()
                    val hash = StringHash.calc(text)
                    if (hashes.take(count).contains(hash)) continue
                    hashes[count] = hash
                    strings[count] = text
                    count++
                }
            }
            strings.sort()
            count = 0
            for (s in strings) {
                if (s.isEmpty()) continue
                if (count++ > 0) {
                    if (count > MAX_VARIANTS_TO_DISPLAY) {
                        sb.append(" and ...")
                        break
                    } else {
                        sb.append(", ")
                    }
                }
                val displayText = if (s.startsWith('<') || isJavaIdentifierStart(s[0])) s else "'$s'"
                sb.append(displayText)
            }
            if (count > 1 && count < MAX_VARIANTS_TO_DISPLAY) {
                val idx = sb.lastIndexOf(", ")
                sb.replace(idx, idx + 1, " or")
            }
            return sb.toString()
        }

        fun clearVariants(frame: Frame?) {
            clearVariants(true, frame?.variantCount ?: 0)
            frame?.lastVariantAt = -1
        }

        fun clearVariants(expected: Boolean, start: Int) {
            val list = if (expected) variants else unexpected
            if (start < 0 || start >= list.size) return
            for (i in start until list.size) {
                VARIANTS.recycle(list[i])
            }
            list.setSize(start)
        }

        fun typeExtends(child: IElementType, parent: IElementType): Boolean {
            if (child == parent) return true
            if (extendsSets != null) {
                for (set in extendsSets!!) {
                    if (set.contains(child) && set.contains(parent)) return true
                }
            }
            return altExtendsChecker?.process(child, parent) ?: false
        }
    }

    class Frame {
        var parentFrame: Frame? = null
        lateinit var elementType: IElementType

        var offset: Int = 0
        var position: Int = 0
        var level: Int = 0
        var modifiers: Int = 0
        var name: String? = null
        var variantCount: Int = 0
        var errorReportedAt: Int = -1
        var lastVariantAt: Int = -1
        var leftMarker: PsiBuilder.Marker? = null

        fun init(
            builder: PsiBuilder,
            state: ErrorState,
            level_: Int,
            modifiers_: Int,
            elementType_: IElementType,
            name_: String
        ): Frame {
            parentFrame = state.currentFrame
            elementType = elementType_
            offset = builder.currentOffset
            position = builder.rawTokenIndex()
            level = level_
            modifiers = modifiers_
            name = name_
            variantCount = state.variants.size
            errorReportedAt = -1
            lastVariantAt = -1
            leftMarker = null
            return this
        }

        override fun toString(): String {
            val mod = when (modifiers) {
                _NONE_ -> "_NONE_, "
                else -> buildString {
                    if (modifiers and _COLLAPSE_ != 0) append("_CAN_COLLAPSE_, ")
                    if (modifiers and _LEFT_ != 0) append("_LEFT_, ")
                    if (modifiers and _LEFT_INNER_ != 0) append("_LEFT_INNER_, ")
                    if (modifiers and _AND_ != 0) append("_AND_, ")
                    if (modifiers and _NOT_ != 0) append("_NOT_, ")
                    if (modifiers and _UPPER_ != 0) append("_UPPER_, ")
                }
            }
            return "{$offset:$position:$level, $errorReportedAt, $mod$elementType, $name}"
        }
    }

    class Variant {
        var position: Int = 0
        var `object`: Any? = null

        fun init(pos: Int, o: Any?): Variant {
            position = pos
            `object` = o
            return this
        }

        override fun toString(): String {
            return "<$position, $`object`>"
        }
    }

    class Hooks<T>(
        val hook: Hook<T>,
        val param: T,
        val level: Int,
        val next: Hooks<*>?
    ) {
        companion object {
            fun <E> concat(hook: Hook<E>, param: E, level: Int, hooks: Hooks<*>?): Hooks<E> {
                return Hooks(hook, param, level, hooks)
            }
        }
    }


    companion object {
        private
        val LOG: Logger = Logger.getInstance(
            GeneratedParserUtilBase::class.java
        )

        val COMPLETION_STATE_KEY: Key<CompletionState> = Key.create("COMPLETION_STATE_KEY")

        private
        val MAX_RECURSION_LEVEL: Int = Options.GPUB_MAX_LEVEL.get()

        private
        const val MAX_VARIANTS_SIZE: Int = 10000

        private
        const val MAX_VARIANTS_TO_DISPLAY: Int = 50

        private
        const val MAX_ERROR_TOKEN_TEXT: Int = 20

        private
        const val INITIAL_VARIANTS_SIZE: Int = 1000

        private
        const val VARIANTS_POOL_SIZE: Int = 10000

        private
        const val FRAMES_POOL_SIZE: Int = 500


        val DUMMY_BLOCK: IElementType = DummyBlockElementType()
        // here's the new section API for compact parsers & less IntelliJ platform API exposure

        const val _NONE_: Int = 0x0

        const val _COLLAPSE_: Int = 0x1

        const val _LEFT_: Int = 0x2

        const val _LEFT_INNER_: Int = 0x4

        const val _AND_: Int = 0x8

        const val _NOT_: Int = 0x10

        const val _UPPER_: Int = 0x20


        val TOKEN_ADVANCER: Parser =
            object : Parser {
                override fun parse(builder: PsiBuilder, level: Int): Boolean {
                    if (builder.eof()) return false
                    builder.advanceLexer()
                    return true
                }
            }

    }

    fun nextTokenIsFast(token: IElementType): Boolean {
        return myBuilder.tokenType === token
    }

    fun nextTokenIs(token: IElementType): Boolean {
        if (!addVariantSmart(token, false)) return true
        return nextTokenIsFast(token)
    }

    fun recursion_guard_(level: Int, funcName: String): Boolean {
        if (level > MAX_RECURSION_LEVEL) {
            mark()
                .error("Maximum recursion level ($MAX_RECURSION_LEVEL) reached in '$funcName'")
            return false
        }
        return true
    }

    private fun addVariantInner(
        state: ErrorState,
        frame: Frame,
        pos: Int,
        o: Any
    ) {
        val variant = state.VARIANTS.alloc().init(pos, o)
        if (state.predicateSign) {
            state.variants.add(variant)
            if (frame.lastVariantAt < pos) {
                frame.lastVariantAt = pos
            }
        } else {
            state.unexpected.add(variant)
        }
    }

    private fun addCompletionVariant(

        completionState: CompletionState,
        o: Any
    ) {
        val offset = myBuilder.currentOffset
        if (!myBuilder.eof() && offset == myBuilder.rawTokenTypeStart(1)) return  // suppress for zero-length tokens

        val text = completionState.convertItem(o)
        val length = text.length
        var add = length != 0 && completionState.prefixMatches(text)
        add = add && length > 1 && !(text[0] == '<' && text[length - 1] == '>') &&
                !(text[0] == '\'' && text[length - 1] == '\'' && length < 5)
        if (add) {
            completionState.addItem(text)
        }
    }

    private fun addVariant(state: ErrorState, o: Any) {
        eof() // skip whitespaces
        state.currentFrame?.let { addVariantInner(state, it, myBuilder.rawTokenIndex(), o) }

        val completionState = state.completionState
        if (completionState != null && state.predicateSign) {
            addCompletionVariant(completionState, o)
        }
    }

    private fun addVariantSmart(token: Any, force: Boolean): Boolean {
        val state = ErrorState.get(myBuilder)
        // skip FIRST check in completion mode
        if (state.completionState != null && !force) return false
        eof()
        if (!state.suppressErrors && state.predicateCount < 2) {
            addVariant(state, token)
        }
        return true
    }

    private fun reportFrameError(state: ErrorState) {
        if (state.currentFrame == null || state.suppressErrors) return
        val frame = state.currentFrame
        val pos = myBuilder.rawTokenIndex()
        if (frame != null) {
            if (frame.errorReportedAt > pos) {
                // report error for previous unsuccessful frame
                val marker = myBuilder.latestDoneMarker
                var endOffset = if (marker != null) (marker as PsiBuilderImpl.ProductionMarker).endIndex else pos + 1
                while (endOffset <= pos && isWhitespaceOrComment(

                        rawLookup(endOffset - pos)
                    )
                ) endOffset++
                val inner = endOffset == pos
                eof()
                reportError(state, frame, inner, true, false)
            }
        }
    }

    private fun reportError(

        state: ErrorState,
        frame: Frame,
        inner: Boolean,
        force: Boolean,
        advance: Boolean
    ): Boolean {
        val position = myBuilder.rawTokenIndex()
        val expected = state.getExpected(position, true)
        if (!force && expected.isEmpty() && !advance) return false

        val actual = trim(myBuilder.tokenText)
        val message = when {
            expected.isEmpty() -> {
                if (isEmpty(actual)) "unmatched input"
                else "'${first(actual, MAX_ERROR_TOKEN_TEXT, true)}' unexpected"
            }

            isEmpty(actual) -> "$expected expected"
            else -> "$expected expected, got '${first(actual, MAX_ERROR_TOKEN_TEXT, true)}'"
        }

        if (advance) {
            val mark = myBuilder.mark()
            state.tokenAdvancer.parse(myBuilder, frame.level + 1)
            mark.error(message)
        } else if (inner) {
            val latestDoneMarker = getLatestExtensibleDoneMarker()
            myBuilder.error(message)
            if (latestDoneMarker != null &&
                frame.position >= latestDoneMarker.startIndex &&
                frame.position <= latestDoneMarker.endIndex
            ) {
                extend_marker_impl(latestDoneMarker as PsiBuilder.Marker)
            }
        } else {
            myBuilder.error(message)
        }

        myBuilder.eof() // skip whitespaces
        frame.errorReportedAt = myBuilder.rawTokenIndex()
        return true
    }

    private fun extend_marker_impl(marker: PsiBuilder.Marker) {
        val precede = marker.precede()
        val elementType = (marker as LighterASTNode).tokenType
        if (elementType === TokenType.ERROR_ELEMENT) {
            precede.error(notNullize(PsiBuilderImpl.getErrorMessage(marker as LighterASTNode)))
        } else {
            precede.done(elementType)
        }
        marker.drop()
    }

    private fun getLatestExtensibleDoneMarker(): PsiBuilderImpl.ProductionMarker? {
        val marker = ContainerUtil.getLastItem((myBuilder as PsiBuilderImpl).productions)
        return if (marker == null || marker.tokenType == null || marker !is PsiBuilder.Marker) null else marker
    }

    fun enter_section_(): PsiBuilder.Marker {
        val state = ErrorState.get(myBuilder)
        reportFrameError(state)
        state.level++
        return mark()
    }

    fun consumeTokenFast(token: IElementType): Boolean {
        if (nextTokenIsFast(token)) {
            myBuilder.advanceLexer()
            return true
        }
        return false
    }

    fun consumeToken(token: IElementType): Boolean {
        addVariantSmart(token, true)
        if (nextTokenIsFast(token)) {
            myBuilder.advanceLexer()
            return true
        }
        return false
    }

    private fun consumeTokens(

        smart: Boolean,
        pin: Int,
        vararg tokens: IElementType
    ): Boolean {
        val state = ErrorState.get(myBuilder)
        if (state.completionState != null && state.predicateSign) {
            addCompletionVariant(state.completionState!!, tokens)
        }
        // suppress single token completion
        val completionState = state.completionState
        state.completionState = null
        var result = true
        var pinned = false
        for ((i, token) in tokens.withIndex()) {
            if (pin > 0 && i == pin) pinned = result
            if (result || pinned) {
                val fast = smart && i == 0
                if (!(if (fast) consumeTokenFast(token) else consumeToken(token))) {
                    result = false
                    if (pin < 0 || pinned) report_error_(state, false)
                }
            }
        }
        state.completionState = completionState
        return pinned || result
    }

    fun report_error_(state: ErrorState, advance: Boolean) {
        val frame = state.currentFrame
        if (frame == null) {
            LOG.error("unbalanced enter/exit section call: got null")
            return
        }
        val position = myBuilder.rawTokenIndex()
        if (frame.errorReportedAt < position && frame.lastVariantAt > -1 && frame.lastVariantAt <= position) {
            reportError(state, frame, false, true, advance)
        }
    }

    fun report_error_(result: Boolean): Boolean {
        if (!result) report_error_(

            ErrorState.get(myBuilder),
            false
        )
        return result
    }

    fun consumeTokens(pin: Int, vararg token: IElementType): Boolean {
        return consumeTokens(false, pin, *token)
    }

    private fun close_marker_impl_(
        frame: Frame?,
        marker: PsiBuilder.Marker?,
        elementType: IElementType?,
        result: Boolean
    ) {
        if (marker == null) return
        if (result) {
            if (elementType != null) {
                marker.done(elementType)
            } else {
                marker.drop()
            }
        } else {
            if (frame != null) {
                val position = (marker as PsiBuilderImpl.ProductionMarker).startIndex
                if (frame.errorReportedAt > position) {
                    frame.errorReportedAt = if (frame.parentFrame == null) -1 else frame.parentFrame!!.errorReportedAt
                }
            }
            marker.rollbackTo()
        }
    }

    private fun run_hooks_impl_(state: ErrorState, elementType: IElementType?) {
        if (state.hooks == null) return

        var marker: PsiBuilder.Marker? =
            if (elementType != null) myBuilder.latestDoneMarker as PsiBuilder.Marker? else null

        if (elementType != null && marker == null) {
            mark().error("No expected done marker at offset ${myBuilder.currentOffset}")
        }

        while (state.hooks != null && state.hooks!!.level >= state.level) {
            if (state.hooks!!.level == state.level) {
                marker = state.hooks!!.param?.let { (state.hooks!!.hook as Hook<Any>).run(myBuilder, marker, it) }
            }
            state.hooks = state.hooks!!.next
        }
    }


    fun exit_section_(

        marker: PsiBuilder.Marker?,
        elementType: IElementType?,
        result: Boolean
    ) {
        val state = ErrorState.get(myBuilder)
        close_marker_impl_(state.currentFrame, marker, elementType, result)
        run_hooks_impl_(state, if (result) elementType else null)
        state.level--
    }

    fun current_position_(): Int {
        return myBuilder.rawTokenIndex()
    }

    fun empty_element_parsed_guard_(funcName: String, pos: Int): Boolean {
        if (pos == current_position_()) {
            // sometimes this is a correct situation, therefore no explicit marker
            myBuilder.error("Empty element parsed in '" + funcName + "' at offset " + myBuilder.currentOffset)
            return false
        }
        return true
    }
}

/**
 * 使用bnf工具生成代码
 */
class CangJieBnfParsing(
    builder: SemanticWhitespaceAwarePsiBuilder,
    private val cangJieParsing: CangJieParsing, isLazy: Boolean
) : BnfParsing(
    builder, isLazy
) {

    // MACRO_ATTR_EXPR?
//    private fun MACRO_EXPRESSION_2(l: Int): Boolean {
//        if (!recursion_guard_(l, "MACRO_EXPRESSION_2")) return false
//        MACRO_ATTR_EXPR(l + 1)
//        return true
//    }
//
//    /* ********************************************************** */ // LBRACKET QUOTE_TOKENS* RBRACKET
//    fun MACRO_ATTR_EXPR(l: Int): Boolean {
//        if (!recursion_guard_(l, "MACRO_ATTR_EXPR")) return false
//        if (!nextTokenIs(CjTokens.LBRACKET)) return false
//        val m = enter_section_()
//        var r = consumeToken(CjTokens.LBRACKET)
//        r = r && MACRO_ATTR_EXPR_1(l + 1)
//        r = r && consumeToken(CjTokens.RBRACKET)
//        exit_section_(m, MACRO_ATTR_EXPR, r)
//        return r
//    }
//
//    // QUOTE_TOKENS*
//    private fun MACRO_ATTR_EXPR_1(l: Int): Boolean {
//        if (!recursion_guard_(l, "MACRO_ATTR_EXPR_1")) return false
//        while (true) {
//            val c = current_position_()
//            if (!QUOTE_TOKENS(l + 1)) break
//            if (!empty_element_parsed_guard_(
//
//                    "MACRO_ATTR_EXPR_1",
//                    c
//                )
//            ) break
//        }
//        return true
//    }
//
//    /* ********************************************************** */ // AT  IDENTIFIER MACRO_ATTR_EXPR? MACRO_INPUT_EXPR_WITHOUT_PARENS
//    fun MACRO_EXPRESSION(l: Int): Boolean {
//        if (!recursion_guard_(l, "MACRO_EXPRESSION")) return false
//        if (!nextTokenIs(CjTokens.AT)) return false
//        val m = enter_section_()
//        var r =
//            consumeTokens(0, CjTokens.AT, CjTokens.IDENTIFIER)
//        r = r && MACRO_EXPRESSION_2(l + 1)
//        r = r && MACRO_INPUT_EXPR_WITHOUT_PARENS(l + 1)
//        exit_section_(m, CjNodeTypes.MACRO_EXPRESSION, r)
//        return r
//    }

    override fun create(builder: SemanticWhitespaceAwarePsiBuilder?): CangJieParsing = cangJieParsing.create(builder)
}
