package com.huawei.cangjie.ide.codeinsight.quickDoc.cdoc


interface CangJieIdeDescriptorRendererHighlightingManager<TAttributes : CangJieIdeDescriptorRendererHighlightingManager.Companion.Attributes> {

    fun StringBuilder.appendHighlighted(value: String, attributes: TAttributes)

    fun StringBuilder.appendCodeSnippetHighlightedByLexer(codeSnippet: String)

    val asError: TAttributes

    val asInfo: TAttributes

    val asDot: TAttributes

    val asComma: TAttributes
    val asStatic: TAttributes
    val asColon: TAttributes

    val asLtColon: TAttributes
    val asDoubleColon: TAttributes

    val asParentheses: TAttributes

    val asArrow: TAttributes

    val asBrackets: TAttributes

    val asBraces: TAttributes

    val asOperationSign: TAttributes

    val asNonNullAssertion: TAttributes

    val asNullityMarker: TAttributes

    val asKeyword: TAttributes

    val asLet: TAttributes
    val asMut: TAttributes
    val asProp: TAttributes
    val asVar: TAttributes

    val asAnnotationName: TAttributes

    val asAnnotationAttributeName: TAttributes

    val asClassName: TAttributes

    val asPackageName: TAttributes

    val asObjectName: TAttributes

    val asInstanceProperty: TAttributes

    val asTypeAlias: TAttributes

    val asParameter: TAttributes

    val asTypeParameterName: TAttributes

    val asLocalVarOrLet: TAttributes

    val asFunDeclaration: TAttributes

    val asFunCall: TAttributes


    companion object {

        interface Attributes

        fun <TAttributes : Attributes> CangJieIdeDescriptorRendererHighlightingManager<TAttributes>.eraseTypeParameter():
                CangJieIdeDescriptorRendererHighlightingManager<Attributes> {
            @Suppress("UNCHECKED_CAST")
            return this as CangJieIdeDescriptorRendererHighlightingManager<Attributes>
        }

        private val EMPTY_ATTRIBUTES = object : Attributes {}

        val NO_HIGHLIGHTING = object : CangJieIdeDescriptorRendererHighlightingManager<Attributes> {
            override fun StringBuilder.appendHighlighted(value: String, attributes: Attributes) {
                append(value)
            }

            override fun StringBuilder.appendCodeSnippetHighlightedByLexer(codeSnippet: String) {
                append(codeSnippet)
            }

            override val asLtColon = EMPTY_ATTRIBUTES
            override val asError = EMPTY_ATTRIBUTES
            override val asInfo = EMPTY_ATTRIBUTES
            override val asDot = EMPTY_ATTRIBUTES
            override val asComma = EMPTY_ATTRIBUTES
            override val asColon = EMPTY_ATTRIBUTES
            override val asStatic = EMPTY_ATTRIBUTES
            override val asDoubleColon = EMPTY_ATTRIBUTES
            override val asParentheses = EMPTY_ATTRIBUTES
            override val asArrow = EMPTY_ATTRIBUTES
            override val asBrackets = EMPTY_ATTRIBUTES
            override val asBraces = EMPTY_ATTRIBUTES
            override val asOperationSign = EMPTY_ATTRIBUTES
            override val asNonNullAssertion = EMPTY_ATTRIBUTES
            override val asNullityMarker = EMPTY_ATTRIBUTES
            override val asKeyword = EMPTY_ATTRIBUTES
            override val asLet = EMPTY_ATTRIBUTES
            override val asVar = EMPTY_ATTRIBUTES
            override val asAnnotationName = EMPTY_ATTRIBUTES
            override val asAnnotationAttributeName = EMPTY_ATTRIBUTES
            override val asClassName = EMPTY_ATTRIBUTES
            override val asPackageName = EMPTY_ATTRIBUTES
            override val asObjectName = EMPTY_ATTRIBUTES
            override val asInstanceProperty = EMPTY_ATTRIBUTES
            override val asTypeAlias = EMPTY_ATTRIBUTES
            override val asParameter = EMPTY_ATTRIBUTES
            override val asTypeParameterName = EMPTY_ATTRIBUTES
            override val asLocalVarOrLet = EMPTY_ATTRIBUTES
            override val asFunDeclaration = EMPTY_ATTRIBUTES
            override val asFunCall = EMPTY_ATTRIBUTES

            override val asMut: Attributes = EMPTY_ATTRIBUTES
            override val asProp: Attributes = EMPTY_ATTRIBUTES
        }
    }
}
