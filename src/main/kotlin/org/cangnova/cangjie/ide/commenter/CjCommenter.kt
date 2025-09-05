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

package org.cangnova.cangjie.ide.commenter

import org.cangnova.cangjie.psi.cdoc.psi.CDoc
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lexer.CjTokens.BLOCK_COMMENT
import org.cangnova.cangjie.lexer.CjTokens.DOC_COMMENT
import org.cangnova.cangjie.lexer.CjTokens.EOL_COMMENT
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
