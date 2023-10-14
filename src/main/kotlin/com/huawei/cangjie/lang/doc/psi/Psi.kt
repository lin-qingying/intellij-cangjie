package com.huawei.cangjie.lang.doc.psi





import com.huawei.cangjie.lang.core.psi.ext.CjElement
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.injected.InjectionBackgroundSuppressor

interface CjDocElement : CjElement {
    val containingDoc: CjDocComment

    val markdownValue: String
}


interface CjDocGap : PsiComment


interface CjDocHeading : CjDocElement

interface CjDocAtxHeading : CjDocHeading

interface CjDocSetextHeading : CjDocHeading


interface CjDocEmphasis : CjDocElement


interface CjDocStrong : CjDocElement


interface CjDocCodeSpan : CjDocElement


interface CjDocAutoLink : CjDocElement

interface CjDocLink : CjDocElement


interface CjDocInlineLink : CjDocLink {
    val linkText: CjDocLinkText
    val linkDestination: CjDocLinkDestination
}

sealed interface CjDocPathLinkParent : CjDocElement


interface CjDocLinkReferenceShort : CjDocLink, CjDocPathLinkParent {
    val linkLabel: CjDocLinkLabel
}


interface CjDocLinkReferenceFull : CjDocLink {
    val linkText: CjDocLinkText
    val linkLabel: CjDocLinkLabel
}


interface CjDocLinkDefinition : CjDocLink {
    val linkLabel: CjDocLinkLabel
    val linkDestination: CjDocLinkDestination
}


interface CjDocLinkText : CjDocElement


interface CjDocLinkLabel : CjDocElement


interface CjDocLinkTitle : CjDocElement


interface CjDocLinkDestination : CjDocPathLinkParent


interface CjDocCodeFence : CjDocElement, PsiLanguageInjectionHost, InjectionBackgroundSuppressor {
    val start: CjDocCodeFenceStartEnd
    val end: CjDocCodeFenceStartEnd?
    val lang: CjDocCodeFenceLang?
}


interface CjDocCodeBlock : CjDocElement

interface CjDocHtmlBlock : CjDocElement


interface CjDocCodeFenceStartEnd : CjDocElement


interface CjDocCodeFenceLang : CjDocElement
