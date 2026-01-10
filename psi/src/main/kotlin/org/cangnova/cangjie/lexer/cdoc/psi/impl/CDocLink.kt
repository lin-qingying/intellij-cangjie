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

package org.cangnova.cangjie.lexer.cdoc.psi.impl

import org.cangnova.cangjie.psi.CjElementImpl
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange

class CDocLink(node: ASTNode) : CjElementImpl(node) {
    fun getLinkText(): String = getLinkTextRange().substring(text)

    fun getLinkTextRange(): TextRange {
        val text = text
        if (text.startsWith('[') && text.endsWith(']')) {
            return TextRange(1, text.length - 1)
        }
        return TextRange(0, text.length)
    }

    /**
     * If this link is the subject of a tag, returns the tag. Otherwise, returns null.
     */
    fun getTagIfSubject(): CDocTag? {
        val tag = getStrictParentOfType<CDocTag>()
        return if (tag != null && tag.getSubjectLink() == this) tag else null
    }
}
