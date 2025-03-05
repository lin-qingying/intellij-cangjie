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

package cn.cangnova.cangjie.psi

import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.name.Name.Companion.identifier
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls

abstract class CjNamedDeclarationNotStubbed(node: ASTNode) :
    CjDeclarationImpl(node),
    CjNamedDeclaration {
    override fun getName(): String? {
        val identifier = nameIdentifier
        if (identifier != null) {
            val text = identifier.text
            return if (text != null) CjPsiUtil.unquoteIdentifier(text) else null
        } else {
            return null
        }
    }

    override val nameAsName: Name?
        get() {
            val name = name
            return if (name != null) identifier(name) else null
        }

    override val nameAsSafeName: Name
        get() = CjPsiUtil.safeName(name)

    override fun getNameIdentifier(): PsiElement? {
        return findChildByType(CjTokens.IDENTIFIER)
    }

    @Throws(IncorrectOperationException::class)
    override fun setName(name: @NonNls String): PsiElement {
        val identifier = nameIdentifier ?: throw IncorrectOperationException()

        return identifier.replace(CjPsiFactory(project).createNameIdentifier(name))
    }

    override fun getTextOffset(): Int {
        val identifier = nameIdentifier
        return identifier?.textRange?.startOffset ?: textRange.startOffset
    }
}
