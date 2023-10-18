package com.huawei.cangjie.lang.doc.psi

import com.huawei.cangjie.lang.doc.psi.impl.*
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes


@Suppress("MemberVisibilityCanBePrivate")
object CjDocElementTypes {
    val DOC_DATA = CjDocTokenType("<DOC_DATA>")
    val DOC_GAP = CjDocTokenType("<DOC_GAP>")

    val DOC_ATX_HEADING = CjDocCompositeTokenType("<DOC_ATX_HEADING>", ::CjDocAtxHeadingImpl)
    val DOC_SETEXT_HEADING = CjDocCompositeTokenType("<DOC_SETEXT_HEADING>", ::CjDocSetextHeadingImpl)

    val DOC_EMPHASIS = CjDocCompositeTokenType("<DOC_EMPHASIS>", ::CjDocEmphasisImpl)
    val DOC_STRONG = CjDocCompositeTokenType("<DOC_STRONG>", ::CjDocStrongImpl)
    val DOC_CODE_SPAN = CjDocCompositeTokenType("<DOC_CODE_SPAN>", ::CjDocCodeSpanImpl)

    val DOC_AUTO_LINK = CjDocCompositeTokenType("<DOC_AUTO_LINK>", ::CjDocAutoLinkImpl)
    val DOC_INLINE_LINK = CjDocCompositeTokenType("<DOC_INLINE_LINK>", ::CjDocInlineLinkImpl)
    val DOC_SHORT_REFERENCE_LINK = CjDocCompositeTokenType("<DOC_SHORT_REFERENCE_LINK>", ::CjDocLinkReferenceShortImpl)
    val DOC_FULL_REFERENCE_LINK = CjDocCompositeTokenType("<DOC_FULL_REFERENCE_LINK>", ::CjDocLinkReferenceFullImpl)
    val DOC_LINK_DEFINITION = CjDocCompositeTokenType("<DOC_LINK_DEFINITION>", ::CjDocLinkDefinitionImpl)

    val DOC_LINK_TEXT = CjDocCompositeTokenType("<DOC_LINK_TEXT>", ::CjDocLinkTextImpl)
    val DOC_LINK_LABEL = CjDocCompositeTokenType("<DOC_LINK_LABEL>", ::CjDocLinkLabelImpl)
    val DOC_LINK_TITLE = CjDocCompositeTokenType("<DOC_LINK_TITLE>", ::CjDocLinkTitleImpl)
    val DOC_LINK_DESTINATION = CjDocCompositeTokenType("<DOC_LINK_DESTINATION>", ::CjDocLinkDestinationImpl)

    val DOC_CODE_FENCE = CjDocCompositeTokenType("<DOC_CODE_FENCE>", ::CjDocCodeFenceImpl)
    val DOC_CODE_BLOCK = CjDocCompositeTokenType("<DOC_CODE_BLOCK>", ::CjDocCodeBlockImpl)
    val DOC_HTML_BLOCK = CjDocCompositeTokenType("<DOC_HTML_BLOCK>", ::CjDocHtmlBlockImpl)

    val DOC_CODE_FENCE_START_END = CjDocCompositeTokenType("<DOC_CODE_FENCE_START_END>", ::CjDocCodeFenceStartEndImpl)
    val DOC_CODE_FENCE_LANG = CjDocCompositeTokenType("<DOC_CODE_FENCE_LANG>", ::CjDocCodeFenceLangImpl)

    private val MARKDOWN_ATX_HEADINGS = setOf(
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4,
        MarkdownElementTypes.ATX_5,
        MarkdownElementTypes.ATX_6
    )


    fun mapMarkdownToCangJie(type: org.intellij.markdown.IElementType): CjDocCompositeTokenType? {
        return when (type) {
            in MARKDOWN_ATX_HEADINGS -> DOC_ATX_HEADING
            MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.SETEXT_2 -> DOC_SETEXT_HEADING
            MarkdownElementTypes.EMPH -> DOC_EMPHASIS
            MarkdownElementTypes.STRONG -> DOC_STRONG
            MarkdownElementTypes.CODE_SPAN -> DOC_CODE_SPAN
            MarkdownElementTypes.AUTOLINK -> DOC_AUTO_LINK
            MarkdownElementTypes.INLINE_LINK -> DOC_INLINE_LINK
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> DOC_SHORT_REFERENCE_LINK
            MarkdownElementTypes.FULL_REFERENCE_LINK -> DOC_FULL_REFERENCE_LINK
            MarkdownElementTypes.LINK_DEFINITION -> DOC_LINK_DEFINITION
            MarkdownElementTypes.LINK_TEXT -> DOC_LINK_TEXT
            MarkdownElementTypes.LINK_LABEL -> DOC_LINK_LABEL
            MarkdownElementTypes.LINK_TITLE -> DOC_LINK_TITLE
            MarkdownElementTypes.LINK_DESTINATION -> DOC_LINK_DESTINATION
            MarkdownElementTypes.CODE_FENCE -> DOC_CODE_FENCE
            MarkdownElementTypes.CODE_BLOCK -> DOC_CODE_BLOCK
            MarkdownElementTypes.HTML_BLOCK -> DOC_HTML_BLOCK
            MarkdownTokenTypes.CODE_FENCE_START, MarkdownTokenTypes.CODE_FENCE_END -> DOC_CODE_FENCE_START_END
            MarkdownTokenTypes.FENCE_LANG -> DOC_CODE_FENCE_LANG
            else -> null
        }
    }
}
