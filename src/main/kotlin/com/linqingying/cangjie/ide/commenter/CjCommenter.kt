package com.linqingying.cangjie.ide.commenter

import com.linqingying.cangjie.doc.psi.CDoc
import com.linqingying.cangjie.lang.CangJieLanguage
import com.linqingying.cangjie.lexer.CjTokens.*
import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.generation.CommenterDataHolder
import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

data class CommentHolder(val file: PsiFile) : CommenterDataHolder() {
    fun useSpaceAfterLineComment(): Boolean =
        CodeStyle.getLanguageSettings(file, CangJieLanguage).LINE_COMMENT_ADD_SPACE
}
//
//class CjCommenter : Commenter, CodeDocumentationAwareCommenter, SelfManagingCommenter<CommentHolder> {
//
//}


class CjCommenter : CodeDocumentationAwareCommenter {
    override fun getLineCommentPrefix(): String = "//"


    override fun getBlockCommentPrefix(): String = "/*"


    override fun getBlockCommentSuffix(): String = "*/"


    override fun getCommentedBlockCommentPrefix(): String? = null


    override fun getCommentedBlockCommentSuffix(): String? = null


    override fun getLineCommentTokenType(): IElementType = EOL_COMMENT

    override fun getBlockCommentTokenType(): IElementType = BLOCK_COMMENT
    override fun getDocumentationCommentTokenType(): IElementType = DOC_COMMENT

    override fun getDocumentationCommentPrefix(): String = "/**"

    override fun getDocumentationCommentLinePrefix(): String = "*"

    override fun getDocumentationCommentSuffix(): String = "*/"
    override fun isDocumentationComment(element: PsiComment?): Boolean = element is CDoc
}
