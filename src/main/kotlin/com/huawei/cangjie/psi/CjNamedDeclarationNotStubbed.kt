package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.name.Name.Companion.identifier
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls

abstract class CjNamedDeclarationNotStubbed(node: ASTNode) : CjDeclarationImpl(node),
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
